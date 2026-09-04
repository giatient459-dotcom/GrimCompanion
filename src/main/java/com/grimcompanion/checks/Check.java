package com.grimcompanion.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Lop co so cho tat ca cac check anti-cheat.
 * Moi check con phai implement handlePacketReceive/handlePacketSend
 * va goi flag() khi phat hien hanh vi bat thuong.
 */
public abstract class Check {

    protected final GrimCompanion plugin;
    protected final String name;          // Ten check, dung de tra config va hien thi (vd: "CrystalAura")
    protected final String description;   // Mo ta ngan gon
    protected boolean enabled;
    protected int maxViolations;

    public Check(GrimCompanion plugin, String name, String description) {
        this.plugin = plugin;
        this.name = name;
        this.description = description;
        reloadConfig();
    }

    /**
     * Doc lai cau hinh tu config.yml (goi khi khoi tao va khi /gc reload)
     */
    public void reloadConfig() {
        String path = "checks." + name.toLowerCase() + ".";
        this.enabled = plugin.getConfig().getBoolean(path + "enabled", true);
        this.maxViolations = plugin.getConfig().getInt(path + "max-violations", 10);
    }

    /**
     * Xu ly packet ma client GUI (server nhan tu client)
     */
    public abstract void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data);

    /**
     * Xu ly packet server gui cho client (it dung hon, chu yeu cho ping/keepalive)
     */
    public abstract void handlePacketSend(PacketSendEvent event, Player player, PlayerData data);

    /**
     * Ghi nhan 1 vi pham cho nguoi choi. Tu dong canh bao staff va xu phat neu vuot nguong.
     *
     * @param player  nguoi choi vi pham
     * @param details chi tiet vi pham (hien thi trong log/alert)
     */
    public void flag(Player player, String details) {
        if (!enabled) return;
        if (player.hasPermission("grimcompanion.bypass")) return;

        // Kiem tra bypass tam thoi (do plugin khac hoac admin cap qua BypassManager) -
        // dung cho cac tinh huong hop le nhung "trong bat thuong" (thang may, jetpack,
        // cutscene, teleport lien tuc do plugin khac dieu khien). Xem BypassManager.java.
        if (plugin.getBypassManager().isBypassed(player, name)) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[Debug] Bo qua flag " + name + " cho " + player.getName()
                        + " do dang trong thoi gian bypass.");
            }
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player);
        int vl = data.incrementViolation(name);

        plugin.getCheckManager().handleViolation(player, name, details, vl, maxViolations);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxViolations() {
        return maxViolations;
    }

    protected double getConfigDouble(String key, double def) {
        return plugin.getConfig().getDouble("checks." + name.toLowerCase() + "." + key, def);
    }

    protected int getConfigInt(String key, int def) {
        return plugin.getConfig().getInt("checks." + name.toLowerCase() + "." + key, def);
    }
}
