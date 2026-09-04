package com.grimcompanion.checks.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * AutoClickerCheck: phat hien click chuot (attack/animation) qua deu hoac qua nhanh.
 * Hai tieu chi:
 *  1) CPS vuot nguong (vd > 20) trong 1 giay.
 *  2) Do lech chuan (stddev) giua cac khoang cach click qua thap -> click qua deu,
 *     dac trung cua autoclicker phan cung/phan mem, khac voi nguoi that (co dao dong tu nhien).
 */
public class AutoClickerCheck extends Check {

    public AutoClickerCheck(GrimCompanion plugin) {
        super(plugin, "AutoClicker", "Phat hien click chuot tu dong (auto click)");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        // Goi tin ANIMATION (swing arm) dai dien cho moi lan click chuot trai
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            long now = System.currentTimeMillis();
            data.addClick(now);

            double maxCps = getConfigDouble("max-cps", 20);
            double cps = data.getClickCPS();

            if (cps > maxCps) {
                flag(player, "CPS = " + (int) cps + " (nguong: " + (int) maxCps + ")");
                return;
            }

            int sampleSize = getConfigInt("sample-size", 20);
            double minStdDev = getConfigDouble("min-stddev", 20);
            double stddev = data.getClickIntervalStdDev(sampleSize);

            // stddev = -1 nghia la chua du du lieu de tinh
            if (stddev >= 0 && stddev < minStdDev && cps >= 8) {
                // Chi flag khi CPS cung du cao (>=8), tranh false positive khi nguoi choi click thua thot
                flag(player, "Click qua deu, stddev = " + String.format("%.2f", stddev) + "ms (nguong: " + minStdDev + "ms)");
            }
        }
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly
    }
}
