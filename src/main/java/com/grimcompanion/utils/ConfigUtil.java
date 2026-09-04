package com.grimcompanion.utils;

import com.grimcompanion.GrimCompanion;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Tien ich doc config an toan (co gia tri mac dinh, tranh NullPointerException).
 */
public class ConfigUtil {

    private final GrimCompanion plugin;

    public ConfigUtil(GrimCompanion plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
    }

    public FileConfiguration get() {
        return plugin.getConfig();
    }

    public List<String> getBlockedBrands() {
        return plugin.getConfig().getStringList("checks.clientbrand.blocked-brands");
    }

    public String getPrefix() {
        return MessageUtil.colorize(plugin.getConfig().getString("prefix", "&c[GrimCompanion] "));
    }
}
