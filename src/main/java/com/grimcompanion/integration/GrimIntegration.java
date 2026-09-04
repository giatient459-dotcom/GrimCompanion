package com.grimcompanion.integration;

import com.grimcompanion.GrimCompanion;
import com.grimcompanion.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.logging.Level;

/**
 * GrimIntegration: cau noi (TUY CHON) giua GrimCompanion va GrimAC khi GrimAC duoc cai tren server.
 *
 * TU KHI CO PredictionEngine RIENG (xem package com.grimcompanion.engine), GrimCompanion khong
 * con can "muon" engine cua GrimAC nua - cac check di chuyen (Flight, Speed) luon chay bang
 * engine cua chinh GrimCompanion, doc lap hoan toan, dung nhu 1 plugin bo tro that su.
 *
 * Class nay gio chi con 1 nhiem vu: LANG NGHE va DOI CHIEU (khong bat buoc) - dua vi pham
 * (punishment/flag) tu GrimAC vao chung he thong canh bao + thong ke cua GrimCompanion
 * (/gc stats, /gc check), de staff xem duoc ca 2 nguon du lieu (engine rieng cua GrimCompanion
 * VA engine cua GrimAC) o cung 1 cho, tien so sanh/doi chieu ket qua giua 2 plugin.
 *
 * QUAN TRONG: GrimAC khong cam ket API on dinh 100% giua cac ban. Ten class/method duoi day
 * (GrimAPI, PlayerDataManager, GrimPunishmentEvent...) dua theo cau truc pho bien cua module
 * 'api' chinh thuc cua GrimAC. Neu ban build that va gap loi "cannot find symbol", hay:
 *   - Mo file .jar cua GrimAC (hoac artifact "api") de doi chieu ten package/class/method that.
 *   - Chinh lai phan reflection/goi truc tiep trong file nay cho khop.
 * Toan bo logic o day duoc boc trong try-catch(Throwable) de neu API thay doi hoac GrimAC
 * khong duoc cai, GrimCompanion van chay binh thuong (chi mat phan tich hop, khong crash) -
 * vi PredictionEngine rieng khong phu thuoc vao class nay chut nao.
 */
public class GrimIntegration implements Listener {

    private final GrimCompanion plugin;
    private boolean hooked = false;

    public GrimIntegration(GrimCompanion plugin) {
        this.plugin = plugin;
    }

    /**
     * Thu hook vao GrimAC neu plugin ton tai va API tuong thich.
     * Goi 1 lan trong onEnable(), sau khi cac plugin khac da load xong.
     */
    public void tryHook() {
        Plugin grimPlugin = Bukkit.getPluginManager().getPlugin("GrimAC");
        if (grimPlugin == null || !grimPlugin.isEnabled()) {
            plugin.getLogger().info("Khong tim thay GrimAC dang bat - GrimCompanion van chay day du "
                    + "voi PredictionEngine rieng, khong can GrimAC.");
            return;
        }

        try {
            // Kiem tra class GrimAPI co ton tai trong classpath runtime khong
            // (chi co that neu jar GrimAC that su duoc load, khac voi compileOnly).
            Class.forName("ac.grim.grimac.api.GrimAPI");

            // Dang ky lang nghe su kien vi pham cua GrimAC (ten event tham khao: GrimPunishmentEvent).
            // Neu ten event thuc te khac, thay the class duoc reference trong GrimPunishmentListener.
            Bukkit.getPluginManager().registerEvents(this, plugin);

            this.hooked = true;
            plugin.getLogger().info("Da tich hop thanh cong voi GrimAC API - se doi chieu vi pham "
                    + "giua PredictionEngine rieng cua GrimCompanion va GrimAC (neu relay-grimac-violations bat).");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("Tim thay plugin GrimAC nhung khong doc duoc API (ac.grim.grimac.api.GrimAPI). "
                    + "Co the ban GrimAC nay khong xuat API hoac da doi cau truc package. "
                    + "GrimCompanion van hoat dong day du (engine rieng khong bi anh huong).");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Loi khong xac dinh khi tich hop GrimAC (engine rieng cua GrimCompanion van chay binh thuong).", t);
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    /**
     * Truy van violation level (VL) hien tai cua 1 check tren GrimAC cho 1 nguoi choi,
     * dung de hien thi trong /gc check <player>. Tra ve -1 neu khong doc duoc.
     *
     * Cau truc tham khao: GrimAPI.INSTANCE.getPlayerDataManager().getPlayerData(uuid)
     * -> doi tuong GrimPlayer, sau do doc VL tu checkManager cua GrimPlayer.
     * Vi cau truc noi bo nay hay doi giua cac ban GrimAC, ham nay dung reflection
     * de giam rui ro loi bien dich khi API thay doi nho; neu that bai se tra ve -1 mot cach an toan.
     */
    public int getGrimViolationLevel(Player player, String checkName) {
        if (!hooked) return -1;
        try {
            Class<?> apiClass = Class.forName("ac.grim.grimac.api.GrimAPI");
            Object apiInstance = apiClass.getField("INSTANCE").get(null);

            Object playerDataManager = apiClass.getMethod("getPlayerDataManager").invoke(apiInstance);
            Object grimPlayer = playerDataManager.getClass()
                    .getMethod("getPlayerData", UUID.class)
                    .invoke(playerDataManager, player.getUniqueId());

            if (grimPlayer == null) return -1;

            // Cau truc GrimPlayer.getCheckManager().getVL(checkName) la GIA DINH tham khao;
            // hay doi chieu voi API that va sua lai neu can.
            Object checkManager = grimPlayer.getClass().getMethod("getCheckManager").invoke(grimPlayer);
            Object vlObj = checkManager.getClass().getMethod("getVL", String.class).invoke(checkManager, checkName);

            if (vlObj instanceof Number number) {
                return number.intValue();
            }
            return -1;
        } catch (Throwable t) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("Khong the doc VL tu GrimAC cho check '" + checkName + "': " + t.getMessage());
            }
            return -1;
        }
    }

    /**
     * Lang nghe su kien vi pham/punishment tu GrimAC bang reflection an toan (khong import
     * truc tiep class event de tranh loi bien dich neu ban GrimAC khong co class nay).
     * Cach lam thay the, an toan hon: dang ky qua PacketEvents hoac qua GrimAC's public
     * listener API neu co, thay vi @EventHandler truc tiep len 1 class cu the.
     *
     * O day minh hoa bang 1 EventHandler generic nhan Bukkit Event bat ky co ten class
     * chua "GrimPunishmentEvent" hoac "GrimFlagEvent" - Bukkit khong ho tro dieu nay natively,
     * nen cach dung THUC TE khuyen nghi la: them dependency GrimAC that (khong chi compileOnly)
     * va @EventHandler truc tiep len event class, vi du:
     *
     *   @EventHandler
     *   public void onGrimPunishment(ac.grim.grimac.api.events.GrimPunishmentEvent event) {
     *       Player player = event.getPlayer();
     *       String checkName = event.getCheckName();
     *       relayViolation(player, checkName, "GrimAC engine flag");
     *   }
     *
     * Ham relayViolation() ben duoi da san sang de ban noi vao khi xac dinh dung ten event/method.
     */
    public void relayViolation(Player player, String grimCheckName, String details) {
        if (!plugin.getConfig().getBoolean("integration.relay-grimac-violations", true)) return;

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        // Dung ten check dang "Grim:<TenCheck>" de phan biet voi check noi bo cua GrimCompanion
        // trong thong ke /gc stats, tranh nham lan nguon goc vi pham.
        String label = "Grim:" + grimCheckName;
        int vl = data.incrementViolation(label);
        plugin.getCheckManager().handleViolation(player, label, details, vl, Integer.MAX_VALUE);
        // maxViolations = MAX_VALUE vi GrimAC da tu xu ly kick/ban rieng theo config cua no;
        // GrimCompanion o day chi dong vai tro ghi nhan + canh bao staff, khong phat lai lan 2.
    }
}
