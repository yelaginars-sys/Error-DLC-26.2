package error.util.display.blur;

public enum BlurType {
    KAWASE,
    GAUSSIAN,
    BOX;

    public static final int MAX_STRENGTH = 6;

    public int outputLevel(int strength) {
        return this == KAWASE ? 1 : (strength + 1) / 2;
    }

    public int downDepth(int strength) {
        return this == KAWASE ? strength : outputLevel(strength);
    }
}
