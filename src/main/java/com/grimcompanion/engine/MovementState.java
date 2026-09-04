package com.grimcompanion.engine;

import java.util.UUID;

/**
 * Luu trang thai mo phong vat ly di chuyen cua 1 nguoi choi, dung boi PredictionEngine.
 * Day la "bo nho" cua engine: van toc uoc tinh, vi tri gan nhat, va he thong "trust buffer"
 * (bo dem tin cay) de giam false positive do lag/jitter mang - ky thuat pho bien trong
 * cac anti-cheat that (thay vi flag ngay lan dau sai lech, tru dan buffer va chi flag
 * khi buffer can kiet, sau do buffer tu hoi phuc dan theo thoi gian di chuyen hop le).
 */
public class MovementState {

    // So "tin dung" toi da (vua la nguong tru, vua la muc hoi phuc toi da)
    public static final double MAX_TRUST = 6.0;

    private final UUID uuid;
    private boolean initialized = false;

    // Vi tri lan cap nhat gan nhat (dung lam moc tinh delta cho lan sau)
    public double lastX, lastY, lastZ;
    public boolean lastOnGround;

    // Van toc uoc tinh hien tai (dung cho truc Y la chinh, vi truc Y tuan theo trong luc ro rang nhat)
    public double velY;

    // So tick lien tiep dang o tren khong (dung de gioi han "jump token")
    public int airTicks = 0;
    // Con "jump token": moi lan cham dat duoc phep nhay 1 lan; tranh flag nhay hop le
    public boolean jumpTokenAvailable = true;

    // Trust buffer rieng cho truc doc (flight) va truc ngang (speed)
    public double verticalTrust = MAX_TRUST;
    public double horizontalTrust = MAX_TRUST;

    public long lastUpdateMs = 0L;

    public MovementState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void markInitialized() {
        this.initialized = true;
    }

    /**
     * Reset toan bo trang thai (goi khi nguoi choi teleport, doi the gioi, hoac
     * vao trang thai dac biet ma engine khong mo phong duoc: bay creative, boat, nuoc...).
     * Tranh flag oan sau khi trang thai thuc te thay doi dot ngot ma engine khong biet.
     */
    public void reset() {
        this.initialized = false;
        this.velY = 0;
        this.airTicks = 0;
        this.jumpTokenAvailable = true;
        this.verticalTrust = MAX_TRUST;
        this.horizontalTrust = MAX_TRUST;
    }
}
