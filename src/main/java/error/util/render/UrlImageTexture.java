package error.util.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronously downloads head textures from Minotar / Helm API and registers DynamicTextures.
 */
public final class UrlImageTexture {

    private static final Map<String, UrlImageTexture> CACHE = new ConcurrentHashMap<>();

    private final String url;
    private final Identifier id;
    private volatile boolean loading = false;
    private volatile boolean loaded = false;
    private volatile boolean failed = false;

    private UrlImageTexture(String url) {
        this.url = url;
        this.id = Identifier.fromNamespaceAndPath("error", "url_head_" + Math.abs(url.hashCode()));
    }

    public static UrlImageTexture get(String url) {
        return CACHE.computeIfAbsent(url, UrlImageTexture::new);
    }

    public static Identifier getIdentifier(String url) {
        UrlImageTexture texture = get(url);
        texture.loadAsync();
        return texture.isLoaded() ? texture.id : null;
    }

    public static boolean isLoaded(String url) {
        UrlImageTexture texture = CACHE.get(url);
        return texture != null && texture.isLoaded();
    }

    public boolean isLoaded() {
        return loaded;
    }

    public Identifier getId() {
        return id;
    }

    public void loadAsync() {
        if (loaded || loading || failed) return;
        loading = true;

        Thread thread = new Thread(() -> {
            try {
                URL target = URI.create(url).toURL();
                HttpURLConnection connection = (HttpURLConnection) target.openConnection();
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(6000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

                try (InputStream stream = connection.getInputStream()) {
                    BufferedImage image = ImageIO.read(stream);
                    if (image == null) {
                        failed = true;
                        return;
                    }

                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", baos);
                    byte[] pngBytes = baos.toByteArray();

                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null) {
                        mc.execute(() -> uploadPng(pngBytes));
                    }
                } finally {
                    connection.disconnect();
                }
            } catch (Throwable t) {
                failed = true;
            } finally {
                loading = false;
            }
        }, "url-head-loader-" + id.getPath());
        thread.setDaemon(true);
        thread.start();
    }

    private void uploadPng(byte[] pngBytes) {
        if (loaded) return;

        try {
            NativeImage nativeImage = NativeImage.read(new java.io.ByteArrayInputStream(pngBytes));
            DynamicTexture texture = new DynamicTexture(() -> id.toString(), nativeImage);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            this.loaded = true;
        } catch (Throwable t) {
            this.failed = true;
        }
    }
}
