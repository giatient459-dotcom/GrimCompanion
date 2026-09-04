package com.grimcompanion.data;

import com.grimcompanion.GrimCompanion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quan ly PlayerData cho tat ca nguoi choi dang online, va ghi log vi pham ra file.
 */
public class DataManager {

    private final GrimCompanion plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public DataManager(GrimCompanion plugin) {
        this.plugin = plugin;
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        this.logFile = new File(logsDir, "violations.log");
    }

    /**
     * Lay PlayerData cua nguoi choi, tao moi neu chua co (lazy init).
     */
    public PlayerData getPlayerData(org.bukkit.entity.Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(),
                uuid -> new PlayerData(uuid, player.getName()));
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void removePlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public boolean hasPlayerData(UUID uuid) {
        return playerDataMap.containsKey(uuid);
    }

    /**
     * Ghi 1 dong log ra file logs/violations.log (append, khong chan main thread).
     */
    public void writeLog(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println("[" + dateFormat.format(new Date()) + "] " + message);
        } catch (IOException e) {
            plugin.getLogger().warning("Khong the ghi log vi pham: " + e.getMessage());
        }
    }

    public void clearAll() {
        playerDataMap.clear();
    }
}
