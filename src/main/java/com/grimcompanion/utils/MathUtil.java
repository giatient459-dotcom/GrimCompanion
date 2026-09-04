package com.grimcompanion.utils;

/**
 * Cac ham toan hoc dung chung cho nhieu check (khoang cach, goc, chuan hoa...).
 */
public class MathUtil {

    public static double distance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Chuan hoa goc ve khoang (-180, 180]
     */
    public static float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle >= 180f) angle -= 360f;
        if (angle < -180f) angle += 360f;
        return angle;
    }

    public static double average(java.util.Collection<Long> values) {
        if (values.isEmpty()) return 0;
        return values.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    public static double stdDev(java.util.Collection<Long> values) {
        if (values.isEmpty()) return 0;
        double mean = average(values);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }
}
