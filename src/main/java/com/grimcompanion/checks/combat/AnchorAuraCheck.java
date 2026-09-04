package com.grimcompanion.checks.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * AnchorAuraCheck: tuong tu CrystalAuraCheck nhung ap dung cho Respawn Anchor.
 *  1) Dat/pha anchor qua nhanh (macro).
 *  2) Tuong tac (pha) VOI NHIEU HON 1 ANCHOR KHAC NHAU trong cung 1 tick server (~50ms).
 *     Vi Respawn Anchor la BLOCK (khong co entityId nhu crystal), "ID muc tieu" duoc tinh
 *     bang hash toa do block (x,y,z) de phan biet cac anchor khac nhau.
 */
public class AnchorAuraCheck extends Check {

    public AnchorAuraCheck(GrimCompanion plugin) {
        super(plugin, "AnchorAura", "Phat hien dat/pha Respawn Anchor qua nhanh hoac tuong tac nhieu anchor 1 tick");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        int minDelay = getConfigInt("min-delay", 100);

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() == Material.RESPAWN_ANCHOR) {
                long now = System.currentTimeMillis();
                long interval = data.lastAnchorPlaceInterval();
                data.addAnchorPlace(now);

                if (interval < minDelay) {
                    flag(player, "Dat anchor cach nhau " + interval + "ms (nguong: " + minDelay + "ms)");
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            Block target = player.getTargetBlockExact(5);
            if (target != null && target.getType() == Material.RESPAWN_ANCHOR) {
                long now = System.currentTimeMillis();

                // (1) Toc do pha qua nhanh
                long interval = data.lastAnchorBreakInterval();
                data.addAnchorBreak(now);
                if (interval < minDelay) {
                    flag(player, "Pha anchor cach nhau " + interval + "ms (nguong: " + minDelay + "ms)");
                }

                // (2) Tuong tac nhieu hon 1 anchor KHAC NHAU trong cung 1 tick
                long blockHash = blockPosHash(target.getX(), target.getY(), target.getZ());
                data.addAnchorInteraction(now, blockHash);
                int sameTickWindowMs = getConfigInt("same-tick-window-ms", 55);
                int distinctTargets = data.countDistinctAnchorTargets(sameTickWindowMs);
                if (distinctTargets >= 2) {
                    flag(player, "Tuong tac " + distinctTargets + " anchor khac nhau trong cung 1 tick (~"
                            + sameTickWindowMs + "ms) - khong the lam thu cong");
                }
            }
        }
    }

    /**
     * Ma hoa toa do block (x,y,z) thanh 1 so long duy nhat de dung lam "ID muc tieu"
     * (tuong tu entityId cua crystal), du de phan biet cac vi tri khac nhau trong pham vi hop ly.
     */
    private long blockPosHash(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly
    }
}
