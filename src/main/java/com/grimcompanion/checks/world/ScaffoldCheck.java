package com.grimcompanion.checks.world;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import org.bukkit.entity.Player;

/**
 * ScaffoldCheck: phat hien dat block (di chuyen kieu "scaffold"/bridge tu dong)
 * qua nhanh so voi kha nang click chuot cua nguoi that. Ket hop kiem tra nguoi
 * choi co dang di lui/sang ngang trong khi dat block lien tuc phia duoi chan
 * (dac trung cua auto-scaffold/tower).
 */
public class ScaffoldCheck extends Check {

    public ScaffoldCheck(GrimCompanion plugin) {
        super(plugin, "Scaffold", "Phat hien dat block qua nhanh (scaffold)");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) return;

        org.bukkit.inventory.ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !hand.getType().isBlock()) return;

        long now = System.currentTimeMillis();
        data.addBlockPlace(now);

        double maxCps = getConfigDouble("max-cps", 5);
        double cps = data.getBlockPlaceCPS();

        if (cps > maxCps) {
            flag(player, "Dat block " + (int) cps + " lan/giay (nguong: " + (int) maxCps + ")");
        }
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly
    }
}
