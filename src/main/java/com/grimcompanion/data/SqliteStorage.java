package com.grimcompanion.data;

import com.grimcompanion.GrimCompanion;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SqliteStorage: luu vi pham vao file SQLite (plugins/GrimCompanion/data.db) de KHONG MAT
 * du lieu khi restart server - khac voi PlayerData/DataManager truoc day chi luu trong RAM,
 * mat het moi thu ngay khi server tat/nguoi choi thoat.
 *
 * QUAN TRONG VE DEPENDENCY: Paper da NHUNG SAN driver SQLite JDBC (org.sqlite.JDBC) o
 * classloader cha cua server tu rat lau (theo tai lieu chinh thuc docs.papermc.io/paper/dev/
 * using-databases). Vi vay KHONG can khai bao them dependency nao trong build.gradle, khong
 * can shade/relocate gi ca - chi can Class.forName("org.sqlite.JDBC") la du. Neu vi ly do nao
 * do server khong co san driver nay (rat hiem, chi xay ra tren ban Paper cuc ky cu hoac fork
 * la thuong), connect() se nem SQLException va duoc bat an toan, ghi log canh bao thay vi crash.
 *
 * Cau truc bang "violations": moi dong la 1 lan flag, giu lai LICH SU (khong ghi de),
 * de sau nay co the xem "vi pham gan day" chu khong chi tong so hien tai.
 */
public class SqliteStorage {

    private final GrimCompanion plugin;
    private Connection connection;
    private boolean available = false;

    public SqliteStorage(GrimCompanion plugin) {
        this.plugin = plugin;
    }

    /**
     * Ket noi database va tao bang neu chua co. Goi 1 lan luc onEnable().
     * Neu that bai (driver khong co, file loi...), plugin van chay binh thuong o che do
     * chi luu trong RAM (giong hanh vi cu truoc khi co tinh nang nay).
     */
    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");

            File dbFile = new File(plugin.getDataFolder(), "data.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS violations (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            player_name TEXT NOT NULL,
                            check_name TEXT NOT NULL,
                            details TEXT,
                            vl INTEGER NOT NULL,
                            timestamp INTEGER NOT NULL
                        )
                        """);
                // Index de truy van nhanh theo player - quan trong khi bang phinh to sau thoi gian dai
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_violations_player ON violations(player_uuid)");

                stmt.execute("""
                        CREATE TABLE IF NOT EXISTS player_totals (
                            player_uuid TEXT NOT NULL,
                            check_name TEXT NOT NULL,
                            total_vl INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY (player_uuid, check_name)
                        )
                        """);
            }

            available = true;
            plugin.getLogger().info("Da ket noi SQLite storage (plugins/GrimCompanion/data.db) - "
                    + "vi pham se duoc giu lai qua cac lan restart server.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Khong tim thay driver SQLite (org.sqlite.JDBC) tren server nay. "
                    + "GrimCompanion se chay o che do CHI LUU TRONG RAM - vi pham se mat khi restart. "
                    + "Truong hop nay rat hiem gap tren Paper hien dai.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Loi ket noi/tao bang SQLite, chay o che do chi luu RAM.", e);
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Ghi 1 dong lich su vi pham + cong don vao bang tong (player_totals).
     * Chay tren thread rieng (goi tu CheckManager qua Bukkit async task) de KHONG lam
     * cham main thread bang thao tac I/O dia.
     */
    public void recordViolation(UUID uuid, String playerName, String checkName, String details, int vl) {
        if (!available) return;
        try {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO violations (player_uuid, player_name, check_name, details, vl, timestamp) "
                            + "VALUES (?, ?, ?, ?, ?, ?)")) {
                insert.setString(1, uuid.toString());
                insert.setString(2, playerName);
                insert.setString(3, checkName);
                insert.setString(4, details);
                insert.setInt(5, vl);
                insert.setLong(6, System.currentTimeMillis());
                insert.executeUpdate();
            }

            try (PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO player_totals (player_uuid, check_name, total_vl) VALUES (?, ?, ?) "
                            + "ON CONFLICT(player_uuid, check_name) DO UPDATE SET total_vl = ?")) {
                upsert.setString(1, uuid.toString());
                upsert.setString(2, checkName);
                upsert.setInt(3, vl);
                upsert.setInt(4, vl);
                upsert.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Loi ghi vi pham vao SQLite", e);
        }
    }

    /**
     * Doc toan bo VL da luu cua 1 nguoi choi (dung khi player join lai, hoac /gc stats
     * cho nguoi hien khong online - thu duoc lich su cu qua cac lan restart).
     */
    public Map<String, Integer> loadPlayerTotals(UUID uuid) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (!available) return result;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT check_name, total_vl FROM player_totals WHERE player_uuid = ? ORDER BY total_vl DESC")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("check_name"), rs.getInt("total_vl"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Loi doc vi pham tu SQLite", e);
        }
        return result;
    }

    /**
     * Xoa toan bo vi pham cua 1 nguoi choi (dung boi /gc reset - de dong bo ca RAM lan DB).
     */
    public void resetPlayer(UUID uuid) {
        if (!available) return;
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM player_totals WHERE player_uuid = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Loi xoa vi pham trong SQLite", e);
        }
    }

    /**
     * Lay N dong lich su vi pham gan day nhat cua 1 nguoi choi (moi lan flag rieng le,
     * khac voi player_totals la tong don). Dung cho lenh xem chi tiet sau nay neu can.
     */
    public java.util.List<String> loadRecentHistory(UUID uuid, int limit) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (!available) return lines;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT check_name, details, vl, timestamp FROM violations "
                        + "WHERE player_uuid = ? ORDER BY timestamp DESC LIMIT ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(String.format("[%s] %s (VL:%d) - %s",
                            new java.text.SimpleDateFormat("dd/MM HH:mm:ss").format(new java.util.Date(rs.getLong("timestamp"))),
                            rs.getString("check_name"), rs.getInt("vl"), rs.getString("details")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Loi doc lich su SQLite", e);
        }
        return lines;
    }

    /**
     * Don dep cac dong vi pham cu hon X ngay de bang "violations" khong phinh to vo han
     * theo thoi gian (bang "player_totals" khong bi anh huong vi no la tong don, khong phai log).
     * Nen goi dinh ky (vd: 1 lan/ngay qua Bukkit scheduler) tu GrimCompanion.java.
     */
    public void pruneOldHistory(int keepDays) {
        if (!available) return;
        long cutoff = System.currentTimeMillis() - (keepDays * 86400000L);
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM violations WHERE timestamp < ?")) {
            stmt.setLong(1, cutoff);
            int deleted = stmt.executeUpdate();
            if (deleted > 0 && plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Debug] Da don dep " + deleted + " dong lich su vi pham cu hon " + keepDays + " ngay.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Loi don dep lich su SQLite", e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Loi dong ket noi SQLite", e);
            }
        }
    }
}
