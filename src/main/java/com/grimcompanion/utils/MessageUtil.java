package com.grimcompanion.utils;

import org.bukkit.ChatColor;

/**
 * Tien ich xu ly va format tin nhan (mau sac, placeholder).
 */
public class MessageUtil {

    /**
     * Thay the {player}, {check}, {vl} trong template va dich mau '&' -> ChatColor.
     */
    public static String format(String template, String player, String check, String vl) {
        if (template == null) return "";
        String result = template
                .replace("{player}", player == null ? "" : player)
                .replace("{check}", check == null ? "" : check)
                .replace("{vl}", vl == null ? "" : vl);
        return colorize(result);
    }

    public static String colorize(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
