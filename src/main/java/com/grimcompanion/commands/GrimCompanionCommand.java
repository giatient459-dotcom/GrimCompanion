package com.grimcompanion.commands;

import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import com.grimcompanion.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Xu ly lenh /grimcompanion (alias /gc) va cac lenh con:
 * reload, check <player>, alerts, stats <player>, reset <player>
 */
public class GrimCompanionCommand implements CommandExecutor, TabCompleter {

    private final GrimCompanion plugin;

    public GrimCompanionCommand(GrimCompanion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = MessageUtil.colorize(plugin.getConfig().getString("prefix", "&c[GrimCompanion] "));

        if (args.length == 0) {
            sendHelp(sender, prefix);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> handleReload(sender, prefix);
            case "check" -> handleCheck(sender, prefix, args);
            case "alerts" -> handleAlerts(sender, prefix);
            case "stats" -> handleStats(sender, prefix, args);
            case "reset" -> handleReset(sender, prefix, args);
            case "bypass" -> handleBypass(sender, prefix, args);
            default -> sendHelp(sender, prefix);
        }
        return true;
    }

    /**
     * /gc bypass <player> <check|all> <giay> - cap bypass tam thoi thu cong, dung khi
     * mot plugin khac (thang may, jetpack...) gay di chuyen la nhung khong ho tro goi
     * BypassManager API truc tiep. Xem them com.grimcompanion.compat.BypassManager.
     */
    private void handleBypass(CommandSender sender, String prefix, String[] args) {
        if (!sender.hasPermission("grimcompanion.admin")) {
            sender.sendMessage(prefix + errMsg("no-permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(prefix + "§cCu phap: /gc bypass <player> <check|all> <giay>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + errMsg("player-not-found"));
            return;
        }
        String checkName = args[2];
        int seconds;
        try {
            seconds = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(prefix + "§cSo giay khong hop le: " + args[3]);
            return;
        }
        if (seconds <= 0 || seconds > 300) {
            sender.sendMessage(prefix + "§cSo giay phai trong khoang 1-300.");
            return;
        }

        plugin.getBypassManager().grantBypass(target, checkName, seconds);
        sender.sendMessage(prefix + "§aDa cap bypass '" + checkName + "' cho " + target.getName()
                + " trong " + seconds + " giay.");
    }

    private void handleReload(CommandSender sender, String prefix) {
        if (!sender.hasPermission("grimcompanion.admin")) {
            sender.sendMessage(prefix + errMsg("no-permission"));
            return;
        }
        plugin.getConfigUtil().reload();
        plugin.getCheckManager().reloadChecks();
        sender.sendMessage(prefix + msg("reload-success", "&aDa reload config thanh cong!"));
    }

    private void handleCheck(CommandSender sender, String prefix, String[] args) {
        if (!sender.hasPermission("grimcompanion.admin")) {
            sender.sendMessage(prefix + errMsg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(prefix + "§cCu phap: /gc check <player>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + errMsg("player-not-found"));
            return;
        }
        PlayerData data = plugin.getDataManager().getPlayerData(target);
        sender.sendMessage(prefix + "§7Thong tin: §f" + target.getName());
        sender.sendMessage("§7Ping: §f" + data.getLastPingMs() + "ms");
        sender.sendMessage("§7Client brand: §f" + (data.getClientBrand() == null ? "chua xac dinh" : data.getClientBrand()));
        sender.sendMessage("§7Tong so check dang bat: §f" +
                plugin.getCheckManager().getChecks().stream().filter(Check::isEnabled).count());

        var engineState = plugin.getPredictionEngine().getState(target);
        sender.sendMessage("§7PredictionEngine (rieng GrimCompanion): §f"
                + "V-trust=" + String.format("%.1f", engineState.verticalTrust)
                + " H-trust=" + String.format("%.1f", engineState.horizontalTrust));

        boolean hooked = plugin.getGrimIntegration() != null && plugin.getGrimIntegration().isHooked();
        sender.sendMessage("§7Doi chieu GrimAC: " + (hooked ? "§aDa ket noi" : "§cKhong hoat dong (khong bat buoc)"));
        if (hooked) {
            int vl = plugin.getGrimIntegration().getGrimViolationLevel(target, "Speed");
            if (vl >= 0) sender.sendMessage("§7  VL Speed (tu GrimAC, chi de doi chieu): §f" + vl);
        }
    }

    private void handleAlerts(CommandSender sender, String prefix) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cLenh nay chi danh cho nguoi choi!");
            return;
        }
        if (!player.hasPermission("grimcompanion.staff")) {
            sender.sendMessage(prefix + errMsg("no-permission"));
            return;
        }
        boolean nowOn = plugin.getCheckManager().toggleAlerts(player);
        if (nowOn) {
            sender.sendMessage(prefix + msg("alerts-on", "&aDa bat canh bao!"));
        } else {
            sender.sendMessage(prefix + msg("alerts-off", "&cDa tat canh bao!"));
        }
    }

    private void handleStats(CommandSender sender, String prefix, String[] args) {
        if (!sender.hasPermission("grimcompanion.admin")) {
            sender.sendMessage(prefix + errMsg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(prefix + "§cCu phap: /gc stats <player>");
            return;
        }

        Player online = Bukkit.getPlayer(args[1]);
        if (online != null) {
            // Nguoi choi dang online: uu tien du lieu RAM (moi nhat, chua kip ghi xong SQLite
            // async cung khong sao vi RAM luon la nguon "song" nhat trong luc dang choi).
            PlayerData data = plugin.getDataManager().getPlayerData(online);
            Map<String, Integer> violations = data.getAllViolations();

            sender.sendMessage(prefix + "§7Thong ke vi pham cua §f" + online.getName() + " §7(dang online):");
            if (violations.isEmpty()) {
                sender.sendMessage("§7  Khong co vi pham nao trong phien nay.");
            } else {
                violations.forEach((check, vl) ->
                        sender.sendMessage("§7  - §c" + check + "§7: §f" + vl + " VL"));
            }
            return;
        }

        // Nguoi choi offline: doc tu SQLite (du lieu qua cac lan restart truoc, RAM da mat het).
        org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
        if (offline.getUniqueId() == null || (!offline.hasPlayedBefore() && offline.getName() == null)) {
            sender.sendMessage(prefix + errMsg("player-not-found"));
            return;
        }
        if (!plugin.getSqliteStorage().isAvailable()) {
            sender.sendMessage(prefix + "§cPlayer khong online va SQLite storage khong kha dung - khong the tra cuu lich su.");
            return;
        }
        Map<String, Integer> totals = plugin.getSqliteStorage().loadPlayerTotals(offline.getUniqueId());
        sender.sendMessage(prefix + "§7Thong ke vi pham cua §f" + args[1] + " §7(offline, tu SQLite):");
        if (totals.isEmpty()) {
            sender.sendMessage("§7  Khong co vi pham nao duoc luu.");
            return;
        }
        totals.forEach((check, vl) -> sender.sendMessage("§7  - §c" + check + "§7: §f" + vl + " VL (tong tich luy)"));
    }

    private void handleReset(CommandSender sender, String prefix, String[] args) {
        if (!sender.hasPermission("grimcompanion.admin")) {
            sender.sendMessage(prefix + errMsg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(prefix + "§cCu phap: /gc reset <player>");
            return;
        }

        UUID targetUuid;
        String targetName;
        Player online = Bukkit.getPlayer(args[1]);
        if (online != null) {
            plugin.getDataManager().getPlayerData(online).resetAllViolations();
            targetUuid = online.getUniqueId();
            targetName = online.getName();
        } else {
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            if (!offline.hasPlayedBefore()) {
                sender.sendMessage(prefix + errMsg("player-not-found"));
                return;
            }
            targetUuid = offline.getUniqueId();
            targetName = args[1];
        }

        // Xoa dong bo ca SQLite (chay async, khong chan main thread)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getSqliteStorage().resetPlayer(targetUuid));

        sender.sendMessage(prefix + msg("reset-success", "&aDa reset VL!").replace("{player}", targetName));
    }

    private void sendHelp(CommandSender sender, String prefix) {
        sender.sendMessage(prefix + "§7Danh sach lenh:");
        sender.sendMessage("§e/gc reload §7- Reload config");
        sender.sendMessage("§e/gc check <player> §7- Kiem tra thong tin player");
        sender.sendMessage("§e/gc alerts §7- Bat/tat canh bao");
        sender.sendMessage("§e/gc stats <player> §7- Xem thong ke vi pham");
        sender.sendMessage("§e/gc reset <player> §7- Reset VL");
        sender.sendMessage("§e/gc bypass <player> <check|all> <giay> §7- Cap bypass tam thoi (thang may, jetpack...)");
    }

    private String msg(String key, String def) {
        return MessageUtil.colorize(plugin.getConfig().getString("messages." + key, def));
    }

    private String errMsg(String key) {
        return msg(key, "&cLoi!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("reload", "check", "alerts", "stats", "reset", "bypass"), args[0]);
        }
        if (args.length == 2 && List.of("check", "stats", "reset", "bypass").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return filterStartsWith(names, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("bypass")) {
            return filterStartsWith(List.of("all", "flight", "speed", "noslowdown", "crystalaura",
                    "anchoraura", "autoclicker", "killaura", "reach", "scaffold"), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("bypass")) {
            return filterStartsWith(List.of("5", "10", "30", "60"), args[3]);
        }
        return Collections.emptyList();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(input.toLowerCase())) result.add(opt);
        }
        return result;
    }
}
