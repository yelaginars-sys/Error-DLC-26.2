package error.util.display.shadow;

import error.util.display.color.Color;
import error.util.display.batch.DisplayBatcher;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Shadow {
    private static final float MAX_RADIUS = 31.0F;
    private static final float MAX_STRENGTH = 4.0F;
    private static final float STRENGTH_STEP = 0.2F;

    private float x;
    private float y;
    private float width;
    private float height;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private float blur = 8.0F;
    private float spread;
    private float offsetX;
    private float offsetY;
    private float strength = 1.0F;
    private int colorTopLeft = 0xFF000000;
    private int colorTopRight = 0xFF000000;
    private int colorBottomRight = 0xFF000000;
    private int colorBottomLeft = 0xFF000000;
    private float alpha = 1.0F;

    private Shadow() {
    }

    public static Shadow create() {
        return new Shadow();
    }

    public static Shadow of(float x, float y, float width, float height) {
        return new Shadow().bounds(x, y, width, height);
    }

    public Shadow position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Shadow size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Shadow bounds(float x, float y, float width, float height) {
        return position(x, y).size(width, height);
    }

    public Shadow radius(float radius) {
        return radius(radius, radius, radius, radius);
    }

    public Shadow radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public Shadow radiusTop(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public Shadow radiusBottom(float radius) {
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Shadow radiusLeft(float radius) {
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public Shadow radiusRight(float radius) {
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Shadow blur(float blur) {
        this.blur = Math.clamp(blur, 0.0F, 63.5F);
        return this;
    }

    public Shadow spread(float spread) {
        this.spread = spread;
        return this;
    }

    public Shadow offset(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }

    public Shadow strength(float strength) {
        this.strength = Math.clamp(strength, 0.0F, MAX_STRENGTH);
        return this;
    }

    public Shadow color(int argb) {
        return corners(argb, argb, argb, argb);
    }

    public Shadow verticalGradient(int top, int bottom) {
        return corners(top, top, bottom, bottom);
    }

    public Shadow horizontalGradient(int left, int right) {
        return corners(left, right, right, left);
    }

    public Shadow corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.colorTopLeft = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public Shadow color(Color color) {
        return color(color.argb());
    }

    public Shadow verticalGradient(Color top, Color bottom) {
        return verticalGradient(top.argb(), bottom.argb());
    }

    public Shadow horizontalGradient(Color left, Color right) {
        return horizontalGradient(left.argb(), right.argb());
    }

    public Shadow corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public Shadow alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0F, 1.0F);
        return this;
    }

    public ShadowRenderState build(GuiGraphicsExtractor graphics) {
        int blurBits = Math.clamp(Math.round(blur * 2.0F), 0, 127);
        int strengthBits = Math.clamp(Math.round((Math.max(strength, 1.0F) - 1.0F) / STRENGTH_STEP), 0, 15);
        float shapeWidth = width + spread * 2.0F;
        float shapeHeight = height + spread * 2.0F;
        float maxRadius = Math.min(Math.min(shapeWidth, shapeHeight) * 0.5F, MAX_RADIUS);
        int topLeft = packRadius(radiusTopLeft + spread, maxRadius);
        int topRight = packRadius(radiusTopRight + spread, maxRadius);
        int bottomRight = packRadius(radiusBottomRight + spread, maxRadius);
        int bottomLeft = packRadius(radiusBottomLeft + spread, maxRadius);
        float factor = alpha * Math.min(strength, 1.0F);
        return new ShadowRenderState(
                DisplayBatcher.pose(graphics), x + offsetX - spread, y + offsetY - spread, shapeWidth, shapeHeight, blurBits * 0.5F,
                topLeft | topRight << 5 | bottomRight << 10, bottomLeft | blurBits << 5 | strengthBits << 12,
                applyAlpha(colorTopLeft, factor), applyAlpha(colorTopRight, factor), applyAlpha(colorBottomRight, factor), applyAlpha(colorBottomLeft, factor),
                graphics.scissorStack.peek()
        );
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (width + spread * 2.0F <= 0.0F || height + spread * 2.0F <= 0.0F || alpha <= 0.0F || strength <= 0.0F) {
            return;
        }
        DisplayBatcher.submit(graphics, build(graphics));
    }

    private static int applyAlpha(int color, float factor) {
        int a = Math.round((color >>> 24) * factor);
        return a << 24 | color & 0xFFFFFF;
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp(Math.round(Math.clamp(radius, 0.0F, max)), 0, 31);
    }
}
