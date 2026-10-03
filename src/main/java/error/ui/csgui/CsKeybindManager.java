package error.ui.csgui;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Persistent keybind manager for CS ClickGUI. */
public final class CsKeybindManager {
    private static final CsKeybindManager INSTANCE = new CsKeybindManager();
    private final Map<String, Integer> bindings = new LinkedHashMap<>();
    private final Map<Integer, Boolean> keyWasDown = new HashMap<>();
    private Path file;
    private boolean loaded;

    public static CsKeybindManager get() {
        return INSTANCE;
    }

    private void load() {
        if (loaded) return;
        loaded = true;
        try {
            file = Minecraft.getInstance().gameDirectory.toPath().resolve("error-dlc").resolve("cs-binds.properties");
            if (!Files.isRegularFile(file)) return;
            Properties values = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                values.load(in);
                for (String id : values.stringPropertyNames()) {
                    try {
                        int key = Integer.parseInt(values.getProperty(id));
                        if (key > 0 && key <= GLFW.GLFW_KEY_LAST) bindings.put(id, key);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    public void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Properties values = new Properties();
            for (Map.Entry<String, Integer> e : bindings.entrySet()) {
                values.setProperty(e.getKey(), String.valueOf(e.getValue()));
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                values.store(out, "CS Binds Config");
            }
        } catch (Throwable ignored) {}
    }

    public int getBind(String id) {
        load();
        return bindings.getOrDefault(id, 0);
    }

    public void setBind(String id, int key) {
        load();
        if (key <= 0 || key > GLFW.GLFW_KEY_LAST) {
            bindings.remove(id);
        } else {
            bindings.put(id, key);
        }
        save();
    }
}
