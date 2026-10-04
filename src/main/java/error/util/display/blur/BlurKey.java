package error.util.display.blur;

public record BlurKey(BlurType type, int strength) {
    public BlurKey {
        strength = Math.clamp(strength, 1, BlurType.MAX_STRENGTH);
    }
}
