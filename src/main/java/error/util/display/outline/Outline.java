package error.util.display.outline;

import error.util.display.color.Color;
import error.util.display.batch.DisplayBatcher;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Outline {
    private static final float MAX_RADIUS = 31.5F;
    private static final float THICKNESS_SCALE = 16.0F;

    private float x;
    private float y;
    private float width;
    private float height;
    private float thickness = 1.0F;
    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;
    private int colorTopLeft = -1;
    private int colorTopRight = -1;
    private int colorBottomRight = -1;
    private int colorBottomLeft = -1;
    private float alpha = 1.0F;

    private Outline() {
    }

    public static Outline create() {
        return new Outline();
    }

    public static Outline of(float x, float y, float width, float height) {
        return new Outline().bounds(x, y, width, height);
    }

    public Outline position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Outline size(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public Outline bounds(float x, float y, float width, float height) {
        return position(x, y).size(width, height);
    }

    public Outline thickness(float thickness) {
        this.thickness = Math.clamp(thickness, 0.0F, 255.0F / THICKNESS_SCALE);
        return this;
    }

    public Outline radius(float radius) {
        return radius(radius, radius, radius, radius);
    }

    public Outline radius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = topLeft;
        this.radiusTopRight = topRight;
        this.radiusBottomRight = bottomRight;
        this.radiusBottomLeft = bottomLeft;
        return this;
    }

    public Outline radiusTop(float radius) {
        this.radiusTopLeft = radius;
        this.radiusTopRight = radius;
        return this;
    }

    public Outline radiusBottom(float radius) {
        this.radiusBottomLeft = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Outline radiusLeft(float radius) {
        this.radiusTopLeft = radius;
        this.radiusBottomLeft = radius;
        return this;
    }

    public Outline radiusRight(float radius) {
        this.radiusTopRight = radius;
        this.radiusBottomRight = radius;
        return this;
    }

    public Outline color(int argb) {
        return corners(argb, argb, argb, argb);
    }

    public Outline color(Color color) {
        return color(color.argb());
    }

    public Outline verticalGradient(int top, int bottom) {
        return corners(top, top, bottom, bottom);
    }

    public Outline verticalGradient(Color top, Color bottom) {
        return verticalGradient(top.argb(), bottom.argb());
    }

    public Outline horizontalGradient(int left, int right) {
        return corners(left, right, right, left);
    }

    public Outline horizontalGradient(Color left, Color right) {
        return horizontalGradient(left.argb(), right.argb());
    }

    public Outline corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.colorTopLeft = topLeft;
        this.colorTopRight = topRight;
        this.colorBottomRight = bottomRight;
        this.colorBottomLeft = bottomLeft;
        return this;
    }

    public Outline corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public Outline alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0F, 1.0F);
        return this;
    }

    public OutlineRenderState build(GuiGraphicsExtractor graphics) {
        float maxRadius = Math.min(Math.min(width, height) * 0.5F, MAX_RADIUS);
        int topLeft = packRadius(radiusTopLeft, maxRadius);
        int topRight = packRadius(radiusTopRight, maxRadius);
        int bottomRight = packRadius(radiusBottomRight, maxRadius);
        int bottomLeft = packRadius(radiusBottomLeft, maxRadius);
        int packedThickness = Math.clamp(Math.round(thickness * THICKNESS_SCALE), 0, 255);
        return new OutlineRenderState(
                DisplayBatcher.pose(graphics), x, y, width, height,
                topLeft | topRight << 6 | (packedThickness & 0xF) << 12,
                bottomLeft | bottomRight << 6 | (packedThickness >> 4) << 12,
                applyAlpha(colorTopLeft), applyAlpha(colorTopRight), applyAlpha(colorBottomRight), applyAlpha(colorBottomLeft),
                graphics.scissorStack.peek()
        );
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (width <= 0.0F || height <= 0.0F || thickness <= 0.0F || alpha <= 0.0F) {
            return;
        }
        DisplayBatcher.submit(graphics, build(graphics));
    }

    private int applyAlpha(int color) {
        int a = Math.round((color >>> 24) * alpha);
        return a << 24 | color & 0xFFFFFF;
    }

    private static int packRadius(float radius, float max) {
        return Math.clamp(Math.round(Math.clamp(radius, 0.0F, max) * 2.0F), 0, 63);
    }
}
