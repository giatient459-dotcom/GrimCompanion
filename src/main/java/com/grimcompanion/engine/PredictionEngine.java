package com.grimcompanion.engine;

import com.grimcompanion.GrimCompanion;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PredictionEngine: engine mo phong vat ly di chuyen RIENG cua GrimCompanion.
 *
 * Muc tieu: khong phu thuoc vao GrimAC, tu tinh toan "vi tri du kien" cua nguoi choi
 * o tick tiep theo dua tren cong thuc vat ly Minecraft don gian hoa (trong luc, ma sat,
 * luc nhay), roi so sanh voi vi tri thuc te client bao cao qua packet. Neu sai lech
 * vuot nguong NHIEU LAN LIEN TUC (qua het "trust buffer") thi moi flag - giup giam
 * false positive do lag/jitter mang so voi cach so sanh tuc thoi tung tick.
 *
 * GIOI HAN CAN LUU Y (ghi ro de tranh hieu lam day la ban sao NMS chinh xac):
 *  - Day la mo hinh DON GIAN HOA, khong bao gom day du: nuoc, dung nham, mang nhen,
 *    bang truot (slipperiness khac nhau tung block), thuyen, ngua, cac hieu ung tiem an khac.
 *  - Khi phat hien nguoi choi o trong cac trang thai phuc tap tren, engine se tu SKIP
 *    (khong du doan, khong flag) thay vi doan sai va gay false positive.
 *  - Muc dich la lam LOP BO SUNG doc lap ben canh GrimAC, khong thay the hoan toan
 *    prediction engine day du cua GrimAC.
 */
public class PredictionEngine {

    // Hang so vat ly xap xi theo Minecraft (block/tick) - khong config duoc vi day la
    // dinh luat vat ly co dinh cua game, khac voi tolerance (nguong dung sai) o duoi.
    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;       // he so nhan van toc Y moi tick khi tren khong
    private static final double JUMP_VELOCITY = 0.42;  // van toc Y ban dau khi nhay thuong

    // Toc do di chuyen ngang co so (block/tick), da tinh gan dung cho di bo tren dat bang phang
    private static final double BASE_WALK_SPEED = 0.2158;
    private static final double SPRINT_MULTIPLIER = 1.30;
    private static final double SNEAK_MULTIPLIER = 0.30;

    private final GrimCompanion plugin;
    private final Map<UUID, MovementState> states = new ConcurrentHashMap<>();

    public PredictionEngine(GrimCompanion plugin) {
        this.plugin = plugin;
    }

    private double verticalTolerance() {
        return plugin.getConfig().getDouble("engine.vertical-tolerance", 0.035);
    }

    private double horizontalToleranceMultiplier() {
        return plugin.getConfig().getDouble("engine.horizontal-tolerance-multiplier", 1.25);
    }

    private double maxTrustBuffer() {
        return plugin.getConfig().getDouble("engine.max-trust-buffer", MovementState.MAX_TRUST);
    }

    public MovementState getState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), MovementState::new);
    }

    public void reset(Player player) {
        MovementState state = states.get(player.getUniqueId());
        if (state != null) state.reset();
    }

    public void remove(Player player) {
        states.remove(player.getUniqueId());
    }

    /**
     * Ket qua mo phong 1 tick: co du lieu de cac check (Flight/Speed) tu quyet dinh flag hay khong.
     */
    public record TickResult(
            boolean skip,                 // true = engine chu dong bo qua tick nay (trang thai qua phuc tap de mo phong)
            boolean verticalViolation,     // true = da vuot het vertical trust buffer
            boolean horizontalViolation,   // true = da vuot het horizontal trust buffer
            double verticalDeviation,
            double horizontalDeviation,
            double allowedHorizontalSpeed
    ) {
        public static TickResult skipped() {
            return new TickResult(true, false, false, 0, 0, 0);
        }
    }

    /**
     * Goi moi khi nhan duoc goi tin vi tri moi (PLAYER_POSITION / PLAYER_POSITION_AND_ROTATION).
     * Day la trai tim cua engine: cap nhat trang thai + tra ve ket qua so sanh du doan vs thuc te.
     */
    public TickResult simulateTick(Player player, double newX, double newY, double newZ, boolean onGround) {
        MovementState state = getState(player);

        // Cac trang thai qua phuc tap / khong dang tin cay de mo phong don gian -> bo qua an toan
        if (isUnsupportedState(player)) {
            state.reset();
            return TickResult.skipped();
        }

        if (!state.isInitialized()) {
            initState(state, player, newX, newY, newZ, onGround);
            return TickResult.skipped();
        }

        // ===== Truc doc (Y) - trong luc =====
        double actualVelY = newY - state.lastY;
        double expectedVelY = computeExpectedVelY(state, player, onGround);
        double verticalDeviation = Math.abs(actualVelY - expectedVelY);

        boolean verticalViolation = false;
        double vTolerance = verticalTolerance();
        double maxTrust = maxTrustBuffer();
        if (verticalDeviation > vTolerance) {
            state.verticalTrust -= 1.0;
            if (state.verticalTrust <= 0) {
                verticalViolation = true;
                state.verticalTrust = maxTrust * 0.5; // hoi phuc mot phan sau khi flag, tranh spam flag lien tuc
            }
        } else {
            state.verticalTrust = Math.min(maxTrust, state.verticalTrust + 0.5);
        }

        // Cap nhat van toc Y du kien cho tick sau dua tren van toc THUC TE (bam theo client
        // de tranh tich luy sai so, engine chi dung de PHAT HIEN lech dot ngot chu khong ep buoc)
        state.velY = actualVelY;

        // Cap nhat jump token: neu vua cham dat thi "sac lai" 1 luot nhay
        if (onGround) {
            state.jumpTokenAvailable = true;
            state.airTicks = 0;
        } else {
            state.airTicks++;
        }

        // ===== Truc ngang (X/Z) =====
        double dx = newX - state.lastX;
        double dz = newZ - state.lastZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        double allowedSpeed = computeAllowedHorizontalSpeed(player);
        double threshold = allowedSpeed * horizontalToleranceMultiplier();

        boolean horizontalViolation = false;
        double horizontalDeviation = horizontalDist - threshold;

        // Chi tinh vi pham ngang khi dang o tren mat dat (tren khong co du bien so phuc tap
        // hon - da co Vertical check rieng lo phan bay; tranh chong cheo/false positive).
        if (onGround && horizontalDist > threshold) {
            state.horizontalTrust -= 1.0;
            if (state.horizontalTrust <= 0) {
                horizontalViolation = true;
                state.horizontalTrust = maxTrust * 0.5;
            }
        } else {
            state.horizontalTrust = Math.min(maxTrust, state.horizontalTrust + 0.5);
        }

        // Cap nhat vi tri cuoi cung
        state.lastX = newX;
        state.lastY = newY;
        state.lastZ = newZ;
        state.lastOnGround = onGround;
        state.lastUpdateMs = System.currentTimeMillis();

        return new TickResult(false, verticalViolation, horizontalViolation, verticalDeviation, horizontalDeviation, allowedSpeed);
    }

    private void initState(MovementState state, Player player, double x, double y, double z, boolean onGround) {
        state.lastX = x;
        state.lastY = y;
        state.lastZ = z;
        state.lastOnGround = onGround;
        state.velY = 0;
        state.markInitialized();
    }

    /**
     * Tinh van toc Y du kien cho tick nay dua tren trang thai truoc do.
     * Xu ly rieng cac truong hop: dang o mat dat (co the vua nhay), dang roi tu do,
     * co hieu ung Jump Boost / Levitation / Slow Falling lam thay doi cong thuc.
     */
    private double computeExpectedVelY(MovementState state, Player player, boolean onGround) {
        // Cac hieu ung lam thay doi trong luc ro ret -> nong tolerance len rat cao thay vi tinh chinh xac
        // cong thuc rieng cho tung hieu ung (do phuc tap), tranh false positive.
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return state.velY; // gia dinh van toc giu nguyen, tolerance rieng se duoc ap dung ben ngoai neu can
        }

        if (state.lastOnGround && !onGround && state.jumpTokenAvailable) {
            // Vua roi mat dat va con luot nhay -> co the la cu nhay hop le
            state.jumpTokenAvailable = false;
            int jumpBoostAmp = getAmplifier(player, PotionEffectType.JUMP_BOOST);
            double jumpVel = JUMP_VELOCITY + (jumpBoostAmp >= 0 ? (jumpBoostAmp + 1) * 0.1 : 0);
            return jumpVel;
        }

        if (onGround) {
            return 0.0;
        }

        // Dang o tren khong, khong vua nhay -> ap dung cong thuc roi tu do co ban
        return (state.velY - GRAVITY) * AIR_DRAG;
    }

    /**
     * Tinh toc do ngang toi da hop le (block/tick) dua tren trang thai hien tai cua nguoi choi
     * (sprint, sneak, potion Speed). Don gian hoa: khong tinh gia toc tang dan tu 0 (nguoi choi
     * that can vai tick de dat toc do toi da), nen threshold da nhan them he so dung sai o tren.
     */
    private double computeAllowedHorizontalSpeed(Player player) {
        double speed = BASE_WALK_SPEED;

        if (player.isSprinting()) speed *= SPRINT_MULTIPLIER;
        if (player.isSneaking()) speed *= SNEAK_MULTIPLIER;

        int speedAmp = getAmplifier(player, PotionEffectType.SPEED);
        if (speedAmp >= 0) {
            speed *= (1 + 0.2 * (speedAmp + 1));
        }
        int slowAmp = getAmplifier(player, PotionEffectType.SLOWNESS);
        if (slowAmp >= 0) {
            speed *= Math.max(0.0, 1 - 0.15 * (slowAmp + 1));
        }

        return speed;
    }

    private int getAmplifier(Player player, PotionEffectType type) {
        if (!player.hasPotionEffect(type)) return -1;
        return player.getActivePotionEffects().stream()
                .filter(e -> e.getType().equals(type))
                .findFirst()
                .map(org.bukkit.potion.PotionEffect::getAmplifier)
                .orElse(-1);
    }

    /**
     * Cac trang thai ma engine don gian nay KHONG mo phong duoc dang tin cay:
     * bay (creative/spectator/allow-flight), luon (elytra), boi, leo, o trong phuong tien,
     * o trong nuoc/nham (co cong thuc rieng phuc tap hon nhieu). Tra ve true -> engine bo qua tick.
     */
    private boolean isUnsupportedState(Player player) {
        return player.getAllowFlight()
                || player.isFlying()
                || player.isGliding()
                || player.isSwimming()
                || player.isClimbing()
                || player.isInsideVehicle()
                || player.isInWater()
                || player.isInLava()
                || player.getGameMode().name().equals("CREATIVE")
                || player.getGameMode().name().equals("SPECTATOR");
    }
}
