package com.grimcompanion.compat;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BypassManager: co che de "bao truoc" cho GrimCompanion rang mot nguoi choi sap co
 * hanh vi bat thuong NHUNG HOP LE (vd: bi plugin thang may/jetpack/cutscene khac day
 * di chuyen, dich chuyen bang lenh cua plugin khac...), tranh bi cac check (dac biet
 * la Flight/Speed/NoSlowdown) flag oan.
 *
 * Day la CACH TIEP CAN DUNG THUC TE ma cac anti-cheat lon (GrimAC, Vulcan...) dung de
 * tuong thich voi plugin thu 3: thay vi co gang liet ke ten hang tram plugin thang may/
 * jetpack/parkour tren doi (khong the nao liet ke het va luon thieu), GrimCompanion mo san
 * 1 "cua" (API + lenh admin) de PLUGIN KHAC hoac ADMIN chu dong bao truoc "dung flag nguoi
 * nay trong X giay", roi tu lam viec cua minh (day player, teleport, doi velocity...).
 *
 * Cach 1 - Plugin khac tu goi API (khuyen dung, chinh xac nhat vi dung luc):
 *   GrimCompanion plugin = (GrimCompanion) Bukkit.getPluginManager().getPlugin("GrimCompanion");
 *   if (plugin != null) {
 *       plugin.getBypassManager().grantBypass(player, "flight", 3); // bypass 3 giay
 *   }
 *
 * Cach 2 - Admin tu go lenh khi thay flag oan (fallback khi plugin kia khong ho tro API):
 *   /gc bypass <player> <check|all> <giay>
 */
public class BypassManager {

    // UUID -> (ten check hoa thuong, hoac "all") -> thoi diem het han (epoch millis)
    private final Map<UUID, Map<String, Long>> bypasses = new ConcurrentHashMap<>();

    /**
     * Cap 1 bypass tam thoi cho 1 check cu the, hoac "all" de bypass toan bo check
     * trong khoang thoi gian do (vd: dang cutscene/teleport lien tuc).
     *
     * @param player       nguoi choi can bypass
     * @param checkName    ten check (khong phan biet hoa thuong, vd "flight", "speed"),
     *                     hoac "all" de bypass toan bo check trong khoang thoi gian nay
     * @param durationSeconds thoi gian bypass (giay)
     */
    public void grantBypass(Player player, String checkName, int durationSeconds) {
        long expiry = System.currentTimeMillis() + (durationSeconds * 1000L);
        bypasses.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(checkName.toLowerCase(), expiry);
    }

    /**
     * Kiem tra xem 1 check co dang duoc bypass cho nguoi choi nay khong (con hieu luc).
     * Check.flag() se goi ham nay TRUOC KHI thuc su ghi nhan vi pham.
     */
    public boolean isBypassed(Player player, String checkName) {
        Map<String, Long> playerBypasses = bypasses.get(player.getUniqueId());
        if (playerBypasses == null) return false;

        long now = System.currentTimeMillis();

        Long allExpiry = playerBypasses.get("all");
        if (allExpiry != null && allExpiry > now) return true;

        Long checkExpiry = playerBypasses.get(checkName.toLowerCase());
        return checkExpiry != null && checkExpiry > now;
    }

    /**
     * Huy bypass som (vd: plugin kia biet chac hanh dong da xong truoc thoi gian du kien).
     */
    public void revokeBypass(Player player, String checkName) {
        Map<String, Long> playerBypasses = bypasses.get(player.getUniqueId());
        if (playerBypasses != null) {
            playerBypasses.remove(checkName.toLowerCase());
        }
    }

    /**
     * Don dep cac bypass da het han (goi dinh ky de tranh memory leak neu server chay lau).
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        bypasses.values().forEach(map -> map.values().removeIf(expiry -> expiry <= now));
        bypasses.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void clearPlayer(UUID uuid) {
        bypasses.remove(uuid);
    }
}
