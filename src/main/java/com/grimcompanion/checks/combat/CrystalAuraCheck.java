package com.grimcompanion.checks.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * CrystalAuraCheck: phat hien cac hanh vi Crystal PvP bat thuong:
 *  1) Dat/pha End Crystal qua nhanh (macro/auto crystal) - so sanh khoang cach thoi gian
 *     voi nguong toi thieu (min-delay).
 *  2) Tuong tac (pha) VOI NHIEU HON 1 CRYSTAL KHAC NHAU trong cung 1 tick server (~50ms) -
 *     dau hieu "double pop" tu dong ma nguoi choi that khong the click chinh xac 2 muc tieu
 *     khac nhau trong cung 1 tick bang 1 click chuot don.
 *
 * SUA LOI so voi ban truoc: khong con dung heuristic "co crystal nao do gan day" (isLookingAtCrystal)
 * ma resolve DUNG entity ma client bao cao tuong tac toi qua entityId trong goi tin INTERACT_ENTITY,
 * roi kiem tra entity do co that la END_CRYSTAL hay khong - chinh xac hon nhieu.
 */
public class CrystalAuraCheck extends Check {

    public CrystalAuraCheck(GrimCompanion plugin) {
        super(plugin, "CrystalAura", "Phat hien dat/pha End Crystal qua nhanh hoac tuong tac nhieu crystal 1 tick");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        int minDelay = getConfigInt("min-delay", 100);

        // ===== Dat crystal =====
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() == Material.END_CRYSTAL) {
                long now = System.currentTimeMillis();
                long interval = data.lastCrystalPlaceInterval();
                data.addCrystalPlace(now);

                if (interval < minDelay) {
                    flag(player, "Dat crystal cach nhau " + interval + "ms (nguong: " + minDelay + "ms)");
                }
            }
            return;
        }

        // ===== Pha crystal (right-click/attack vao entity END_CRYSTAL that) =====
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

            Entity target = findEntityById(player, wrapper.getEntityId());
            if (target == null || target.getType() != EntityType.END_CRYSTAL) return;

            long now = System.currentTimeMillis();

            // (1) Toc do pha qua nhanh
            long interval = data.lastCrystalBreakInterval();
            data.addCrystalBreak(now);
            if (interval < minDelay) {
                flag(player, "Pha crystal cach nhau " + interval + "ms (nguong: " + minDelay + "ms)");
            }

            // (2) Tuong tac nhieu hon 1 crystal KHAC NHAU trong cung 1 tick
            data.addCrystalInteraction(now, target.getEntityId());
            int sameTickWindowMs = getConfigInt("same-tick-window-ms", 55); // ~1 tick server
            int distinctTargets = data.countDistinctCrystalTargets(sameTickWindowMs);
            if (distinctTargets >= 2) {
                flag(player, "Tuong tac " + distinctTargets + " crystal khac nhau trong cung 1 tick (~"
                        + sameTickWindowMs + "ms) - khong the lam thu cong");
            }
        }
    }

    private Entity findEntityById(Player player, int entityId) {
        for (Entity e : player.getWorld().getEntities()) {
            if (e.getEntityId() == entityId) return e;
        }
        return null;
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly packet gui di cho check nay
    }
}
