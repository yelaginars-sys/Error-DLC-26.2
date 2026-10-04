package error.util.display.blur;

import error.util.display.color.Color;
import error.util.display.batch.DisplayBatcher;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Blur {
    private static final float MAX_RADIUS = 31.5F;
    private static final float OPACITY_STEPS = 31.0F;

    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private BlurType type = BlurType.KAWASE;
    private int strength = 3;
    private int tintTopLeft;
    private int tintTopRight;
    private int tintBottomRight;
    private int tintBottomLeft;
    private float alpha = 1.0F;

    private Blur() {
    }

    public static Blur create() {
        return new Blur();
    }

    public static Blur of(float x, float y, float width, float height) {
        return new Blur().bounds(x, y, width, height);
    }

    public Blur position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Blur size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Blur bounds(float x, float y, float width, float height) {
        return position(x, y).size(width, height);
    }

    public Blur radius(float radius) {
        return radius(radius, radius, radius, radius);
    }

    public Blur radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public Blur radiusTop(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public Blur radiusBottom(float radius) {
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Blur radiusLeft(float radius) {
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public Blur radiusRight(float radius) {
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Blur type(BlurType type) {
        this.type = type;
        return this;
    }

    public Blur strength(int strength) {
        this.strength = Math.clamp(strength, 1, BlurType.MAX_STRENGTH);
        return this;
    }

    public Blur tint(int argb) {
        return corners(argb, argb, argb, argb);
    }

    public Blur tint(Color color) {
        return tint(color.argb());
    }

    public Blur verticalGradient(Color top, Color bottom) {
        return corners(top.argb(), top.argb(), bottom.argb(), bottom.argb());
    }

    public Blur horizontalGradient(Color left, Color right) {
        return corners(left.argb(), right.argb(), right.argb(), left.argb());
    }

    public Blur corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.tintTopLeft = topLeft;
        this.tintTopRight = topRight;
        this.tintBottomRight = bottomRight;
        this.tintBottomLeft = bottomLeft;
        return this;
    }

    public Blur alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0F, 1.0F);
        return this;
    }

    public BlurRenderState build(GuiGraphicsExtractor graphics) {
        float maxRadius = Math.min(Math.min(width, height) * 0.5F, MAX_RADIUS);
        int topLeft = packRadius(radiusTopLeft, maxRadius);
        int topRight = packRadius(radiusTopRight, maxRadius);
        int bottomRight = packRadius(radiusBottomRight, maxRadius);
        int bottomLeft = packRadius(radiusBottomLeft, maxRadius);
        int opacity = Math.round(alpha * OPACITY_STEPS);
        BlurKey key = new BlurKey(type, strength);
        return new BlurRenderState(
                key, DisplayBatcher.pose(graphics), x, y, width, height,
                topLeft | topRight << 6 | type.outputLevel(strength) << 12 | (opacity & 1) << 15,
                bottomLeft | bottomRight << 6 | (opacity >> 1) << 12,
                tintTopLeft, tintTopRight, tintBottomRight, tintBottomLeft,
                graphics.scissorStack.peek()
        );
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (width <= 0.0F || height <= 0.0F || alpha <= 0.0F) {
            return;
        }
        DisplayBatcher.submit(graphics, build(graphics));
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp(Math.round(Math.clamp(radius, 0.0F, max) * 2.0F), 0, 63);
    }
}
