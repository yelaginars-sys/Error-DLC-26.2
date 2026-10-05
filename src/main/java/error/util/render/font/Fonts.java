package error.util.render.font;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import error.util.RenderExtend;
import error.util.render.Render2DUtil;
import error.util.render.menu.TextRenderState;

/**
 */
public final class Fonts {
    public static final MsdfFont SF_MEDIUM = MsdfFont.load(Identifier.parse("error:fonts/sfmedium.json"));
    public static final MsdfFont ICONS = MsdfFont.load(Identifier.parse("error:fonts/icons.json"));
    public static final MsdfFont EMOJIS = MsdfFont.load(Identifier.parse("error:fonts/emojis.json"));
    public static final MsdfFont ENERGY = MsdfFont.load(Identifier.parse("error:fonts/energy.json"));
    public static final MsdfFont ICONS_NURIK = MsdfFont.load(Identifier.parse("error:fonts/icons_nurik.json"));
    public static final MsdfFont NURIK_MENU = MsdfFont.load(Identifier.parse("error:fonts/nurik_menu.json"));

    public static final String NURIK_COMBAT = "\uEA07";
    public static final String NURIK_MOVEMENT = "\uEA0F";
    public static final String NURIK_VISUALS = "\uEA1D";
    public static final String NURIK_PLAYER = "\uEA12";
    public static final String NURIK_MISC = "\uEA0E";
    public static final String NURIK_PRESETS = "\uEA14";
    public static final String NURIK_AUTOBUY = "\uEA03";
    public static final String NURIK_ACCOUNTS = "\uEA01";
    public static final String NURIK_SCRIPTS = "\uEA11";
    public static final String NURIK_GEAR = "\uEA06";
    public static final String NURIK_ANGLES = "\uEA02";
    public static final String NURIK_BIND = "\uEA04";
    public static final String NURIK_DOTS = "\uEA0A";
    public static final String NURIK_SEARCH = "\uEA17";
    public static final String NURIK_XMARK = "\uEA1E";
    public static final String NURIK_CHECK = "\uEA05";
    public static final String NURIK_LOGO = "\uEA10";

    public static void drawString(MsdfFont font, String text, float x, float y, float size, int color) {
        draw(font, text, x, y, size, color, TextAlign.LEFT);
    }

    public static void drawCenteredString(MsdfFont font, String text, float x, float y, float size, int color) {
        draw(font, text, x, y, size, color, TextAlign.CENTER);
    }

    public static void drawIcon(IconUse icon, float x, float y, float size, int color) {
        if (icon == null || icon.glyph == null) return;
        draw(ICONS_NURIK, icon.glyph, x, y, size, color, TextAlign.LEFT);
    }

    public static void drawCenteredIcon(IconUse icon, float x, float y, float size, int color) {
        if (icon == null || icon.glyph == null) return;
        draw(ICONS_NURIK, icon.glyph, x, y, size, color, TextAlign.CENTER);
    }

    public static float getIconWidth(IconUse icon, float size) {
        if (icon == null || icon.glyph == null) return 0.0F;
        return ICONS_NURIK.getWidth(icon.glyph, size);
    }

    private static void draw(MsdfFont font, String text, float x, float y, float size, int color, TextAlign align) {
        if (Render2DUtil.hasEmptyScissor()) return;
        if (font == null || text == null || text.isEmpty()) return;
        GuiGraphicsExtractor extractor = RenderExtend.currentGuiGraphicsExtractor();
        if (extractor == null) return;

        Render2DUtil.queue(new TextRenderState(
                extractor.pose(),
                font,
                x, y, size,
                font.shape(text),
                color,
                align,
                0.0F,
                Render2DUtil.currentScissor()
        ));
    }

    private Fonts() {}
}