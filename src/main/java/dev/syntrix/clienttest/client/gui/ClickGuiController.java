package dev.syntrix.clienttest.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dev.syntrix.clienttest.client.combat.CrystalAuraSettings;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ClickGuiController {
    public static final ClickGuiState STATE = new ClickGuiState();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("noctra-crystalaura/settings.json");
    private static boolean readable = true;

    public static void initialize() {
        load();
    }

    public static ClickGuiState.Module module() {
        return STATE.modules.getFirst();
    }

    public static void setEnabled(boolean enabled) {
        module().enabled = enabled;
    }

    private static void load() {
        try {
            if (!Files.exists(FILE)) return;
            var json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            var values = new java.util.LinkedHashMap<>(module().values);
            for (var setting : CrystalAuraSettings.DEFINITION.settings()) {
                if (!json.has(setting.id())) continue;
                double value = json.get(setting.id()).getAsDouble();
                if (!setting.valid(value)) throw new IllegalArgumentException("Invalid setting: " + setting.id());
                values.put(setting.id(), value);
            }
            module().values.clear();
            module().values.putAll(values);
        } catch (Exception error) {
            readable = false;
            org.slf4j.LoggerFactory.getLogger("crystalaura").warn("Cannot read settings.json; preserving the file", error);
        }
    }

    public static boolean save() {
        if (!readable) return false;
        Path temporary = null;
        try {
            Files.createDirectories(FILE.getParent());
            temporary = Files.createTempFile(FILE.getParent(), ".settings-", ".tmp");
            Files.writeString(temporary, new Gson().toJson(module().values));
            Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger("crystalaura").warn("Could not save settings.json", error);
            return false;
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) {}
        }
    }

    private ClickGuiController() {}
}
