package error.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import error.Client;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.persiki.ChatUtil;
import error.util.client.clients.Theme;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    public static boolean isLoadingConfig = false;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File configDir;
    private String currentConfig = "default";

    public ConfigManager() {
        this.configDir = new File(error.IMinecraft.mc.gameDirectory, "error/configs");
        if (!this.configDir.exists()) {
            this.configDir.mkdirs();
        }
    }

    public File getConfigDir() {
        return configDir;
    }

    public String getCurrentConfig() {
        return currentConfig;
    }

    public File getConfigFile(String name) {
        String cleanName = name.endsWith(".json") ? name : name + ".json";
        return new File(configDir, cleanName);
    }

    public synchronized void autoSave() {
        if (isLoadingConfig) return;
        saveConfig(currentConfig, false);
    }

    public synchronized boolean saveConfig(String name, boolean notify) {
        if (name == null || name.trim().isEmpty()) name = "default";
        String cleanName = name.replace(".json", "").trim();

        try {
            JsonObject root = new JsonObject();

            JsonObject themeJson = new JsonObject();
            themeJson.addProperty("accentColor", Theme.getAccentColor());
            themeJson.addProperty("secondaryColor", Theme.getSecondaryColor());
            themeJson.addProperty("accentMode", Theme.getAccentMode());
            themeJson.addProperty("backgroundMode", Theme.getBackgroundMode());
            themeJson.addProperty("glassStyle", Theme.getGlassStyle());
            themeJson.addProperty("uiStyle", Theme.getUiStyle());
            themeJson.addProperty("bgColor1", Theme.getBgColor1());
            themeJson.addProperty("bgColor2", Theme.getBgColor2());
            themeJson.addProperty("panelAlpha", Theme.getPanelAlpha());
            root.add("theme", themeJson);

            JsonObject modulesJson = new JsonObject();
            if (Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
                for (Module module : Client.INSTANCE.moduleManager.getModules()) {
                    if (module == null) continue;
                    JsonObject moduleData = new JsonObject();
                    moduleData.addProperty("enabled", module.isEnabled());

                    if (module.getBind() != null) {
                        List<Integer> binds = module.getBind().getValue();
                        com.google.gson.JsonArray bindArr = new com.google.gson.JsonArray();
                        if (binds != null) {
                            for (int b : binds) bindArr.add(b);
                        }
                        moduleData.add("bind", bindArr);
                    }

                    JsonObject settingsJson = new JsonObject();
                    for (Setting<?> setting : module.getSettings()) {
                        if (setting == null) continue;

                        if (setting instanceof CheckBox cb) {
                            settingsJson.addProperty(setting.getName(), cb.getValue());
                        } else if (setting instanceof SliderSetting slider) {
                            settingsJson.addProperty(setting.getName(), slider.getValue());
                        } else if (setting instanceof ModeSetting mode) {
                            settingsJson.addProperty(setting.getName(), mode.getValue());
                        } else if (setting instanceof MultiModeSetting multi) {
                            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
                            for (String val : multi.getValue()) array.add(val);
                            settingsJson.add(setting.getName(), array);
                        } else if (setting instanceof ColorSetting color) {
                            settingsJson.addProperty(setting.getName(), color.getValue());
                        } else if (setting instanceof BindSetting bind) {
                            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
                            if (bind.getValue() != null) {
                                for (int val : bind.getValue()) array.add(val);
                            }
                            settingsJson.add(setting.getName(), array);
                        }
                    }
                    moduleData.add("settings", settingsJson);

                    modulesJson.add(module.getName(), moduleData);
                }
            }
            root.add("modules", modulesJson);

            JsonObject hudJson = new JsonObject();
            if (HudManager.getInstance() != null) {
                for (HudElement element : HudManager.getInstance().getElements()) {
                    if (element == null) continue;
                    JsonObject elementData = new JsonObject();
                    elementData.addProperty("x", element.getX());
                    elementData.addProperty("y", element.getY());
                    elementData.addProperty("enabled", element.isEnabled());

                    hudJson.add(element.getClass().getSimpleName(), elementData);
                }
            }
            root.add("hud", hudJson);

            File targetFile = getConfigFile(cleanName);
            java.nio.file.Files.writeString(targetFile.toPath(), gson.toJson(root), StandardCharsets.UTF_8);

            this.currentConfig = cleanName;

            if (notify) {
                ChatUtil.success("Конфигурация '" + cleanName + "' успешно сохранена!");
            }
            return true;
        } catch (Exception e) {
            if (notify) {
                ChatUtil.error("Не удалось сохранить конфигурацию: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        }
    }

    public synchronized boolean loadConfig(String name, boolean notify) {
        isLoadingConfig = true;
        try {
            if (name == null || name.trim().isEmpty()) name = "default";
            String cleanName = name.replace(".json", "").trim();

            File configFile = getConfigFile(cleanName);
            if (!configFile.exists()) {
                if (notify) {
                    ChatUtil.error("Конфигурация '" + cleanName + "' не найдена!");
                }
                return false;
            }

            String jsonString = java.nio.file.Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();

            if (root.has("theme")) {
                JsonObject themeJson = root.getAsJsonObject("theme");
                if (themeJson.has("accentColor")) Theme.setAccentColor(themeJson.get("accentColor").getAsInt());
                if (themeJson.has("secondaryColor")) Theme.setSecondaryColor(themeJson.get("secondaryColor").getAsInt());
                if (themeJson.has("accentMode")) Theme.setAccentMode(themeJson.get("accentMode").getAsString());
                if (themeJson.has("backgroundMode")) Theme.setBackgroundMode(themeJson.get("backgroundMode").getAsString());
                if (themeJson.has("glassStyle")) Theme.setGlassStyle(themeJson.get("glassStyle").getAsString());
                if (themeJson.has("uiStyle")) Theme.setUiStyle(themeJson.get("uiStyle").getAsString());
                if (themeJson.has("bgColor1")) Theme.setBgColor1(themeJson.get("bgColor1").getAsInt());
                if (themeJson.has("bgColor2")) Theme.setBgColor2(themeJson.get("bgColor2").getAsInt());
                if (themeJson.has("panelAlpha")) Theme.setPanelAlpha(themeJson.get("panelAlpha").getAsFloat());
            }

            if (root.has("modules") && Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
                JsonObject modulesJson = root.getAsJsonObject("modules");
                for (Module module : Client.INSTANCE.moduleManager.getModules()) {
                    if (module != null && modulesJson.has(module.getName())) {
                        JsonObject moduleData = modulesJson.getAsJsonObject(module.getName());

                        if (moduleData.has("enabled")) {
                            boolean enabled = moduleData.get("enabled").getAsBoolean();
                            if (module.isEnabled() != enabled) {
                                module.setEnabled(enabled);
                            }
                        }

                        if (moduleData.has("bind") && module.getBind() != null) {
                            JsonElement bindElem = moduleData.get("bind");
                            if (bindElem.isJsonArray()) {
                                List<Integer> binds = new ArrayList<>();
                                for (JsonElement be : bindElem.getAsJsonArray()) {
                                    binds.add(be.getAsInt());
                                }
                                if (module instanceof error.module.impl.render.ClickGui && binds.isEmpty()) {
                                    binds.add(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
                                }
                                module.getBind().setValue(binds);
                            }
                        }

                        if (moduleData.has("settings")) {
                            JsonObject settingsJson = moduleData.getAsJsonObject("settings");
                            for (Setting<?> setting : module.getSettings()) {
                                if (setting == null || !settingsJson.has(setting.getName())) continue;

                                JsonElement elem = settingsJson.get(setting.getName());
                                try {
                                    if (setting instanceof CheckBox cb) {
                                        cb.setValue(elem.getAsBoolean());
                                    } else if (setting instanceof SliderSetting slider) {
                                        slider.setValue(elem.getAsFloat());
                                    } else if (setting instanceof ModeSetting mode) {
                                        mode.setValue(elem.getAsString());
                                    } else if (setting instanceof MultiModeSetting multi) {
                                        List<String> list = new ArrayList<>();
                                        if (elem.isJsonArray()) {
                                            for (JsonElement me : elem.getAsJsonArray()) {
                                                list.add(me.getAsString());
                                            }
                                        }
                                        multi.setValue(list);
                                    } else if (setting instanceof ColorSetting color) {
                                        color.setValue(elem.getAsInt());
                                    } else if (setting instanceof BindSetting bind) {
                                        List<Integer> binds = new ArrayList<>();
                                        if (elem.isJsonArray()) {
                                            for (JsonElement me : elem.getAsJsonArray()) {
                                                binds.add(me.getAsInt());
                                            }
                                        }
                                        bind.setValue(binds);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }

            if (root.has("hud") && HudManager.getInstance() != null) {
                JsonObject hudJson = root.getAsJsonObject("hud");
                for (HudElement element : HudManager.getInstance().getElements()) {
                    if (element == null) continue;
                    String className = element.getClass().getSimpleName();
                    if (hudJson.has(className)) {
                        JsonObject elementData = hudJson.getAsJsonObject(className);
                        if (elementData.has("x")) element.setX(elementData.get("x").getAsFloat());
                        if (elementData.has("y")) element.setY(elementData.get("y").getAsFloat());
                        if (elementData.has("enabled")) element.setEnabled(elementData.get("enabled").getAsBoolean());
                    }
                }
            }

            this.currentConfig = cleanName;

            if (notify) {
                ChatUtil.success("Конфигурация '" + cleanName + "' успешно загружена!");
            }
            return true;
        } catch (Exception e) {
            if (notify) {
                ChatUtil.error("Ошибка при чтении конфигурации: " + e.getMessage());
            }
            e.printStackTrace();
            return false;
        } finally {
            isLoadingConfig = false;
        }
    }

    public synchronized boolean deleteConfig(String name) {
        if (name == null) return false;
        File file = getConfigFile(name);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public void openFolder() {
        try {
            if (configDir.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(configDir);
            }
        } catch (Exception ignored) {}
    }

    private static final String KEY_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    public static String generateRandomKey() {
        StringBuilder sb = new StringBuilder("ERR-");
        for (int i = 0; i < 16; i++) {
            sb.append(KEY_CHARS.charAt(RANDOM.nextInt(KEY_CHARS.length())));
        }
        return sb.toString();
    }

    public synchronized String getOrCreateShareKey(String configName) {
        if (configName == null || configName.trim().isEmpty()) configName = "default";
        String cleanName = configName.replace(".json", "").trim();

        File keysFile = new File(configDir, "shared_keys.json");
        JsonObject keysRoot = new JsonObject();
        if (keysFile.exists()) {
            try {
                String content = java.nio.file.Files.readString(keysFile.toPath(), StandardCharsets.UTF_8);
                JsonElement el = JsonParser.parseString(content);
                if (el != null && el.isJsonObject()) {
                    keysRoot = el.getAsJsonObject();
                }
            } catch (Exception ignored) {}
        }

        JsonObject configsMap = keysRoot.has("configs") ? keysRoot.getAsJsonObject("configs") : new JsonObject();
        JsonObject keysMap = keysRoot.has("keys") ? keysRoot.getAsJsonObject("keys") : new JsonObject();

        String existingKey = null;
        if (configsMap.has(cleanName)) {
            existingKey = configsMap.get(cleanName).getAsString();
        }

        File cfgFile = getConfigFile(cleanName);
        if (cfgFile.exists()) {
            try {
                String content = java.nio.file.Files.readString(cfgFile.toPath(), StandardCharsets.UTF_8);
                JsonElement el = JsonParser.parseString(content);
                if (el != null && el.isJsonObject() && el.getAsJsonObject().has("shareKey")) {
                    String k = el.getAsJsonObject().get("shareKey").getAsString();
                    if (k != null && k.startsWith("ERR-") && k.length() == 20) {
                        existingKey = k;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (existingKey == null || !existingKey.startsWith("ERR-") || existingKey.length() != 20) {
            existingKey = generateRandomKey();
        }

        configsMap.addProperty(cleanName, existingKey);
        keysMap.addProperty(existingKey, cleanName);
        keysRoot.add("configs", configsMap);
        keysRoot.add("keys", keysMap);

        try {
            java.nio.file.Files.writeString(keysFile.toPath(), gson.toJson(keysRoot), StandardCharsets.UTF_8);

            if (cfgFile.exists()) {
                File sharedDir = new File(configDir, "shared");
                if (!sharedDir.exists()) sharedDir.mkdirs();
                File sharedCopy = new File(sharedDir, existingKey + ".json");
                java.nio.file.Files.copy(cfgFile.toPath(), sharedCopy.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {}

        return existingKey;
    }

    public String createShareCode(String configName, int usages) {
        return getOrCreateShareKey(configName);
    }

    public synchronized boolean loadShareCode(String code, boolean notify) {
        if (code == null || code.trim().isEmpty()) {
            if (notify) ChatUtil.error("Ключ не может быть пустым!");
            return false;
        }

        String trimmed = code.trim();
        String upper = trimmed.toUpperCase();

        File sharedFile = new File(new File(configDir, "shared"), upper + ".json");
        if (sharedFile.exists()) {
            try {
                String json = java.nio.file.Files.readString(sharedFile.toPath(), StandardCharsets.UTF_8);
                String importName = "shared_" + (upper.length() > 8 ? upper.substring(4, 10) : upper);
                File target = getConfigFile(importName);
                java.nio.file.Files.writeString(target.toPath(), json, StandardCharsets.UTF_8);
                return loadConfig(importName, notify);
            } catch (Exception ignored) {}
        }

        File keysFile = new File(configDir, "shared_keys.json");
        if (keysFile.exists()) {
            try {
                String content = java.nio.file.Files.readString(keysFile.toPath(), StandardCharsets.UTF_8);
                JsonElement el = JsonParser.parseString(content);
                if (el != null && el.isJsonObject()) {
                    JsonObject root = el.getAsJsonObject();
                    if (root.has("keys")) {
                        JsonObject keysMap = root.getAsJsonObject("keys");
                        if (keysMap.has(upper)) {
                            String targetName = keysMap.get(upper).getAsString();
                            File targetFile = getConfigFile(targetName);
                            if (targetFile.exists()) {
                                return loadConfig(targetName, notify);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    try {
                        String content = java.nio.file.Files.readString(f.toPath(), StandardCharsets.UTF_8);
                        JsonElement el = JsonParser.parseString(content);
                        if (el != null && el.isJsonObject() && el.getAsJsonObject().has("shareKey")) {
                            if (upper.equalsIgnoreCase(el.getAsJsonObject().get("shareKey").getAsString())) {
                                String cfgName = f.getName().replace(".json", "");
                                return loadConfig(cfgName, notify);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (notify) {
            ChatUtil.error("Конфигурация с ключом '" + trimmed + "' не найдена!");
        }
        return false;
    }

    public List<String> getAvailableConfigs() {
        List<String> list = new ArrayList<>();
        if (configDir.exists() && configDir.isDirectory()) {
            File[] files = configDir.listFiles((dir, name1) -> name1.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    list.add(file.getName().replace(".json", ""));
                }
            }
        }
        return list;
    }
}