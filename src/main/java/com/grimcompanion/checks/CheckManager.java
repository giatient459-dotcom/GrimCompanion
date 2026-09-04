package com.grimcompanion.checks;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.combat.*;
import com.grimcompanion.checks.exploit.*;
import com.grimcompanion.checks.movement.*;
import com.grimcompanion.checks.world.ScaffoldCheck;
import com.grimcompanion.data.PlayerData;
import com.grimcompanion.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Quan ly toan bo cac check: dang ky, dispatch packet, xu ly vi pham
 * (canh bao staff, ghi log, ap dung hinh phat).
 */
public class CheckManager {

    private final GrimCompanion plugin;
    private final List<Check> checks = new ArrayList<>();

    // co nhan alert hay khong, theo tung staff (UUID -> bat/tat)
    private final java.util.Map<java.util.UUID, Boolean> alertToggle = new java.util.concurrent.ConcurrentHashMap<>();

    public CheckManager(GrimCompanion plugin) {
        this.plugin = plugin;
        registerChecks();
    }

    private void registerChecks() {
        // Combat
        checks.add(new CrystalAuraCheck(plugin));
        checks.add(new AnchorAuraCheck(plugin));
        checks.add(new AutoClickerCheck(plugin));
        checks.add(new KillAuraCheck(plugin));
        checks.add(new ReachCheck(plugin));

        // Movement
        checks.add(new FlightCheck(plugin));
        checks.add(new SpeedCheck(plugin));
        checks.add(new NoSlowdownCheck(plugin));

        // Exploit
        checks.add(new ItemMacroCheck(plugin));
        checks.add(new AutoFireworkCheck(plugin));
        checks.add(new ElytraTargetCheck(plugin));
        checks.add(new AutoCartCheck(plugin));
        checks.add(new CrystalOptimizerCheck(plugin));
        checks.add(new ClientBrandCheck(plugin));
        checks.add(new PingCheck(plugin));

        // World
        checks.add(new ScaffoldCheck(plugin));

        plugin.getLogger().info("Da dang ky " + checks.size() + " check.");
    }

    public void reloadChecks() {
        for (Check check : checks) {
            check.reloadConfig();
        }
    }

    /**
     * Goi khi nhan packet tu client. Dispatch den tat ca check dang bat.
     */
    public void dispatchReceive(PacketReceiveEvent event, Player player) {
        if (player == null) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player);

        // Chay PredictionEngine DUNG 1 LAN cho moi goi tin vi tri, truoc khi cac check chay.
        // Tranh truong hop nhieu check (Flight, Speed) cung goi simulateTick() rieng le
        // lam sai lech state ben trong engine (engine cap nhat lastX/Y/Z sau moi lan goi).
        var packetType = event.getPacketType();
        if (packetType == com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.PLAYER_POSITION
                || packetType == com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            org.bukkit.util.Vector loc = player.getLocation().toVector();
            var result = plugin.getPredictionEngine().simulateTick(
                    player, loc.getX(), loc.getY(), loc.getZ(), player.isOnGround()
            );
            data.setLastEngineTick(result);
        }

        for (Check check : checks) {
            if (!check.isEnabled()) continue;
            try {
                check.handlePacketReceive(event, player, data);
            } catch (Exception ex) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().log(Level.WARNING, "Loi trong check " + check.getName(), ex);
                }
            }
        }
    }

    /**
     * Goi khi server gui packet toi client.
     */
    public void dispatchSend(PacketSendEvent event, Player player) {
        if (player == null) return;
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        for (Check check : checks) {
            if (!check.isEnabled()) continue;
            try {
                check.handlePacketSend(event, player, data);
            } catch (Exception ex) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().log(Level.WARNING, "Loi trong check " + check.getName(), ex);
                }
            }
        }
    }

    /**
     * Xu ly khi 1 check bi flag: log, broadcast staff, va ap dung hinh phat neu vuot nguong.
     */
    public void handleViolation(Player player, String checkName, String details, int vl, int maxViolations) {
        String prefix = plugin.getConfig().getString("prefix", "§c[GrimCompanion] ");

        // Log
        String logMsg = String.format("%s flag %s (VL: %d) - %s", player.getName(), checkName, vl, details);
        plugin.getLogger().info(logMsg);
        if (plugin.getConfig().getBoolean("log-to-file", true)) {
            plugin.getDataManager().writeLog(logMsg);
        }

        // Canh bao staff
        int warnLevel = plugin.getConfig().getInt("punishments.warn-level", 3);
        if (vl >= warnLevel) {
            String warnMsg = MessageUtil.format(
                    plugin.getConfig().getString("messages.warn", "&e⚠ {player} flag {check} (VL: {vl})"),
                    player.getName(), checkName, String.valueOf(vl)
            );
            broadcastToStaff(prefix + warnMsg);
        }

        // Xu phat
        int kickLevel = plugin.getConfig().getInt("punishments.kick-level", 10);
        int banLevel = plugin.getConfig().getInt("punishments.ban-level", 20);

        if (vl >= banLevel) {
            String banCmd = plugin.getConfig().getString("punishments.ban-command", "")
                    .replace("{player}", player.getName());
            if (!banCmd.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCmd));
            }
            String banMsg = plugin.getConfig().getString("messages.ban", "§cBan da bi ban!");
            Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(banMsg));
        } else if (vl >= kickLevel) {
            String kickMsg = plugin.getConfig().getString("messages.kick", "§cBan da bi kick!");
            Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(kickMsg));
        }
    }

    private void broadcastToStaff(String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("grimcompanion.staff")) continue;
            Boolean toggled = alertToggle.get(online.getUniqueId());
            if (toggled != null && !toggled) continue; // staff da tat alert
            online.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public boolean toggleAlerts(Player player) {
        boolean current = alertToggle.getOrDefault(player.getUniqueId(), true);
        alertToggle.put(player.getUniqueId(), !current);
        return !current;
    }

    public List<Check> getChecks() {
        return checks;
    }

    public Check getCheck(String name) {
        for (Check check : checks) {
            if (check.getName().equalsIgnoreCase(name)) return check;
        }
        return null;
    }
}
