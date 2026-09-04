package com.grimcompanion.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * Diem vao trung tam cho tat ca packet PacketEvents bat duoc.
 * Chuyen tiep den CheckManager de dispatch toi tung check.
 * Cung cap them mot so xu ly chung (vd: cap nhat client brand, keepalive).
 *
 * LUU Y VE API: tu PacketEvents ~2.9+ (bao gom 2.13.0 dang dung), "PacketListener" la
 * mot INTERFACE (khong con la "PacketListenerAbstract" de extends nhu ban cu 2.5.0).
 * Class nay implement thang interface do; khi dang ky voi priority, dung ham
 * asAbstract(priority) (default method co san tren interface) - xem GrimCompanion.java.
 */
public class PacketListener implements com.github.retrooper.packetevents.event.PacketListener {

    private final GrimCompanion plugin;

    public PacketListener(GrimCompanion plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Bo qua neu nguoi choi da roi server hoac chua load xong
        if (player == null || !player.isOnline()) return;

        try {
            plugin.getCheckManager().dispatchReceive(event, player);
        } catch (Exception ex) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("Loi xu ly packet receive: " + ex.getMessage());
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (player == null || !player.isOnline()) return;

        try {
            plugin.getCheckManager().dispatchSend(event, player);
        } catch (Exception ex) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("Loi xu ly packet send: " + ex.getMessage());
            }
        }
    }
}
