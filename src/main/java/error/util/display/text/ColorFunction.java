package error.util.display.text;

@FunctionalInterface
public interface ColorFunction {
    int color(int glyph, float x, float y);
}
