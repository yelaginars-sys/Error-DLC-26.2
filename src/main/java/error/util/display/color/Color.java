package error.util.display.color;

import java.util.Locale;

public record Color(int argb) {
    public static final Color WHITE = new Color(0xFFFFFFFF);
    public static final Color BLACK = new Color(0xFF000000);
    public static final Color TRANSPARENT = new Color(0);

    public static Color of(int argb) {
        return new Color(argb);
    }

    public static Color rgb(int red, int green, int blue) {
        return rgba(red, green, blue, 255);
    }

    public static Color rgba(int red, int green, int blue, int alpha) {
        return new Color(channel(alpha) << 24 | channel(red) << 16 | channel(green) << 8 | channel(blue));
    }

    public static Color rgba(int red, int green, int blue, float alpha) {
        return rgba(red, green, blue, Math.round(alpha * 255.0F));
    }

    public static Color rgbf(float red, float green, float blue, float alpha) {
        return rgba(Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), Math.round(alpha * 255.0F));
    }

    public static Color hex(String value) {
        String digits = value.strip().replaceFirst("^(#|0[xX])", "");
        try {
            return switch (digits.length()) {
                case 3, 4 -> hex(expandShort(digits));
                case 6 -> rgb(parse(digits, 0), parse(digits, 2), parse(digits, 4));
                case 8 -> rgba(parse(digits, 0), parse(digits, 2), parse(digits, 4), parse(digits, 6));
                default -> throw new IllegalArgumentException("Unsupported hex color: " + value);
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid hex color: " + value, exception);
        }
    }

    public static Color hsb(float hue, float saturation, float brightness) {
        return hsba(hue, saturation, brightness, 1.0F);
    }

    public static Color hsba(float hue, float saturation, float brightness, float alpha) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        float s = clamp01(saturation);
        float b = clamp01(brightness);
        float f = h - (float) Math.floor(h);
        float p = b * (1.0F - s);
        float q = b * (1.0F - s * f);
        float t = b * (1.0F - s * (1.0F - f));
        return switch ((int) h) {
            case 0 -> rgbf(b, t, p, alpha);
            case 1 -> rgbf(q, b, p, alpha);
            case 2 -> rgbf(p, b, t, alpha);
            case 3 -> rgbf(p, q, b, alpha);
            case 4 -> rgbf(t, p, b, alpha);
            default -> rgbf(b, p, q, alpha);
        };
    }

    public int red() {
        return argb >> 16 & 0xFF;
    }

    public int green() {
        return argb >> 8 & 0xFF;
    }

    public int blue() {
        return argb & 0xFF;
    }

    public int alpha() {
        return argb >>> 24;
    }

    public Color withAlpha(int alpha) {
        return new Color(channel(alpha) << 24 | argb & 0xFFFFFF);
    }

    public Color withAlpha(float alpha) {
        return withAlpha(Math.round(clamp01(alpha) * 255.0F));
    }

    public Color multiplyAlpha(float factor) {
        return withAlpha(Math.round(alpha() * clamp01(factor)));
    }

    public Color lerp(Color other, float progress) {
        float t = clamp01(progress);
        return rgba(mix(red(), other.red(), t), mix(green(), other.green(), t), mix(blue(), other.blue(), t), mix(alpha(), other.alpha(), t));
    }

    public Color brighter(float amount) {
        return lerp(rgba(255, 255, 255, alpha()), amount);
    }

    public Color darker(float amount) {
        return lerp(rgba(0, 0, 0, alpha()), amount);
    }

    public String toHex() {
        return String.format(Locale.ROOT, "#%02X%02X%02X%02X", red(), green(), blue(), alpha());
    }

    private static String expandShort(String digits) {
        return digits.chars().collect(StringBuilder::new, (builder, c) -> builder.append((char) c).append((char) c), StringBuilder::append).toString();
    }

    private static int parse(String digits, int offset) {
        return Integer.parseInt(digits, offset, offset + 2, 16);
    }

    private static int mix(int from, int to, float progress) {
        return Math.round(from + (to - from) * progress);
    }

    private static int channel(int value) {
        return Math.clamp(value, 0, 255);
    }

    private static float clamp01(float value) {
        return Math.clamp(value, 0.0F, 1.0F);
    }
}
