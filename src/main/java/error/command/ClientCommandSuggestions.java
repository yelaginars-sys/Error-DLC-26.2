package error.command;

import error.Client;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClientCommandSuggestions {
    private static final ClientCommandSuggestions INSTANCE = new ClientCommandSuggestions();
    public static ClientCommandSuggestions getInstance() { return INSTANCE; }

    public record SuggestionEntry(String text, String display, String description, boolean isSubCommand) {}

    private int selectedIndex = 0;
    private final List<SuggestionEntry> currentSuggestions = new ArrayList<>();
    private String lastQuery = "";

    public void update(String currentText) {
        if (currentText == null || !currentText.startsWith(".")) {
            currentSuggestions.clear();
            selectedIndex = 0;
            lastQuery = "";
            return;
        }

        if (currentText.equals(lastQuery)) return;
        lastQuery = currentText;

        currentSuggestions.clear();
        String raw = currentText.substring(1);
        String searchRaw = raw.trim().toLowerCase();

        CommandManager cm = Client.INSTANCE.commandManager;
        if (cm == null) return;

        if (!raw.contains(" ")) {
            // Typing command name: e.g. "." or ".c" or ".cfg"
            for (Command cmd : cm.getCommands()) {
                boolean matchesName = cmd.name().toLowerCase().startsWith(searchRaw);
                boolean matchesAlias = cmd.aliases().stream().anyMatch(a -> a.toLowerCase().startsWith(searchRaw));

                if (matchesName || matchesAlias || searchRaw.isEmpty()) {
                    String aliasStr = cmd.aliases().isEmpty() ? "" : " (" + String.join(", ", cmd.aliases()) + ")";
                    currentSuggestions.add(new SuggestionEntry(
                            "." + cmd.name(),
                            "." + cmd.name() + aliasStr,
                            cmd.description(),
                            false
                    ));
                }
            }
        } else {
            // Typing arguments for a command: e.g. ".cfg " or ".friend add "
            int firstSpace = raw.indexOf(' ');
            String cmdName = raw.substring(0, firstSpace).toLowerCase();
            String argsStr = raw.substring(firstSpace + 1);

            Command cmd = cm.find(cmdName);
            if (cmd != null) {
                String normalizedCmd = cmd.name();

                if ("cfg".equalsIgnoreCase(normalizedCmd)) {
                    int nextSpace = argsStr.indexOf(' ');
                    if (nextSpace == -1) {
                        String subSearch = argsStr.toLowerCase();
                        for (String sub : List.of("save", "load", "list", "dir")) {
                            if (sub.startsWith(subSearch)) {
                                String desc = switch (sub) {
                                    case "save" -> "Сохранить текущую конфигурацию";
                                    case "load" -> "Загрузить конфигурацию из файла";
                                    case "list" -> "Список всех сохраненных конфигураций";
                                    case "dir" -> "Открыть папку с конфигурациями";
                                    default -> "";
                                };
                                currentSuggestions.add(new SuggestionEntry("." + cmdName + " " + sub, sub, desc, true));
                            }
                        }
                    } else {
                        String sub = argsStr.substring(0, nextSpace).toLowerCase();
                        String valSearch = argsStr.substring(nextSpace + 1).toLowerCase();

                        if (("load".equals(sub) || "save".equals(sub)) && Client.INSTANCE.configManager != null) {
                            for (String cfg : Client.INSTANCE.configManager.getAvailableConfigs()) {
                                if (cfg.toLowerCase().startsWith(valSearch)) {
                                    currentSuggestions.add(new SuggestionEntry(
                                            "." + cmdName + " " + sub + " " + cfg,
                                            cfg,
                                            "Конфигурация",
                                            true
                                    ));
                                }
                            }
                        }
                    }
                } else if ("friend".equalsIgnoreCase(normalizedCmd)) {
                    int nextSpace = argsStr.indexOf(' ');
                    if (nextSpace == -1) {
                        String subSearch = argsStr.toLowerCase();
                        for (String sub : List.of("add", "remove", "list", "clear")) {
                            if (sub.startsWith(subSearch)) {
                                String desc = switch (sub) {
                                    case "add" -> "Добавить игрока в друзья";
                                    case "remove" -> "Удалить игрока из друзей";
                                    case "list" -> "Список сохраненных друзей";
                                    case "clear" -> "Очистить список друзей";
                                    default -> "";
                                };
                                currentSuggestions.add(new SuggestionEntry("." + cmdName + " " + sub, sub, desc, true));
                            }
                        }
                    } else {
                        String sub = argsStr.substring(0, nextSpace).toLowerCase();
                        String valSearch = argsStr.substring(nextSpace + 1).toLowerCase();

                        if ("add".equals(sub)) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.getConnection() != null) {
                                for (net.minecraft.client.multiplayer.PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                                    String name = info.getProfile().name();
                                    if (!error.friend.FriendManager.getInstance().isFriend(name) &&
                                            name.toLowerCase().startsWith(valSearch)) {
                                        currentSuggestions.add(new SuggestionEntry("." + cmdName + " add " + name, name, "Игрок онлайн", true));
                                    }
                                }
                            }
                        } else if ("remove".equals(sub) || "del".equals(sub)) {
                            for (String friend : error.friend.FriendManager.getInstance().getFriends()) {
                                if (friend.toLowerCase().startsWith(valSearch)) {
                                    currentSuggestions.add(new SuggestionEntry("." + cmdName + " " + sub + " " + friend, friend, "Друг", true));
                                }
                            }
                        }
                    }
                } else if ("staff".equalsIgnoreCase(normalizedCmd)) {
                    int nextSpace = argsStr.indexOf(' ');
                    if (nextSpace == -1) {
                        String subSearch = argsStr.toLowerCase();
                        for (String sub : List.of("add", "remove", "list", "clear")) {
                            if (sub.startsWith(subSearch)) {
                                String desc = switch (sub) {
                                    case "add" -> "Добавить игрока в стафф";
                                    case "remove" -> "Удалить игрока из стаффа";
                                    case "list" -> "Список стаффа";
                                    case "clear" -> "Очистить список стаффа";
                                    default -> "";
                                };
                                currentSuggestions.add(new SuggestionEntry("." + cmdName + " " + sub, sub, desc, true));
                            }
                        }
                    } else {
                        String sub = argsStr.substring(0, nextSpace).toLowerCase();
                        String valSearch = argsStr.substring(nextSpace + 1).toLowerCase();

                        if ("add".equals(sub)) {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.getConnection() != null) {
                                for (net.minecraft.client.multiplayer.PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                                    String name = info.getProfile().name();
                                    if (!error.staff.StaffManager.getInstance().isStaff(name) &&
                                            name.toLowerCase().startsWith(valSearch)) {
                                        currentSuggestions.add(new SuggestionEntry("." + cmdName + " add " + name, name, "Игрок онлайн", true));
                                    }
                                }
                            }
                        } else if ("remove".equals(sub)) {
                            for (String s : error.staff.StaffManager.getInstance().getStaff()) {
                                if (s.toLowerCase().startsWith(valSearch)) {
                                    currentSuggestions.add(new SuggestionEntry("." + cmdName + " remove " + s, s, "Стафф", true));
                                }
                            }
                        }
                    }
                } else if ("gps".equalsIgnoreCase(normalizedCmd)) {
                    String subSearch = argsStr.toLowerCase();
                    for (String sub : List.of("off", "clear", "stop")) {
                        if (sub.startsWith(subSearch)) {
                            currentSuggestions.add(new SuggestionEntry("." + cmdName + " " + sub, sub, "Управление GPS меткой", true));
                        }
                    }
                } else if ("builder".equalsIgnoreCase(normalizedCmd)) {
                    String subSearch = argsStr.toLowerCase();
                    for (String sub : List.of("start", "stop", "train", "load", "dir")) {
                        if (sub.startsWith(subSearch)) {
                            currentSuggestions.add(new SuggestionEntry("." + cmdName + " " + sub, sub, "Нейро-ротация", true));
                        }
                    }
                } else if ("help".equalsIgnoreCase(normalizedCmd)) {
                    String subSearch = argsStr.toLowerCase();
                    for (Command c : cm.getCommands()) {
                        if (c.name().toLowerCase().startsWith(subSearch)) {
                            currentSuggestions.add(new SuggestionEntry("." + cmdName + " " + c.name(), c.name(), c.description(), true));
                        }
                    }
                }
            }
        }

        if (selectedIndex >= currentSuggestions.size()) {
            selectedIndex = Math.max(0, currentSuggestions.size() - 1);
        }
    }

    public void render(float screenWidth, float screenHeight, float alpha) {
        if (currentSuggestions.isEmpty()) return;

        int themeAccent = Theme.getAccentColor();
        int maxVisible = Math.min(6, currentSuggestions.size());
        float itemH = 18.0F;
        float width = 260.0F;
        float height = maxVisible * itemH + 6.0F;

        float boxX = 14.0F;
        float boxY = screenHeight - 34.0F - height;

        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));
        int glassFill = ColorUtil.rgba(18, 16, 26, (int) (235 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (45 * alpha));
        int accentGlow = ColorUtil.multiplyAlpha(themeAccent, 0.25F * alpha);

        Render2D.drawShadow(boxX, boxY, width, height, 7.0F, 10.0F, shadowCol);
        Render2D.drawShadow(boxX, boxY, width, height, 7.0F, 6.0F, accentGlow);
        Render2D.drawBlur(boxX, boxY, width, height, 7.0F, 16.0F, glassFill, alpha);
        Render2D.drawRoundedRect(boxX, boxY, width, height, 7.0F, glassFill);
        Render2D.drawRoundedOutline(boxX, boxY, width, height, 7.0F, 1.0F, glassBorder);

        float itemY = boxY + 3.0F;
        for (int i = 0; i < maxVisible; i++) {
            SuggestionEntry entry = currentSuggestions.get(i);
            boolean selected = (i == selectedIndex);

            if (selected) {
                int activeBg = ColorUtil.multiplyAlpha(themeAccent, 0.35F * alpha);
                Render2D.drawRoundedRect(boxX + 4.0F, itemY, width - 8.0F, itemH, 4.0F, activeBg);
                Render2D.drawRoundedOutline(boxX + 4.0F, itemY, width - 8.0F, itemH, 4.0F, 1.0F, themeAccent);
            }

            int cmdCol = selected ? 0xFFFFFFFF : ColorUtil.rgba(240, 240, 255, (int) (240 * alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, entry.display(), boxX + 10.0F, itemY + 4.5F, 7.5F, cmdCol);

            if (!entry.description().isEmpty()) {
                float textW = Fonts.SF_MEDIUM.getWidth(entry.display(), 7.5F);
                float descX = Math.max(boxX + 10.0F + textW + 8.0F, boxX + width - 120.0F);
                Fonts.drawString(Fonts.SF_MEDIUM, entry.description(), descX, itemY + 5.0F, 6.5F, ColorUtil.rgba(160, 160, 180, (int) (180 * alpha)));
            }

            itemY += itemH;
        }
    }

    public boolean onKeyPressed(int keyCode, EditBox input) {
        if (currentSuggestions.isEmpty() || input == null) return false;

        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            selectedIndex = (selectedIndex + 1) % currentSuggestions.size();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            selectedIndex = (selectedIndex - 1 + currentSuggestions.size()) % currentSuggestions.size();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            applySelectedSuggestion(input);
            return true;
        }

        return false;
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button, EditBox input) {
        if (currentSuggestions.isEmpty() || input == null || button != 0) return false;

        int maxVisible = Math.min(6, currentSuggestions.size());
        float itemH = 18.0F;
        float width = 260.0F;
        float height = maxVisible * itemH + 6.0F;
        float boxX = 14.0F;
        float boxY = Minecraft.getInstance().getWindow().getGuiScaledHeight() - 34.0F - height;

        if (mouseX >= boxX && mouseX <= boxX + width && mouseY >= boxY && mouseY <= boxY + height) {
            int clickedIdx = (int) ((mouseY - (boxY + 3.0F)) / itemH);
            if (clickedIdx >= 0 && clickedIdx < maxVisible) {
                selectedIndex = clickedIdx;
                applySelectedSuggestion(input);
                return true;
            }
        }
        return false;
    }

    private void applySelectedSuggestion(EditBox input) {
        if (selectedIndex >= 0 && selectedIndex < currentSuggestions.size()) {
            String text = currentSuggestions.get(selectedIndex).text();
            input.setValue(text + " ");
            input.setCursorPosition(input.getValue().length());
        }
    }
}
