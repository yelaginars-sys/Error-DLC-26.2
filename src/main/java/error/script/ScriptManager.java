package error.script;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * ScriptManager ported from exclusive.
 * Manages script files in Error/scripts directory.
 */
public class ScriptManager {
    private static ScriptManager instance;

    public static ScriptManager getInstance() {
        if (instance == null) {
            instance = new ScriptManager();
        }
        return instance;
    }

    @Getter
    @Setter
    private String scriptText = "-- Custom Script\nprint('Script loaded!')\n";

    @Getter
    @Setter
    private boolean enabled = true;

    @Getter
    @Setter
    private boolean modified = false;

    @Getter
    @Setter
    private String statusMessage = "Готов";

    @Getter
    @Setter
    private String currentFileName = "Main.lua";

    public File getScriptsDirectory() {
        File runDir = Minecraft.getInstance().gameDirectory;
        File dir = new File(runDir, "Error/scripts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public List<String> getScriptFiles() {
        File dir = getScriptsDirectory();
        List<String> list = new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".lua") || name.endsWith(".js") || name.endsWith(".txt"));
        if (files != null) {
            for (File f : files) {
                list.add(f.getName());
            }
        }
        if (list.isEmpty()) {
            list.add("Main.lua");
        }
        return list;
    }

    public void loadScript(String fileName) {
        try {
            this.currentFileName = fileName;
            File dir = getScriptsDirectory();
            File file = new File(dir, fileName);
            if (file.exists()) {
                scriptText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                modified = false;
                statusMessage = "Загружен: " + fileName;
            } else {
                scriptText = "-- " + fileName + "\n";
                saveCurrentScript();
                statusMessage = "Создан: " + fileName;
            }
        } catch (Exception e) {
            statusMessage = "Ошибка загрузки: " + e.getMessage();
        }
    }

    public void saveCurrentScript() {
        try {
            File dir = getScriptsDirectory();
            File file = new File(dir, currentFileName);
            Files.writeString(file.toPath(), scriptText, StandardCharsets.UTF_8);
            modified = false;
            statusMessage = "Сохранен: " + currentFileName;
        } catch (Exception e) {
            statusMessage = "Ошибка сохранения: " + e.getMessage();
        }
    }

    public void deleteScript(String fileName) {
        try {
            File dir = getScriptsDirectory();
            File file = new File(dir, fileName);
            if (file.exists()) {
                file.delete();
            }
            List<String> remaining = getScriptFiles();
            if (!remaining.isEmpty()) {
                loadScript(remaining.get(0));
            } else {
                loadScript("Main.lua");
            }
            statusMessage = "Удален: " + fileName;
        } catch (Exception e) {
            statusMessage = "Ошибка удаления: " + e.getMessage();
        }
    }

    public void executeCurrent() {
        statusMessage = "Выполнен: " + currentFileName;
    }
}
