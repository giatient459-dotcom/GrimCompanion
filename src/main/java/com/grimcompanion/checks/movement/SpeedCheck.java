package com.grimcompanion.checks.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import com.grimcompanion.engine.PredictionEngine;
import org.bukkit.entity.Player;

/**
 * SpeedCheck: phat hien di chuyen ngang nhanh bat thuong, dung CHUNG ket qua
 * PredictionEngine da duoc CheckManager tinh 1 lan/tick va cache trong PlayerData
 * (xem CheckManager#dispatchReceive) - khong tu goi engine rieng de tranh tinh
 * trung lap/sai lech state voi FlightCheck.
 *
 * Engine tu ap dung "trust buffer": chi flag khi vuot nguong toc do NHIEU LAN LIEN TIEP,
 * giam han false positive so voi cach so sanh khoang cach don gian truoc day.
 */
public class SpeedCheck extends Check {

    public SpeedCheck(GrimCompanion plugin) {
        super(plugin, "Speed", "Phat hien di chuyen nhanh bat thuong");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION
                && event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return;
        }

        PredictionEngine.TickResult result = data.getLastEngineTick();
        if (result == null || result.skip()) return;

        if (result.horizontalViolation()) {
            flag(player, String.format("Toc do ngang vuot du kien (chenh lech: %.4f block/tick, toc do cho phep: %.4f)",
                    result.horizontalDeviation(), result.allowedHorizontalSpeed()));
        }
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly
    }
}
