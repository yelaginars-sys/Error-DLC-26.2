package error.ui.csgui;

import error.ui.csgui.CsClickGuiModel.CategoryModel;

import java.util.List;

/** Animated, draggable segmented category selector for CS ClickGUI. */
public final class CsRubberSegment {
    private static final int TRACK = 0xFF14121A, TEXT = 0xFFD9D5C9, ACTIVE = 0xFF2A2035;
    private float left, right, targetLeft, targetRight;
    private boolean ready, held, live, onThumb;
    private int pressedSlot;
    private float pressX, dragOffset, dragWidth, lastX, velocity;
    private long lastSample;

    public static float width(int count, float scale) {
        return (count * 80 + 5) * scale;
    }

    public int draw(UiDrawList ui, List<CategoryModel> categories, int selected, float x, float cy, float scale, int accent,
                    int mx, int my, boolean down, boolean pressed, float dt) {
        int count = categories.size();
        if (count == 0) return selected;
        float slot = 80 * scale, inset = 2.5f * scale, trackX = x - inset, trackY = cy - 15 * scale;
        float trackW = count * slot + 2 * inset, trackH = 30 * scale;
        float min = x, max = x + count * slot;

        if (!ready) {
            left = x + selected * slot;
            right = left + slot;
            targetLeft = left;
            targetRight = right;
            ready = true;
        }

        if (pressed && hit(mx, my, trackX, trackY, trackW, trackH)) {
            held = true;
            live = false;
            pressedSlot = clamp((int) ((mx - x) / slot), 0, count - 1);
            pressX = mx;
            onThumb = mx >= left && mx <= right;
            dragOffset = mx - left;
            dragWidth = right - left;
            lastX = mx;
            velocity = 0;
            lastSample = System.nanoTime();
        }

        if (held && down && onThumb) {
            long now = System.nanoTime();
            float elapsed = (now - lastSample) / 1_000_000_000f;
            if (elapsed > .008f) {
                velocity = (mx - lastX) / elapsed;
                lastX = mx;
                lastSample = now;
            }
            if (Math.abs(mx - pressX) > 4 * scale) live = true;
            if (live) {
                float raw = mx - dragOffset;
                float maxLeft = max - dragWidth;
                if (raw < min) {
                    left = min;
                    right = min + dragWidth - rubber(min - raw, dragWidth);
                } else if (raw > maxLeft) {
                    left = maxLeft + rubber(raw - maxLeft, dragWidth);
                    right = max;
                } else {
                    left = raw;
                    right = raw + dragWidth;
                }
            }
        }

        if (!down && held) {
            held = false;
            int targetIndex = live ? clamp(Math.round(((left + right) / 2f - x) / slot), 0, count - 1) : pressedSlot;
            selected = targetIndex;
            targetLeft = x + selected * slot;
            targetRight = targetLeft + slot;
        }

        if (!held) {
            targetLeft = x + selected * slot;
            targetRight = targetLeft + slot;
            left = spring(left, targetLeft, dt * 18);
            right = spring(right, targetRight, dt * 18);
        }

        ui.roundedRect(trackX, trackY, trackW, trackH, 6 * scale, TRACK)
          .roundedOutline(trackX, trackY, trackW, trackH, 6 * scale, 1.0f, 0xFF352D42);

        float tw = right - left;
        ui.roundedRect(left, trackY + inset, tw, trackH - 2 * inset, 4.5f * scale, accent)
          .roundedOutline(left, trackY + inset, tw, trackH - 2 * inset, 4.5f * scale, 1.0f, 0xFFFFFFFF);

        for (int i = 0; i < count; i++) {
            float cx = x + i * slot + slot / 2f;
            boolean active = i == selected;
            int textColor = active ? 0xFFFFFFFF : TEXT;
            ui.text(cx, cy, 7.5f * scale, textColor, categories.get(i).name());
        }

        return selected;
    }

    private static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private static float rubber(float over, float width) {
        return width * (1 - 1 / (1 + over / (width * 0.45f)));
    }

    private static float spring(float cur, float target, float speed) {
        return cur + (target - cur) * Math.min(1.0f, speed);
    }
}
