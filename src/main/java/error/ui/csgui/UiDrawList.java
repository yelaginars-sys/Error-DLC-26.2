package error.ui.csgui;

import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

public final class UiDrawList {

    public UiDrawList rect(float x, float y, float w, float h, int color) {
        Render2D.drawRect(x, y, w, h, color);
        return this;
    }

    public UiDrawList roundedRect(float x, float y, float w, float h, float radius, int color) {
        Render2D.drawRoundedRect(x, y, w, h, radius, color);
        return this;
    }

    public UiDrawList gradientRoundedRect(float x, float y, float w, float h, float radius, int color1, int color2, boolean horizontal) {
        if (horizontal) {
            Render2D.drawGradientRound(x, y, w, h, radius, color1, color2, color2, color1);
        } else {
            Render2D.drawGradientRound(x, y, w, h, radius, color1, color1, color2, color2);
        }
        return this;
    }

    public UiDrawList roundedOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
        Render2D.drawRoundedOutline(x, y, w, h, radius, thickness, color);
        return this;
    }

    public UiDrawList circle(float cx, float cy, float radius, int color) {
        Render2D.drawCircle(cx, cy, radius, color);
        return this;
    }

    public UiDrawList shadow(float x, float y, float w, float h, float radius, float blur, int color) {
        Render2D.drawShadow(x, y, w, h, radius, blur, color);
        return this;
    }

    public UiDrawList blur(float x, float y, float w, float h, float radius, float blur, int color, float alpha) {
        Render2D.drawBlur(x, y, w, h, radius, blur, color, alpha);
        return this;
    }

    public UiDrawList text(float x, float y, float size, int color, String text) {
        if (text != null && !text.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, text, x, y - (size * 0.45F), size, color);
        }
        return this;
    }

    public UiDrawList strongText(float x, float y, float size, int color, String text) {
        if (text != null && !text.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, text, x, y - (size * 0.45F), size, color);
        }
        return this;
    }

    public UiDrawList rightText(float x, float y, float size, int color, String text, boolean shadow) {
        if (text != null && !text.isEmpty()) {
            float width = Fonts.SF_MEDIUM.getWidth(text, size);
            Fonts.drawString(Fonts.SF_MEDIUM, text, x - width, y - (size * 0.45F), size, color);
        }
        return this;
    }

    public UiDrawList icon(float x, float y, float size, int color, int glyphCode) {
        String glyph = String.valueOf((char) glyphCode);
        Fonts.drawString(Fonts.ICONS, glyph, x - (size * 0.5F), y - (size * 0.5F), size, color);
        return this;
    }
}
