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
            themeJson.addProperty("backgroundMode", Theme.getBackgroundMode());
            themeJson.addProperty("glassStyle", Theme.getGlassStyle());
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
                if (themeJson.has("backgroundMode")) Theme.setBackgroundMode(themeJson.get("backgroundMode").getAsString());
                if (themeJson.has("glassStyle")) Theme.setGlassStyle(themeJson.get("glassStyle").getAsString());
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

    public String createShareCode(String configName, int usages) {
        return "ERROR-" + System.currentTimeMillis();
    }

    public boolean loadShareCode(String code, boolean notify) {
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