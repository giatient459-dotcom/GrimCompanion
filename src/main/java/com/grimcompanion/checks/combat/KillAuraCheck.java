package com.grimcompanion.checks.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import com.grimcompanion.utils.MathUtil;
import org.bukkit.entity.Player;

import java.util.Deque;

/**
 * KillAuraCheck: phat hien "snap aim" (xoay camera dot ngot > nguong do) ngay truoc
 * khi tan cong entity, dac trung cua kill aura. Ngoai ra kiem tra "pattern aim":
 * cac lan xoay lap lai theo chu ky/mau bat thuong (vd goc xoay giong het nhau lien tuc).
 */
public class KillAuraCheck extends Check {

    public KillAuraCheck(GrimCompanion plugin) {
        super(plugin, "KillAura", "Phat hien auto aim / snap aim khi tan cong");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        // Cap nhat lich su xoay camera moi khi nhan goi ROTATION hoac POSITION_AND_ROTATION
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION
                || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerRotation wrapper = new WrapperPlayClientPlayerRotation(event);
            data.updateRotation(wrapper.getYaw(), wrapper.getPitch());
        }

        // Khi tan cong entity, kiem tra goc xoay ngay truoc do
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            double maxAngle = getConfigDouble("max-angle", 120);
            Deque<Float> history = data.getYawDeltaHistory();
            if (!history.isEmpty()) {
                float lastDelta = history.peekLast();
                if (lastDelta > maxAngle) {
                    flag(player, "Snap aim " + (int) lastDelta + "° khi tan cong (nguong: " + (int) maxAngle + "°)");
                    return;
                }
            }

            // Pattern aim: kiem tra 5 delta xoay gan nhat co qua giong nhau (bien dong < 1 do) khong
            if (history.size() >= 5) {
                Float[] recent = history.toArray(new Float[0]);
                int len = recent.length;
                boolean patternDetected = true;
                for (int i = len - 5; i < len - 1; i++) {
                    if (Math.abs(recent[i] - recent[i + 1]) > 1.0f) {
                        patternDetected = false;
                        break;
                    }
                }
                // Chi flag pattern neu goc xoay khong phai gan 0 (dung yen thi tu nhien se giong nhau)
                if (patternDetected && recent[len - 1] > 5.0f) {
                    flag(player, "Pattern aim phat hien (5 lan xoay giong nhau lien tuc)");
                }
            }
        }
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly
    }
}
