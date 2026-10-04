package error.util.display.image;

import net.minecraft.resources.Identifier;

public final class ImageAsset {
    private final String name;
    private final Identifier texture;

    ImageAsset(String name, Identifier texture) {
        this.name = name;
        this.texture = texture;
    }

    public String name() {
        return name;
    }

    public Identifier texture() {
        return texture;
    }

    public ImageSize size() {
        return Images.size(texture);
    }

    public int width() {
        return size().width();
    }

    public int height() {
        return size().height();
    }

    public float aspect() {
        return size().aspect();
    }

    public Image image() {
        return Image.of(this);
    }
}
