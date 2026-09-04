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
 * FlightCheck: phat hien bay bat thuong bang PredictionEngine RIENG cua GrimCompanion
 * (khong con phu thuoc GrimAC). Engine mo phong van toc Y theo trong luc moi tick; neu
 * van toc Y thuc te lech qua nhieu so voi du kien NHIEU LAN LIEN TIEP (vuot het "trust
 * buffer"), coi la bay khong hop le.
 *
 * Ket qua mo phong duoc CheckManager tinh 1 lan/tick va cache trong PlayerData
 * (xem CheckManager#dispatchReceive va PlayerData#getLastEngineTick), check nay chi doc
 * lai ket qua chu khong tu goi engine, tranh tinh trung lap voi SpeedCheck.
 */
public class FlightCheck extends Check {

    public FlightCheck(GrimCompanion plugin) {
        super(plugin, "Flight", "Phat hien bay bat thuong (khong hop le)");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION
                && event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return;
        }

        PredictionEngine.TickResult result = data.getLastEngineTick();
        if (result == null || result.skip()) return;

        if (result.verticalViolation()) {
            flag(player, String.format("Van toc Y lech %.4f so voi du kien trong luc (da vuot trust buffer)",
                    result.verticalDeviation()));
        }
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly
    }
}
