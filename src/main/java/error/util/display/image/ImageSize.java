package error.util.display.image;

public record ImageSize(int width, int height) {
    public float aspect() {
        return height > 0 ? (float) width / height : 1.0F;
    }
}
