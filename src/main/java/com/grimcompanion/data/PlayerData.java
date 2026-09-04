package com.grimcompanion.data;

import com.grimcompanion.engine.PredictionEngine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Luu tru toan bo du lieu theo doi cua mot nguoi choi.
 * Dung ConcurrentHashMap/Deque de an toan khi truy cap tu nhieu luong (PacketEvents chay async).
 * Cac danh sach thoi gian duoc gioi han kich thuoc de tranh memory leak.
 */
public class PlayerData {

    private static final int MAX_HISTORY = 200;

    private final UUID uuid;
    private final String name;

    // Cache ket qua PredictionEngine cho goi tin vi tri hien tai, de nhieu check (Flight, Speed)
    // dung CHUNG 1 lan mo phong moi tick thay vi moi check tu goi simulateTick() rieng
    // (goi 2 lan/tick se lam sai lech state cua engine vi engine cap nhat lastX/Y/Z sau moi lan goi).
    private volatile PredictionEngine.TickResult lastEngineTick;

    // ===== Click / attack tracking =====
    private final Deque<Long> clickTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> attackTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> fireworkTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> itemUseTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> blockPlaceTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> crystalPlaceTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> crystalBreakTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> anchorPlaceTimestamps = new ConcurrentLinkedDeque<>();
    private final Deque<Long> anchorBreakTimestamps = new ConcurrentLinkedDeque<>();

    // Lich su (thoi gian, ID muc tieu) cho tung lan tuong tac crystal/anchor, dung de phat hien
    // "tuong tac voi nhieu hon 1 crystal/anchor trong cung 1 tick" (khac voi toc do dat/pha don le).
    // Crystal dung entityId that (INTERACT_ENTITY), Anchor dung hash vi tri block (khong co entityId).
    private final Deque<long[]> crystalInteractionLog = new ConcurrentLinkedDeque<>(); // [timestamp, entityId]
    private final Deque<long[]> anchorInteractionLog = new ConcurrentLinkedDeque<>();  // [timestamp, blockPosHash]

    // TNT Cart: theo doi rieng, KHONG tai su dung blockPlaceTimestamps (truoc day dung nham
    // chung voi Scaffold, gay sai lech ca 2 check).
    private final Deque<Long> tntCartTimestamps = new ConcurrentLinkedDeque<>();

    // ===== Rotation tracking (cho KillAura) =====
    private float lastYaw = 0f;
    private float lastPitch = 0f;
    private long lastRotationTime = 0L;
    private final Deque<Float> yawDeltaHistory = new ConcurrentLinkedDeque<>();

    // ===== Movement tracking =====
    private double lastX, lastY, lastZ;
    private long lastMoveTime = 0L;
    private int airTicks = 0;
    private boolean wasSprinting = false;
    private boolean isBlocking = false; // dang giu chuot phai (shield/bow/food)

    // ===== Ping / keepalive =====
    private long lastKeepAliveSent = 0L;
    private long lastPingMs = 0L;
    private final Deque<Long> keepAliveRttHistory = new ConcurrentLinkedDeque<>();

    // ===== Client brand =====
    private String clientBrand = null;
    private long joinTime = System.currentTimeMillis();

    // ===== Elytra target tracking =====
    private UUID lastElytraTarget = null;
    private final Deque<Long> elytraTargetSwitchTimestamps = new ConcurrentLinkedDeque<>();

    // ===== Violations theo tung check =====
    private final Map<String, Integer> violations = new ConcurrentHashMap<>();

    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    // ---------- Generic helpers ----------

    private void trim(Deque<Long> deque) {
        while (deque.size() > MAX_HISTORY) {
            deque.pollFirst();
        }
    }

    public void addClick(long time) {
        clickTimestamps.addLast(time);
        trim(clickTimestamps);
    }

    public void addAttack(long time) {
        attackTimestamps.addLast(time);
        trim(attackTimestamps);
    }

    public void addFireworkUse(long time) {
        fireworkTimestamps.addLast(time);
        trim(fireworkTimestamps);
    }

    public void addItemUse(long time) {
        itemUseTimestamps.addLast(time);
        trim(itemUseTimestamps);
    }

    public void addBlockPlace(long time) {
        blockPlaceTimestamps.addLast(time);
        trim(blockPlaceTimestamps);
    }

    public void addCrystalPlace(long time) {
        crystalPlaceTimestamps.addLast(time);
        trim(crystalPlaceTimestamps);
    }

    public void addCrystalBreak(long time) {
        crystalBreakTimestamps.addLast(time);
        trim(crystalBreakTimestamps);
    }

    public void addAnchorPlace(long time) {
        anchorPlaceTimestamps.addLast(time);
        trim(anchorPlaceTimestamps);
    }

    public void addAnchorBreak(long time) {
        anchorBreakTimestamps.addLast(time);
        trim(anchorBreakTimestamps);
    }

    // ---------- TNT Cart ----------

    public void addTntCartUse(long time) {
        tntCartTimestamps.addLast(time);
        trim(tntCartTimestamps);
    }

    public double getTntCartCPS() {
        return getCPS(tntCartTimestamps);
    }

    public double getTntCartIntervalStdDev(int sampleSize) {
        return intervalStdDev(tntCartTimestamps, sampleSize);
    }

    // ---------- Crystal / Anchor multi-target trong cung 1 tick ----------

    /**
     * Ghi nhan 1 lan tuong tac voi crystal (entityId that cua End Crystal).
     * Tu dong don dep cac entry cu hon 500ms de deque khong phinh to.
     */
    public void addCrystalInteraction(long time, int entityId) {
        crystalInteractionLog.addLast(new long[]{time, entityId});
        pruneOld(crystalInteractionLog, 500);
    }

    /**
     * Ghi nhan 1 lan tuong tac voi anchor, dung hash vi tri block (x,y,z) lam "ID"
     * vi Respawn Anchor la block chu khong co entityId nhu crystal.
     */
    public void addAnchorInteraction(long time, long blockPosHash) {
        anchorInteractionLog.addLast(new long[]{time, blockPosHash});
        pruneOld(anchorInteractionLog, 500);
    }

    /**
     * Dem so LUONG MUC TIEU KHAC NHAU (entityId/blockHash khac nhau) da tuong tac trong
     * khoang thoi gian windowMs gan nhat - dung de phat hien "tuong tac voi nhieu hon 1
     * crystal/anchor trong cung 1 tick" (server tick ~= 50ms).
     */
    public int countDistinctCrystalTargets(long windowMs) {
        return countDistinctTargets(crystalInteractionLog, windowMs);
    }

    public int countDistinctAnchorTargets(long windowMs) {
        return countDistinctTargets(anchorInteractionLog, windowMs);
    }

    private int countDistinctTargets(Deque<long[]> log, long windowMs) {
        long now = System.currentTimeMillis();
        Set<Long> distinctIds = new HashSet<>();
        for (long[] entry : log) {
            if (now - entry[0] <= windowMs) {
                distinctIds.add(entry[1]);
            }
        }
        return distinctIds.size();
    }

    /**
     * Xoa cac entry cu hon maxAgeMs khoi 1 deque [timestamp, id] - tranh deque phinh to
     * vo han neu nguoi choi khong tuong tac gi trong thoi gian dai (deque chi co it entry
     * moi nen khong the dung trim() theo kich thuoc co dinh nhu cac deque khac).
     */
    private void pruneOld(Deque<long[]> log, long maxAgeMs) {
        long now = System.currentTimeMillis();
        while (!log.isEmpty() && now - log.peekFirst()[0] > maxAgeMs) {
            log.pollFirst();
        }
    }

    // ---------- Keep-alive RTT history (Ping spoof) ----------

    public void addKeepAliveRtt(long rtt) {
        keepAliveRttHistory.addLast(rtt);
        trim(keepAliveRttHistory);
    }

    /**
     * Do lech chuan cua cac gia tri RTT gan nhat. RTT that (mang thuc) luon dao dong tu nhien;
     * RTT gia mao/tra loi tu dong ngay lap tuc thuong rat deu (stddev gan 0).
     */
    public double getKeepAliveRttStdDev(int sampleSize) {
        List<Long> values = new ArrayList<>(keepAliveRttHistory);
        if (values.size() < sampleSize) return -1;
        List<Long> recent = values.subList(Math.max(0, values.size() - sampleSize), values.size());
        double mean = recent.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = recent.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    public double getKeepAliveRttAverage(int sampleSize) {
        List<Long> values = new ArrayList<>(keepAliveRttHistory);
        if (values.isEmpty()) return -1;
        List<Long> recent = values.subList(Math.max(0, values.size() - sampleSize), values.size());
        return recent.stream().mapToLong(Long::longValue).average().orElse(-1);
    }

    /**
     * Tinh CPS (click per second) dua tren cac click trong 1000ms gan nhat.
     */
    public double getCPS(Deque<Long> deque) {
        long now = System.currentTimeMillis();
        long count = deque.stream().filter(t -> now - t <= 1000).count();
        return count;
    }

    public double getClickCPS() {
        return getCPS(clickTimestamps);
    }

    public double getAttackCPS() {
        return getCPS(attackTimestamps);
    }

    public double getFireworkCPS() {
        return getCPS(fireworkTimestamps);
    }

    public double getItemUseCPS() {
        return getCPS(itemUseTimestamps);
    }

    public double getBlockPlaceCPS() {
        return getCPS(blockPlaceTimestamps);
    }

    /**
     * Tinh do lech chuan (standard deviation) giua cac khoang thoi gian click gan nhat.
     * Dung de phan biet click that (stddev cao, khong deu) voi autoclicker (stddev thap, rat deu).
     */
    public double getClickIntervalStdDev(int sampleSize) {
        return intervalStdDev(clickTimestamps, sampleSize);
    }

    /**
     * Do lech chuan giua cac lan dung firework gan nhat - dung de phat hien AutoFirework
     * (bam deu nhu may) tuong tu cach AutoClicker phat hien click deu.
     */
    public double getFireworkIntervalStdDev(int sampleSize) {
        return intervalStdDev(fireworkTimestamps, sampleSize);
    }

    /**
     * Do lech chuan giua cac lan dung item (Ender Pearl...) gan nhat - dung de phat hien
     * macro/bind nut chuot (vd MiddleClickExtra): nguoi that click deu tay it nhat cung co
     * dao dong nho, trong khi macro phan mem/phan cung thuong deu bat thuong.
     */
    public double getItemUseIntervalStdDev(int sampleSize) {
        return intervalStdDev(itemUseTimestamps, sampleSize);
    }

    /**
     * Ham dung chung: tinh stddev cua khoang cach thoi gian giua cac timestamp gan nhat
     * trong 1 deque bat ky. Tra ve -1 neu chua du du lieu (< sampleSize + 1 diem).
     */
    private double intervalStdDev(Deque<Long> deque, int sampleSize) {
        List<Long> times = new ArrayList<>(deque);
        if (times.size() < sampleSize + 1) return -1;

        List<Long> recent = times.subList(Math.max(0, times.size() - sampleSize - 1), times.size());
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < recent.size(); i++) {
            intervals.add(recent.get(i) - recent.get(i - 1));
        }

        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = intervals.stream()
                .mapToDouble(i -> Math.pow(i - mean, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }

    public long lastCrystalPlaceInterval() {
        return intervalOfLast(crystalPlaceTimestamps);
    }

    public long lastCrystalBreakInterval() {
        return intervalOfLast(crystalBreakTimestamps);
    }

    public long lastAnchorPlaceInterval() {
        return intervalOfLast(anchorPlaceTimestamps);
    }

    public long lastAnchorBreakInterval() {
        return intervalOfLast(anchorBreakTimestamps);
    }

    private long intervalOfLast(Deque<Long> deque) {
        if (deque.size() < 2) return Long.MAX_VALUE;
        Iterator<Long> descIterator = deque.descendingIterator();
        long last = descIterator.next();
        long prev = descIterator.next();
        return last - prev;
    }

    // ---------- Rotation ----------

    public void updateRotation(float yaw, float pitch) {
        long now = System.currentTimeMillis();
        if (lastRotationTime != 0L) {
            float deltaYaw = Math.abs(normalizeAngle(yaw - lastYaw));
            yawDeltaHistory.addLast((long) deltaYaw);
            while (yawDeltaHistory.size() > 40) yawDeltaHistory.pollFirst();
        }
        this.lastYaw = yaw;
        this.lastPitch = pitch;
        this.lastRotationTime = now;
    }

    private float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public Deque<Float> getYawDeltaHistory() {
        return yawDeltaHistory;
    }

    // ---------- Movement ----------

    public void updatePosition(double x, double y, double z) {
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastMoveTime = System.currentTimeMillis();
    }

    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getLastZ() { return lastZ; }

    public int getAirTicks() { return airTicks; }
    public void setAirTicks(int airTicks) { this.airTicks = airTicks; }
    public void incrementAirTicks() { this.airTicks++; }
    public void resetAirTicks() { this.airTicks = 0; }

    public boolean isWasSprinting() { return wasSprinting; }
    public void setWasSprinting(boolean wasSprinting) { this.wasSprinting = wasSprinting; }

    public boolean isBlocking() { return isBlocking; }
    public void setBlocking(boolean blocking) { isBlocking = blocking; }

    // ---------- Ping ----------

    public void setLastKeepAliveSent(long time) { this.lastKeepAliveSent = time; }
    public long getLastKeepAliveSent() { return lastKeepAliveSent; }

    public void setLastPingMs(long ping) { this.lastPingMs = ping; }
    public long getLastPingMs() { return lastPingMs; }

    // ---------- Brand ----------

    public String getClientBrand() { return clientBrand; }
    public void setClientBrand(String brand) { this.clientBrand = brand; }
    public long getJoinTime() { return joinTime; }

    // ---------- Elytra target ----------

    public UUID getLastElytraTarget() { return lastElytraTarget; }

    public void updateElytraTarget(UUID target) {
        if (target != null && !target.equals(lastElytraTarget)) {
            elytraTargetSwitchTimestamps.addLast(System.currentTimeMillis());
            while (elytraTargetSwitchTimestamps.size() > 50) elytraTargetSwitchTimestamps.pollFirst();
        }
        this.lastElytraTarget = target;
    }

    public int countElytraTargetSwitches(long windowMs) {
        long now = System.currentTimeMillis();
        return (int) elytraTargetSwitchTimestamps.stream().filter(t -> now - t <= windowMs).count();
    }

    // ---------- Prediction engine cache ----------

    public PredictionEngine.TickResult getLastEngineTick() {
        return lastEngineTick;
    }

    public void setLastEngineTick(PredictionEngine.TickResult result) {
        this.lastEngineTick = result;
    }

    // ---------- Violations ----------

    public int incrementViolation(String checkName) {
        return violations.merge(checkName, 1, Integer::sum);
    }

    public int getViolation(String checkName) {
        return violations.getOrDefault(checkName, 0);
    }

    public void resetViolation(String checkName) {
        violations.remove(checkName);
    }

    public void resetAllViolations() {
        violations.clear();
    }

    public Map<String, Integer> getAllViolations() {
        return violations;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
}
