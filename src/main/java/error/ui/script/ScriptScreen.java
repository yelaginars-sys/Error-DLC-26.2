package error.ui.script;

import error.script.ScriptManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.RenderExtend;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;

/**
 * ScriptScreen - dedicated window for managing and editing scripts.
 */
public class ScriptScreen extends Screen {

    private final Screen parent;
    private boolean isEditing = true;
    private String newScriptName = "";
    private boolean creatingNewScript = false;

    public ScriptScreen(Screen parent) {
        super(Component.literal("Script Editor"));
        this.parent = parent;
    }

    public ScriptScreen() {
        this(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int screenW = this.width;
        int screenH = this.height;

        float windowW = 540.0F;
        float windowH = 340.0F;
        float winX = (screenW - windowW) / 2.0F;
        float winY = (screenH - windowH) / 2.0F;

        Render2DUtil.beginFrame();
        try {
            // Backdrop
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(4, 6, 10, 160));

            // Window Drop Shadow & Base
            Render2D.drawShadow(winX, winY, windowW, windowH, 16.0F, 16.0F, ColorUtil.rgba(0, 0, 0, 220));
            Render2D.drawRoundedRect(winX, winY, windowW, windowH, 14.0F, ColorUtil.rgba(14, 16, 26, 230));
            Render2D.drawRoundedOutline(winX, winY, windowW, windowH, 14.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 30));

            // Header Title
            Fonts.drawString(Fonts.SF_MEDIUM, "Скрипты (Script Editor)", winX + 20.0F, winY + 16.0F, 11.0F, 0xFFFFFFFF);
            String status = ScriptManager.getInstance().getStatusMessage();
            Fonts.drawString(Fonts.SF_MEDIUM, "Статус: " + status, winX + 20.0F, winY + 31.0F, 6.5F, ColorUtil.rgba(160, 175, 205, 220));

            // Left List: Files (140px wide)
            float listX = winX + 18.0F;
            float listY = winY + 48.0F;
            float listW = 140.0F;
            float listH = windowH - 64.0F;

            Render2D.drawRoundedRect(listX, listY, listW, listH, 8.0F, ColorUtil.rgba(20, 24, 38, 140));
            Render2D.drawRoundedOutline(listX, listY, listW, listH, 8.0F, 0.75F, ColorUtil.rgba(255, 255, 255, 25));

            List<String> files = ScriptManager.getInstance().getScriptFiles();
            String currentFile = ScriptManager.getInstance().getCurrentFileName();

            float fileItemY = listY + 6.0F;
            for (String file : files) {
                boolean active = file.equalsIgnoreCase(currentFile);
                boolean hov = mouseX >= listX + 4.0F && mouseX <= listX + listW - 4.0F && mouseY >= fileItemY && mouseY <= fileItemY + 20.0F;

                int itemBg = active ? ColorUtil.withAlpha(Theme.getAccentColor(), 80) : (hov ? ColorUtil.rgba(255, 255, 255, 18) : ColorUtil.rgba(255, 255, 255, 6));
                Render2D.drawRoundedRect(listX + 4.0F, fileItemY, listW - 8.0F, 20.0F, 5.0F, itemBg);
                Fonts.drawString(Fonts.SF_MEDIUM, file, listX + 8.0F, fileItemY + 5.5F, 6.5F, active ? 0xFFFFFFFF : ColorUtil.rgba(200, 210, 230, 200));

                fileItemY += 24.0F;
            }

            // Right Area: Code Editor
            float edX = listX + listW + 12.0F;
            float edY = listY;
            float edW = windowW - listW - 48.0F;
            float edH = listH - 32.0F;

            Render2D.drawRoundedRect(edX, edY, edW, edH, 8.0F, ColorUtil.rgba(10, 12, 20, 200));
            Render2D.drawRoundedOutline(edX, edY, edW, edH, 8.0F, 0.75F, ColorUtil.rgba(255, 255, 255, 25));

            // Code lines rendering
            String code = ScriptManager.getInstance().getScriptText();
            String[] lines = code.split("\n", -1);
            float textY = edY + 8.0F;

            int maxLines = (int) (edH / 12.0F) - 1;
            for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
                String line = lines[i];
                // Line numbers
                Fonts.drawString(Fonts.SF_MEDIUM, String.valueOf(i + 1), edX + 6.0F, textY, 6.0F, ColorUtil.rgba(100, 110, 130, 160));
                // Code text
                Fonts.drawString(Fonts.SF_MEDIUM, line, edX + 26.0F, textY, 6.2F, 0xFFFFFFFF);
                textY += 12.0F;
            }

            // Bottom Buttons: "Сохранить", "Выполнить", "Новый", "Папка", "Закрыть"
            float btnY = edY + edH + 6.0F;
            float btnH = 22.0F;

            // Save Button
            float saveX = edX;
            float saveW = 75.0F;
            boolean saveHov = mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= btnY && mouseY <= btnY + btnH;
            Render2D.drawRoundedRect(saveX, btnY, saveW, btnH, 6.0F, saveHov ? ColorUtil.withAlpha(Theme.getAccentColor(), 140) : ColorUtil.withAlpha(Theme.getAccentColor(), 90));
            Fonts.drawString(Fonts.SF_MEDIUM, "Сохранить", saveX + 14.0F, btnY + 6.5F, 6.8F, 0xFFFFFFFF);

            // Run Button
            float runX = saveX + saveW + 6.0F;
            float runW = 75.0F;
            boolean runHov = mouseX >= runX && mouseX <= runX + runW && mouseY >= btnY && mouseY <= btnY + btnH;
            Render2D.drawRoundedRect(runX, btnY, runW, btnH, 6.0F, runHov ? ColorUtil.rgba(40, 180, 80, 160) : ColorUtil.rgba(40, 180, 80, 100));
            Fonts.drawString(Fonts.SF_MEDIUM, "Выполнить", runX + 14.0F, btnY + 6.5F, 6.8F, 0xFFFFFFFF);

            // New Script Button
            float newX = runX + runW + 6.0F;
            float newW = 60.0F;
            boolean newHov = mouseX >= newX && mouseX <= newX + newW && mouseY >= btnY && mouseY <= btnY + btnH;
            Render2D.drawRoundedRect(newX, btnY, newW, btnH, 6.0F, newHov ? ColorUtil.rgba(255, 255, 255, 30) : ColorUtil.rgba(255, 255, 255, 14));
            Fonts.drawString(Fonts.SF_MEDIUM, "+ Новый", newX + 10.0F, btnY + 6.5F, 6.8F, 0xFFFFFFFF);

            // Folder Button
            float foldX = newX + newW + 6.0F;
            float foldW = 60.0F;
            boolean foldHov = mouseX >= foldX && mouseX <= foldX + foldW && mouseY >= btnY && mouseY <= btnY + btnH;
            Render2D.drawRoundedRect(foldX, btnY, foldW, btnH, 6.0F, foldHov ? ColorUtil.rgba(255, 255, 255, 30) : ColorUtil.rgba(255, 255, 255, 14));
            Fonts.drawString(Fonts.SF_MEDIUM, "Папка", foldX + 14.0F, btnY + 6.5F, 6.8F, 0xFFFFFFFF);

            // Close Button
            float closeX = winX + windowW - 70.0F;
            float closeW = 55.0F;
            boolean closeHov = mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= btnY && mouseY <= btnY + btnH;
            Render2D.drawRoundedRect(closeX, btnY, closeW, btnH, 6.0F, closeHov ? ColorUtil.rgba(220, 60, 60, 140) : ColorUtil.rgba(220, 60, 60, 80));
            Fonts.drawString(Fonts.SF_MEDIUM, "Закрыть", closeX + 10.0F, btnY + 6.5F, 6.8F, 0xFFFFFFFF);

        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            double mouseX = event.x();
            double mouseY = event.y();

            int screenW = this.width;
            int screenH = this.height;

            float windowW = 540.0F;
            float windowH = 340.0F;
            float winX = (screenW - windowW) / 2.0F;
            float winY = (screenH - windowH) / 2.0F;

            float listX = winX + 18.0F;
            float listY = winY + 48.0F;
            float listW = 140.0F;

            float edX = listX + listW + 12.0F;
            float edY = listY;
            float edW = windowW - listW - 48.0F;
            float edH = windowH - 64.0F - 32.0F;
            float btnY = edY + edH + 6.0F;
            float btnH = 22.0F;

            // Save Button
            float saveX = edX;
            float saveW = 75.0F;
            if (mouseX >= saveX && mouseX <= saveX + saveW && mouseY >= btnY && mouseY <= btnY + btnH) {
                ScriptManager.getInstance().saveCurrentScript();
                return true;
            }

            // Run Button
            float runX = saveX + saveW + 6.0F;
            float runW = 75.0F;
            if (mouseX >= runX && mouseX <= runX + runW && mouseY >= btnY && mouseY <= btnY + btnH) {
                ScriptManager.getInstance().executeCurrent();
                return true;
            }

            // New Script
            float newX = runX + runW + 6.0F;
            float newW = 60.0F;
            if (mouseX >= newX && mouseX <= newX + newW && mouseY >= btnY && mouseY <= btnY + btnH) {
                String newName = "Script_" + (ScriptManager.getInstance().getScriptFiles().size() + 1) + ".lua";
                ScriptManager.getInstance().loadScript(newName);
                return true;
            }

            // Open Folder
            float foldX = newX + newW + 6.0F;
            float foldW = 60.0F;
            if (mouseX >= foldX && mouseX <= foldX + foldW && mouseY >= btnY && mouseY <= btnY + btnH) {
                try {
                    java.awt.Desktop.getDesktop().open(ScriptManager.getInstance().getScriptsDirectory());
                } catch (Exception ignored) {}
                return true;
            }

            // Close
            float closeX = winX + windowW - 70.0F;
            float closeW = 55.0F;
            if (mouseX >= closeX && mouseX <= closeX + closeW && mouseY >= btnY && mouseY <= btnY + btnH) {
                onClose();
                return true;
            }

            // Select script from list
            List<String> files = ScriptManager.getInstance().getScriptFiles();
            float fileItemY = listY + 6.0F;
            for (String file : files) {
                if (mouseX >= listX + 4.0F && mouseX <= listX + listW - 4.0F && mouseY >= fileItemY && mouseY <= fileItemY + 20.0F) {
                    ScriptManager.getInstance().loadScript(file);
                    return true;
                }
                fileItemY += 24.0F;
            }
        }
        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        // Paste from clipboard
        if (event.hasControlDown() && key == GLFW.GLFW_KEY_V) {
            try {
                String clip = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                if (clip != null) {
                    ScriptManager.getInstance().setScriptText(ScriptManager.getInstance().getScriptText() + clip);
                    ScriptManager.getInstance().setModified(true);
                }
            } catch (Exception ignored) {}
            return true;
        }

        // Backspace
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            String text = ScriptManager.getInstance().getScriptText();
            if (!text.isEmpty()) {
                ScriptManager.getInstance().setScriptText(text.substring(0, text.length() - 1));
                ScriptManager.getInstance().setModified(true);
            }
            return true;
        }

        // Enter
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            ScriptManager.getInstance().setScriptText(ScriptManager.getInstance().getScriptText() + "\n");
            ScriptManager.getInstance().setModified(true);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char ch = (char) event.codepoint();
        if (ch >= 32) {
            ScriptManager.getInstance().setScriptText(ScriptManager.getInstance().getScriptText() + ch);
            ScriptManager.getInstance().setModified(true);
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        } else {
            super.onClose();
        }
    }
}
