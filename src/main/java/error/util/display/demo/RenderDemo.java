package error.util.display.demo;



import error.event.EventTarget;
import error.event.list.EventDisplay;
import error.util.display.color.Color;
import error.util.display.color.Gradient;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.head.PlayerHead;
import error.util.display.image.Image;
import error.util.display.outline.Outline;
import error.util.display.rounded.RoundedRect;
import error.util.display.shadow.Shadow;
import error.util.display.text.Text;
import error.util.display.text.TextAlign;
import error.util.display.text.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class RenderDemo {
    private static final float ORIGIN_X = 10.0F;
    private static final float ORIGIN_Y = 10.0F;
    private static final float WIDTH = 52.0F;
    private static final float HEIGHT = 34.0F;
    private static final float GAP = 6.0F;
    private static final int STRENGTHS = BlurType.MAX_STRENGTH;
    private static final Identifier SAMPLE = Identifier.fromNamespaceAndPath("example", "textures/arrow_gps.png");
    private static final Color CORAL = Color.rgb(255, 94, 98);
    private static final Color PEACH = Color.rgb(255, 153, 102);
    private static final Color VIOLET = Color.rgb(106, 17, 203);
    private static final Color AZURE = Color.rgb(37, 117, 252);
    private static final Color SAND = Color.rgb(255, 209, 102);
    private static final Color MINT = Color.rgb(6, 214, 160);
    private static final Color OCEAN = Color.rgb(17, 138, 178);
    private static final Color RED = Color.rgb(255, 0, 0);
    private static final Color FADE_WHITE = Color.rgba(255, 255, 255, 32);
    private static final Color FROST = Color.rgba(255, 255, 255, 128);
    private static final Color VEIL = Color.rgba(0, 0, 0, 46);
    private static final Color GLASS_DARK = Color.rgba(0, 0, 0, 64);
    private static final Color GLASS_DEEP = Color.rgba(0, 0, 0, 80);
    private static final Color SMOKE_DARK = Color.rgba(0, 0, 0, 160);
    private static final Color SMOKE_LIGHT = Color.rgba(255, 255, 255, 64);
    private static final Color CORAL_GLASS = Color.rgba(255, 94, 96, 128);
    private static final Color AZURE_GLASS = Color.rgba(37, 117, 252, 128);
    private static final Color SHADOW_DEEP = Color.rgba(0, 0, 0, 192);
    private static final Color CARD = Color.rgb(40, 40, 48);
    private static final Color CARD_DARK = Color.rgb(30, 30, 36);
    private static final Color LIME = Color.hsb(0.35F, 0.7F, 0.9F);
    private static final Color HUE_0 = Color.hsb(0.00F, 0.8F, 1.0F);
    private static final Color HUE_1 = Color.hsb(0.25F, 0.8F, 1.0F);
    private static final Color HUE_2 = Color.hsb(0.50F, 0.8F, 1.0F);
    private static final Color HUE_3 = Color.hsb(0.75F, 0.8F, 1.0F);
    private static final Gradient AURORA = Gradient.of(CORAL, SAND, MINT, OCEAN).mirror();

    @EventTarget
    public void onDisplay(EventDisplay event) {
        render(event.graphics());
    }

    public static void render(GuiGraphicsExtractor graphics) {
        roundedRects(graphics, 0);
        outlines(graphics, 1);
        blurRow(graphics, 2, BlurType.KAWASE);
        blurRow(graphics, 3, BlurType.GAUSSIAN);
        blurRow(graphics, 4, BlurType.BOX);
        blurStyles(graphics, 5);
        texts(graphics, 6);
        shadows(graphics, 7);
        heads(graphics, 8);
        images(graphics, 9);
    }

    private static void roundedRects(GuiGraphicsExtractor graphics, int row) {
        RoundedRect.of(x(0), y(row), WIDTH, HEIGHT)
                .radius(8)
                .color(CORAL)
                .render(graphics);
        RoundedRect.of(x(1), y(row), WIDTH, HEIGHT)
                .radius(8)
                .horizontalGradient(CORAL, PEACH)
                .render(graphics);
        RoundedRect.of(x(2), y(row), WIDTH, HEIGHT)
                .radius(8)
                .verticalGradient(VIOLET, AZURE)
                .render(graphics);
        RoundedRect.of(x(3), y(row), WIDTH, HEIGHT)
                .radius(16, 0, 16, 0)
                .corners(HUE_0, HUE_1, HUE_2, HUE_3)
                .render(graphics);
        RoundedRect.of(x(4), y(row), WIDTH, HEIGHT)
                .radius(HEIGHT / 2.0F)
                .color(LIME)
                .alpha(0.5F)
                .render(graphics);
        RoundedRect.of(x(5), y(row), WIDTH, HEIGHT)
                .radiusTop(12)
                .radiusBottom(2)
                .color(CARD)
                .render(graphics);
    }

    private static void outlines(GuiGraphicsExtractor graphics, int row) {
        Outline.of(x(0), y(row), WIDTH, HEIGHT)
                .radius(8)
                .thickness(1.0F)
                .color(Color.WHITE)
                .render(graphics);
        Outline.of(x(1), y(row), WIDTH, HEIGHT)
                .radius(8)
                .thickness(3.0F)
                .color(PEACH)
                .render(graphics);
        Outline.of(x(2), y(row), WIDTH, HEIGHT)
                .radius(8)
                .thickness(2.0F)
                .corners(CORAL, SAND, MINT, OCEAN)
                .render(graphics);
        Outline.of(x(3), y(row), WIDTH, HEIGHT)
                .radius(16, 0, 16, 0)
                .thickness(2.0F)
                .horizontalGradient(VIOLET, AZURE)
                .render(graphics);
        Outline.of(x(4), y(row), WIDTH, HEIGHT)
                .radius(HEIGHT / 2.0F)
                .thickness(1.5F)
                .color(Color.WHITE)
                .alpha(0.5F)
                .render(graphics);
        RoundedRect.of(x(5), y(row), WIDTH, HEIGHT)
                .radius(8)
                .color(CARD_DARK)
                .render(graphics);
        Outline.of(x(5), y(row), WIDTH, HEIGHT)
                .radius(8)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .render(graphics);
    }

    private static void blurRow(GuiGraphicsExtractor graphics, int row, BlurType type) {
        for (int strength = 1; strength <= STRENGTHS; strength++) {
            Blur.of(x(strength - 1), y(row), WIDTH, HEIGHT)
                    .radius(8)
                    .type(type)
                    .strength(strength)
                    .tint(VEIL)
                    .render(graphics);
        }
    }

    private static void blurStyles(GuiGraphicsExtractor graphics, int row) {
        Blur.of(x(0), y(row), WIDTH, HEIGHT)
                .radius(8)
                .strength(3)
                .render(graphics);
        Blur.of(x(1), y(row), WIDTH, HEIGHT)
                .radius(8)
                .strength(3)
                .tint(SMOKE_DARK)
                .render(graphics);
        Blur.of(x(2), y(row), WIDTH, HEIGHT)
                .radius(8)
                .strength(4)
                .tint(SMOKE_LIGHT)
                .render(graphics);
        Blur.of(x(3), y(row), WIDTH, HEIGHT)
                .radius(8)
                .strength(3)
                .horizontalGradient(CORAL_GLASS, AZURE_GLASS)
                .render(graphics);
        Blur.of(x(4), y(row), WIDTH, HEIGHT)
                .radius(8)
                .strength(5)
                .tint(GLASS_DARK)
                .alpha(0.5F)
                .render(graphics);
        Blur.of(x(5), y(row), WIDTH, HEIGHT)
                .radius(16, 0, 16, 0)
                .strength(3)
                .tint(GLASS_DEEP)
                .render(graphics);
        Outline.of(x(5), y(row), WIDTH, HEIGHT)
                .radius(16, 0, 16, 0)
                .thickness(1.0F)
                .color(FROST)
                .render(graphics);
    }

    private static void texts(GuiGraphicsExtractor graphics, int row) {
        float time = System.nanoTime() / 1.0E9F;
        float y = y(row);
        Text.of(Fonts.MEDIUM, "Plain", x(0), y, 12.0F)
                .render(graphics);
        Text.of(Fonts.MEDIUM, "Gradient", x(1), y, 12.0F)
                .horizontalGradient(CORAL, PEACH)
                .render(graphics);
        Text.of(Fonts.MEDIUM, "Multi", x(2), y, 12.0F)
                .gradient(AURORA.offset(time * 0.25F))
                .render(graphics);
        Text.of(Fonts.MEDIUM, "Vertical", x(3), y, 12.0F)
                .verticalGradient(VIOLET, AZURE)
                .render(graphics);
        Text.of(Fonts.MEDIUM, "Alpha", x(4), y, 12.0F)
                .alpha(0.5F + 0.5F * (float) Math.sin(time * 2.0F))
                .render(graphics);
        Text.of(Fonts.MEDIUM, "Corners", x(5), y, 12.0F)
                .corners(HUE_0, HUE_1, HUE_2, HUE_3)
                .render(graphics);
        Text.of(Fonts.MEDIUM, "Centered", x(0) + WIDTH / 2.0F, y + 16.0F, 10.0F)
                .align(TextAlign.CENTER)
                .render(graphics);
        Text.of(Fonts.MEDIUM, "Spaced", x(2), y + 16.0F, 10.0F)
                .letterSpacing(1.5F)
                .render(graphics);
    }

    private static void shadows(GuiGraphicsExtractor graphics, int row) {
        float y = y(row);
        Shadow.of(x(0), y, WIDTH, HEIGHT)
                .radius(8)
                .blur(10)
                .color(SHADOW_DEEP)
                .render(graphics);
        card(graphics, 0, row, 8, 8, 8, 8);
        Shadow.of(x(1), y, WIDTH, HEIGHT)
                .radius(8)
                .blur(12)
                .color(CORAL)
                .render(graphics);
        card(graphics, 1, row, 8, 8, 8, 8);
        Shadow.of(x(2), y, WIDTH, HEIGHT)
                .radius(8)
                .blur(12)
                .horizontalGradient(CORAL, AZURE)
                .render(graphics);
        card(graphics, 2, row, 8, 8, 8, 8);
        Shadow.of(x(3), y, WIDTH, HEIGHT)
                .radius(8)
                .blur(6)
                .offset(0, 3)
                .strength(3.0F)
                .color(MINT)
                .render(graphics);
        card(graphics, 3, row, 8, 8, 8, 8);
        Shadow.of(x(4), y, WIDTH, HEIGHT)
                .radius(8)
                .blur(4)
                .spread(4)
                .color(Color.WHITE)
                .alpha(0.6F)
                .render(graphics);
        card(graphics, 4, row, 8, 8, 8, 8);
        Shadow.of(x(5), y, WIDTH, HEIGHT)
                .radius(16, 0, 16, 0)
                .blur(8)
                .offset(5, 5)
                .corners(CORAL, SAND, MINT, OCEAN)
                .render(graphics);
        card(graphics, 5, row, 16, 0, 16, 0);
    }

    private static void card(GuiGraphicsExtractor graphics, int column, int row, float topLeft, float topRight, float bottomRight, float bottomLeft) {
        RoundedRect.of(x(column), y(row), WIDTH, HEIGHT)
                .radius(topLeft, topRight, bottomRight, bottomLeft)
                .color(CARD)
                .render(graphics);
    }

    private static void heads(GuiGraphicsExtractor graphics, int row) {
        float y = y(row);
        float size = HEIGHT;
        PlayerHead.of(x(0), y, size).render(graphics);
        PlayerHead.of(x(1), y, size).radius(8).render(graphics);
        PlayerHead.of(x(2), y, size).circle().render(graphics);
        PlayerHead.of(x(3), y, size).radius(8).color(CORAL).render(graphics);
        PlayerHead.of(x(4), y, size).radius(8).horizontalGradient(CORAL, AZURE).render(graphics);
        PlayerHead.of(x(5), y, size).radius(8).alpha(0.5F).render(graphics);
        PlayerHead.of(x(6), y, size).radius(8).hat(false).render(graphics);
        PlayerHead.of(x(7), y, size).radius(8).mix(RED, 0.4F).render(graphics);
        PlayerHead.of(x(8), y, size).radius(16, 0, 16, 0).smooth(true).render(graphics);
    }

    private static void images(GuiGraphicsExtractor graphics, int row) {
        float y = y(row);
        float size = HEIGHT;
        Image.of(SAMPLE, x(0), y, size, size).render(graphics);
        Image.of(SAMPLE, x(1), y, size, size).radius(8).render(graphics);
        Image.of(SAMPLE, x(2), y, size, size).circle().render(graphics);
        Image.of(SAMPLE, x(3), y, size, size).radius(8).color(CORAL).render(graphics);
        Image.of(SAMPLE, x(4), y, size, size).radius(8).horizontalGradient(CORAL, AZURE).render(graphics);
        Image.of(SAMPLE, x(5), y, size, size).radius(8).alpha(0.5F).render(graphics);
        Image.of(SAMPLE, x(6), y, size, size).radius(8).flipX(true).render(graphics);
        Image.of(SAMPLE, x(7), y, WIDTH, HEIGHT).radius(8).contain().render(graphics);
    }

    private static float x(int column) {
        return ORIGIN_X + column * (WIDTH + GAP);
    }

    private static float y(int row) {
        return ORIGIN_Y + row * (HEIGHT + GAP);
    }
}
