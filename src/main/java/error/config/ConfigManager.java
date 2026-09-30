package error.config;

import com.google.gson.*;
import lombok.Getter;
import lombok.Setter;
import error.Client;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.ui.hud.impl.NotificationHud;
import error.ui.hud.impl.PotionsHud;
import error.ui.hud.impl.TargetHud;
import error.ui.hud.impl.WatermarkHud;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.util.client.persiki.ChatUtil;
import error.util.client.clients.Theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

@Getter
public class ConfigManager {

    private final Preferences baseNode = Preferences.userRoot().node("ErrorDLC/configs");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ScheduledExecutorService autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Config-AutoSave-Thread");
        thread.setDaemon(true);
        return thread;
    });

    @Setter
    private String currentConfig = "default";

    public ConfigManager() {
        autoSaveExecutor.scheduleAtFixedRate(() -> {
            try {
                saveConfig(this.currentConfig, false);
            } catch (Throwable t) {
                System.err.println("[ConfigManager] Ошибка при фоновом автосохранении:");
                t.printStackTrace();
            }
        }, 2, 2, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                saveConfig(this.currentConfig, false);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, "Config-Shutdown-Save"));
    }

    public synchronized boolean saveConfig(String name, boolean notify) {
        if (name == null || name.trim().isEmpty()) {
            name = "default";
        }

        String cleanName = name.replace(".ww", "").replace(".json", "");

        try {
            JsonObject root = new JsonObject();

            JsonObject themeJson = new JsonObject();
            themeJson.addProperty("accentColor", Theme.getAccentColor());
            themeJson.addProperty("backgroundMode", Theme.getBackgroundMode());
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
                        JsonArray bindsArray = new JsonArray();
                        for (int code : module.getBind().getValue()) {
                            bindsArray.add(code);
                        }
                        moduleData.add("bind", bindsArray);
                    }

                    JsonObject settingsJson = new JsonObject();
                    for (Setting<?> setting : module.getSettings()) {
                        if (setting == null) continue;

                        if (setting instanceof CheckBox cb) {
                            settingsJson.addProperty(cb.getName(), cb.getValue());
                        } else if (setting instanceof SliderSetting slider) {
                            settingsJson.addProperty(slider.getName(), slider.getValue());
                        } else if (setting instanceof ModeSetting mode) {
                            settingsJson.addProperty(mode.getName(), mode.getValue());
                        } else if (setting instanceof MultiModeSetting multi) {
                            JsonArray activeModes = new JsonArray();
                            for (String m : multi.getValue()) {
                                activeModes.add(m);
                            }
                            settingsJson.add(multi.getName(), activeModes);
                        } else if (setting instanceof ColorSetting color) {
                            settingsJson.addProperty(color.getName(), color.getValue());
                        } else if (setting instanceof BindSetting bind) {
                            JsonArray bindsArray = new JsonArray();
                            for (int code : bind.getValue()) {
                                bindsArray.add(code);
                            }
                            settingsJson.add(bind.getName(), bindsArray);
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

                    if (element instanceof WatermarkHud watermark) {
                        elementData.add("customConfig", watermark.writeConfig());
                    } else if (element instanceof TargetHud targetHud) {
                        elementData.add("customConfig", targetHud.writeConfig());
                    } else if (element instanceof PotionsHud potionsHud) {
                        elementData.add("customConfig", potionsHud.writeConfig());
                    } else if (element instanceof NotificationHud notifHud) {
                        elementData.add("customConfig", notifHud.writeConfig());
                    }

                    hudJson.add(element.getClass().getSimpleName(), elementData);
                }
            }
            root.add("hud", hudJson);

            String jsonString = gson.toJson(root);
            Preferences configNode = baseNode.node(cleanName);
            configNode.put("data", jsonString);
            configNode.flush();

            this.currentConfig = cleanName;

            if (notify) {
                ChatUtil.success("Конфигурация '" + cleanName + "' успешно сохранена в Реестр!");
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

    @SuppressWarnings("unchecked")
    public synchronized boolean loadConfig(String name, boolean notify) {
        if (name == null || name.trim().isEmpty()) {
            name = "default";
        }

        String cleanName = name.replace(".ww", "").replace(".json", "");

        try {
            Preferences configNode = baseNode.node(cleanName);
            String jsonString = configNode.get("data", null);
            if (jsonString == null || jsonString.isEmpty()) {
                if (notify) {
                    ChatUtil.error("Конфигурация '" + cleanName + "' не найдена в Реестре!");
                }
                return false;
            }

            JsonObject root = JsonParser.parseString(jsonString).getAsJsonObject();

            if (root.has("theme")) {
                JsonObject themeJson = root.getAsJsonObject("theme");
                if (themeJson.has("accentColor")) Theme.setAccentColor(themeJson.get("accentColor").getAsInt());
                if (themeJson.has("backgroundMode")) Theme.setBackgroundMode(themeJson.get("backgroundMode").getAsString());
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

                        if (elementData.has("customConfig")) {
                            JsonObject customObj = elementData.getAsJsonObject("customConfig");
                            if (element instanceof WatermarkHud watermark) {
                                watermark.readConfig(customObj);
                            } else if (element instanceof TargetHud targetHud) {
                                targetHud.readConfig(customObj);
                            } else if (element instanceof PotionsHud potionsHud) {
                                potionsHud.readConfig(customObj);
                            } else if (element instanceof NotificationHud notifHud) {
                                notifHud.readConfig(customObj);
                            }
                        }
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
        }
    }

    public List<String> getAvailableConfigs() {
        try {
            return Arrays.asList(baseNode.childrenNames());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void deleteConfig(String name) {
        try {
            String cleanName = name.replace(".ww", "").replace(".json", "");
            baseNode.node(cleanName).removeNode();
            baseNode.flush();
            ChatUtil.success("Конфигурация '" + cleanName + "' удалена из Реестра.");
        } catch (Exception e) {
            ChatUtil.error("Не удалось удалить конфигурацию: " + e.getMessage());
        }
    }

    public void openFolder() {
        ChatUtil.info("Конфиги сохраняются напрямую в Реестр Windows:");
        ChatUtil.entry("Путь:", "HKEY_CURRENT_USER\\Software\\JavaSoft\\Prefs\\errordlc\\configs");
    }
}