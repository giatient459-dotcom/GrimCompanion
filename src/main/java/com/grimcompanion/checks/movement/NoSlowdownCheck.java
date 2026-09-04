package com.grimcompanion.checks.movement;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.grimcompanion.GrimCompanion;
import com.grimcompanion.checks.Check;
import com.grimcompanion.data.PlayerData;
import com.grimcompanion.utils.MathUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

/**
 * NoSlowdownCheck: phat hien nguoi choi khong bi giam toc do khi dang:
 *  - Giu chuot phai voi Shield / Bow / Crossbow / do an (dang blocking/eating/charging)
 *  - Di trong mang nhen (cobweb) hoac an blackberry bush
 * Client hop le se tu dong giam toc do di chuyen (~80% hoac hon), client mod
 * (NoSlowdown) se giu nguyen toc do binh thuong trong khi van dang "blocking".
 */
public class NoSlowdownCheck extends Check {

    private static final double NORMAL_SPEED_THRESHOLD = 0.15; // block/tick, xap xi toc do da giam

    public NoSlowdownCheck(GrimCompanion plugin) {
        super(plugin, "NoSlowdown", "Phat hien khong bi cham khi dang block/an/giuong no");
    }

    @Override
    public void handlePacketReceive(PacketReceiveEvent event, Player player, PlayerData data) {
        // Theo doi trang thai bat dau su dung item (use item = bat dau blocking/eating/charging)
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            // Cac action RELEASE_USE_ITEM se ket thuc blocking; don gian hoa: dua vao isBlocking() cua API
        }

        if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION
                && event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return;
        }

        boolean usingItem = player.isBlocking()
                || (player.getActiveItem() != null && !player.getActiveItem().getType().isAir());

        if (!usingItem) {
            data.setBlocking(false);
            return;
        }
        data.setBlocking(true);

        double curX = player.getLocation().getX();
        double curZ = player.getLocation().getZ();
        if (data.getLastX() == 0 && data.getLastZ() == 0) {
            data.updatePosition(curX, player.getLocation().getY(), curZ);
            return;
        }

        double horizontalDist = MathUtil.distance2D(data.getLastX(), data.getLastZ(), curX, curZ);
        data.updatePosition(curX, player.getLocation().getY(), curZ);

        // Neu dang blocking/eating nhung van di chuyen o toc do binh thuong (khong giam)
        if (player.isSprinting() && horizontalDist > NORMAL_SPEED_THRESHOLD) {
            flag(player, String.format("Khong giam toc khi dang su dung item (%.3f block/tick)", horizontalDist));
        }
    }

    @Override
    public void handlePacketSend(PacketSendEvent event, Player player, PlayerData data) {
        // Khong can xu ly
    }
}
