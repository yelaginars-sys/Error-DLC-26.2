package error.ui.modern;

import java.util.HashMap;
import java.util.Map;

public final class ModernAnim {
    private static final Map<Object, float[]> OBJECTS = new HashMap<>();
    private static long timestamp = System.nanoTime();
    private static float frameDelta = 0.016666668F;

    private ModernAnim() {
    }

    public static void beginFrame() {
        long now = System.nanoTime();
        float delta = (float) (now - timestamp) / 1.0E9F;
        timestamp = now;
        frameDelta = Math.max(1.0E-4F, Math.min(0.1F, delta));
    }

    public static float delta() {
        return frameDelta;
    }

    public static float approach(float current, float target, float speed) {
        if (!ModernTheme.animations()) {
            return target;
        }
        float factor = 1.0F - (float) Math.exp(-speed * frameDelta);
        float next = current + (target - current) * factor;
        return Math.abs(target - next) < 5.0E-4F ? target : next;
    }

    public static float value(Object key, float target, float speed) {
        float[] array = OBJECTS.get(key);
        if (array == null) {
            array = new float[]{target};
            OBJECTS.put(key, array);
            return target;
        } else {
            array[0] = approach(array[0], target, speed);
            return array[0];
        }
    }

    public static void set(Object key, float value) {
        OBJECTS.computeIfAbsent(key, k -> new float[1])[0] = value;
    }

    public static float get(Object key, float fallback) {
        float[] array = OBJECTS.get(key);
        return array == null ? fallback : array[0];
    }

    public static boolean has(Object key) {
        return OBJECTS.containsKey(key);
    }

    public static void resetPrefix(String prefix) {
        OBJECTS.keySet().removeIf(key -> key instanceof String str && str.startsWith(prefix));
    }

    public static float ease(float t) {
        float clamped = Math.max(0.0F, Math.min(1.0F, t));
        return 1.0F - (float) Math.pow(1.0F - clamped, 3.0);
    }
}
