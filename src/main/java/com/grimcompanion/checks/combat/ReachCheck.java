package com.grimcompanion.checks.combat;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import com.grimcompanion.utils.MathUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * ReachCheck: phat hien tan cong entity o khoang cach vuot qua gioi han cho phep
 * (mac dinh Minecraft: ~3.0 block Survival, cau hinh mac dinh 3.2 de tru sai so mang).
 * Tinh khoang cach tu mat nguoi choi (eye location) den hitbox gan nhat cua entity muc tieu.
 */
public class ReachCheck extends Check {

    public ReachCheck(GrimCompanion plugin) {
        super(plugin, "Reach", "Phat hien tan cong tu khoang cach qua xa");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        // Chi kiem tra hanh dong ATTACK, bo qua INTERACT (right click)
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

        Entity target = findEntityById(player, wrapper.getEntityId());
        if (target == null) return;

        double maxReach = getConfigDouble("max-reach", 3.2);

        Vector eyeLoc = player.getEyeLocation().toVector();
        // Khoang cach toi tam hitbox (xap xi bang cach lay center cua bounding box)
        Vector targetCenter = target.getBoundingBox().getCenter();
        double distance = eyeLoc.distance(targetCenter);

        // Tru bot ban kinh hitbox trung binh (~0.3-0.5) de gan voi cach GrimAC tinh reach thuc te
        double effectiveDistance = distance - (target.getWidth() / 2.0);

        if (effectiveDistance > maxReach) {
            flag(player, String.format("Reach = %.2f blocks (nguong: %.2f)", effectiveDistance, maxReach));
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
        // Khong can xu ly
    }
}
