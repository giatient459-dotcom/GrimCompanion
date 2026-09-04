package com.grimcompanion.compat;

import com.grimcompanion.GrimCompanion;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Level;

/**
 * AntiCheatDetector: tu dong do cac anti-cheat KHAC (khong phai GrimAC - GrimAC da co
 * co che rieng qua GrimIntegration.java) dang cai tren server, canh bao admin ve kha
 * nang trung lap/double-punish, va tuy chon TU DONG TAT cac check di chuyen noi bo cua
 * GrimCompanion (Flight/Speed/NoSlowdown) neu phat hien 1 anti-cheat khac cung dang
 * kiem tra di chuyen - tranh 2 plugin flag/kick song song cho cung 1 hanh vi.
 *
 * Danh sach o day chi la CAC ANTI-CHEAT PHO BIEN, khong the day du 100%. Neu server ban
 * dung 1 anti-cheat it nguoi biet khong nam trong danh sach, no se KHONG duoc tu dong nhan
 * dien - ban co the tu tat check di chuyen thu cong trong config.yml (checks.flight.enabled:
 * false, tuong tu cho speed/noslowdown) neu can.
 */
public class AntiCheatDetector {

    // Ten plugin (theo dung ten hien trong plugin.yml cua ho) cua cac anti-cheat pho bien
    // co kha nang trung lap chuc nang kiem tra di chuyen voi GrimCompanion.
    private static final List<String> KNOWN_MOVEMENT_ANTICHEATS = List.of(
            "Vulcan", "NoCheatPlus", "AAC", "Spartan", "Matrix", "Negativity",
            "Karhu", "Intave", "Verus", "Kauri", "Horion-Detector", "AntiAura"
    );

    private final GrimCompanion plugin;

    public AntiCheatDetector(GrimCompanion plugin) {
        this.plugin = plugin;
    }

    /**
     * Quet danh sach plugin dang cai, canh bao neu tim thay anti-cheat khac,
     * va ap dung config compatibility.auto-disable-movement-on-conflict neu bat.
     */
    public void detectAndWarn() {
        for (String name : KNOWN_MOVEMENT_ANTICHEATS) {
            Plugin found = plugin.getServer().getPluginManager().getPlugin(name);
            if (found == null || !found.isEnabled()) continue;

            plugin.getLogger().warning("Phat hien anti-cheat khac dang chay: " + name
                    + " - co the trung lap kiem tra di chuyen (Flight/Speed/NoSlowdown) voi GrimCompanion.");

            boolean autoDisable = plugin.getConfig().getBoolean("compatibility.auto-disable-movement-on-conflict", false);
            if (autoDisable) {
                disableMovementChecks();
                plugin.getLogger().warning("Da TU DONG TAT Flight/Speed/NoSlowdown cua GrimCompanion do "
                        + "compatibility.auto-disable-movement-on-conflict dang bat va phat hien " + name + ".");
            } else {
                plugin.getLogger().warning("Neu gap tinh trang 2 plugin cung flag 1 hanh vi, hay bat "
                        + "compatibility.auto-disable-movement-on-conflict trong config.yml, hoac tu tat "
                        + "checks.flight/speed/noslowdown.enabled thu cong.");
            }
            return; // chi can canh bao 1 lan, khong lap lai cho tung anti-cheat tim thay them
        }
    }

    private void disableMovementChecks() {
        try {
            plugin.getConfig().set("checks.flight.enabled", false);
            plugin.getConfig().set("checks.speed.enabled", false);
            plugin.getConfig().set("checks.noslowdown.enabled", false);
            plugin.getCheckManager().reloadChecks();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Khong the tu dong tat check di chuyen", ex);
        }
    }
}
