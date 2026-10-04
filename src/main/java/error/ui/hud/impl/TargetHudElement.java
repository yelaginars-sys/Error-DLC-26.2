package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.impl.combat.AuraModule;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TargetHudElement extends HudElement implements IMinecraft {

    private static final Identifier HEART_ICON = Identifier.fromNamespaceAndPath("client", "textures/hud/heart.png");

    private final Animation hpAnimation = new Animation(20.0F, 0.25F);
    private final Animation absAnimation = new Animation(0.0F, 0.25F);

    // Modes & Settings
    public String mode = "Полоска"; // "Полоска" | "Кружочек"
    public String armorPos = "Сверху"; // "Сверху" | "Внутри"
    public boolean particlesEnabled = true;

    // Interactive settings panel state
    public boolean settingsOpen = false;
    private float panelX, panelY, panelW, panelH;

    public TargetHudElement() {
        super("targethud", "Target HUD", 350.0F, 220.0F, 185.0F, 48.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        LivingEntity target = getTarget();
        boolean isChat = screen() instanceof ChatScreen;

        if (!isChat && settingsOpen) {
            settingsOpen = false;
        }

        if (target == null && isChat) {
            target = mc.player;
        }

        if (target == null) {
            this.height = 0;
            return;
        }

        boolean isCircleMode = "Кружочек".equalsIgnoreCase(mode);
        boolean armorAbove = "Сверху".equalsIgnoreCase(armorPos);

        float baseHp = target.getHealth();
        float maxBaseHp = target.getMaxHealth() > 0 ? target.getMaxHealth() : 20.0F;
        float absorption = Math.max(0.0F, target.getAbsorptionAmount());
        float totalHp = baseHp + absorption;

        hpAnimation.setTarget(baseHp);
        hpAnimation.update();
        float animBaseHp = hpAnimation.getValue();

        absAnimation.setTarget(absorption);
        absAnimation.update();
        float animAbs = absAnimation.getValue();

        String hpText = (totalHp >= 100.0F || totalHp == (long) totalHp)
                ? String.format(Locale.US, "%.0f", totalHp)
                : String.format(Locale.US, "%.1f", totalHp);

        String name = target.getName().getString();
        if (name.length() > 14) name = name.substring(0, 12) + "…";

        // Target Equipment
        List<ItemStack> armorList = new ArrayList<>();
        armorList.add(target.getItemBySlot(EquipmentSlot.HEAD));
        armorList.add(target.getItemBySlot(EquipmentSlot.CHEST));
        armorList.add(target.getItemBySlot(EquipmentSlot.LEGS));
        armorList.add(target.getItemBySlot(EquipmentSlot.FEET));

        List<ItemStack> heldList = new ArrayList<>();
        heldList.add(target.getMainHandItem());
        heldList.add(target.getOffhandItem());

        List<ItemStack> allEquip = new ArrayList<>(armorList);
        allEquip.addAll(heldList);

        MsdfFont nameFont = Fonts.SF_MEDIUM;
        float nameSize = 8.0F;
        float nameW = nameFont.getWidth(name, nameSize);

        float H = armorAbove ? 38.0F : 48.0F;
        float RADIUS = 8.0F;
        float facePad = 5.0F;
        float faceSize = armorAbove ? 28.0F : 36.0F;
        float contentStartX = facePad + faceSize + 8.0F;
        float rightPad = 8.0F;

        float hpTextW = nameFont.getWidth(hpText, 7.5F);
        float hpGroupW = hpTextW + 3.0F + 8.0F;
        float minContentW = contentStartX + nameW + 16.0F + hpGroupW + rightPad;
        float W = Math.max(170.0F, minContentW);

        this.width = W;
        this.height = H;

        // Render Equipment Above Card if set
        var extractor = event.getGuiGraphicsExtractor();
        if (armorAbove && !allEquip.isEmpty() && extractor != null) {
            float itemSize = 14.0F;
            float itemSpacing = itemSize + 4.0F;
            float totalEquipW = allEquip.size() * itemSpacing - 4.0F;
            float equipStartX = x + (W - totalEquipW) / 2.0F;
            float equipY = y - itemSize - 5.0F;

            Render2DUtil.flush();
            Matrix3x2fStack pose = extractor.pose();

            float curX = equipStartX;
            for (ItemStack stack : allEquip) {
                if (stack != null && !stack.isEmpty()) {
                    pose.pushMatrix();
                    pose.translate(curX, equipY);
                    pose.scale(itemSize / 16.0F, itemSize / 16.0F);
                    extractor.item(stack, 0, 0);
                    pose.popMatrix();
                }
                curX += itemSpacing;
            }
        }

        // Dark Card Container with Frosted Glass Blur
        int bgColor = ColorUtil.rgba(18, 20, 28, 230);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 150);

        Render2D.drawBlur(x, y, W, H, RADIUS, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, W, H, RADIUS, bgColor, 1.0F, outlineColor);

        // Player Face Head
        float faceY = y + (H - faceSize) / 2.0F;
        Identifier skinLocation = null;
        if (target instanceof AbstractClientPlayer clientPlayer) {
            if (clientPlayer.getSkin() != null && clientPlayer.getSkin().body() != null) {
                skinLocation = clientPlayer.getSkin().body().texturePath();
            }
        }
        if (skinLocation != null) {
            Render2D.drawHead(skinLocation, x + facePad, faceY, faceSize, 5.0F);
        } else {
            Render2D.drawRoundedRect(x + facePad, faceY, faceSize, faceSize, 5.0F, ColorUtil.rgba(40, 42, 54, 255));
        }

        float topY = y + (armorAbove ? 6.0F : 7.0F);

        // Name
        Fonts.drawString(nameFont, name, x + contentStartX, topY, nameSize, ColorUtil.rgba(250, 250, 255, 255));

        // HP Text & Heart Icon
        float heartX = x + W - rightPad - 8.0F;
        float heartY = topY + 0.5F;
        float hpTextX = heartX - 3.0F - hpTextW;

        Fonts.drawString(nameFont, hpText, hpTextX, topY, 7.5F, ColorUtil.rgba(255, 120, 120, 255));
        Render2D.drawTexture(HEART_ICON, heartX, heartY, 8.0F, 8.0F, ColorUtil.rgba(240, 70, 80, 255));

        // Render Equipment Inside Card if not above
        if (!armorAbove && extractor != null) {
            Render2DUtil.flush();
            Matrix3x2fStack pose = extractor.pose();
            float itemSize = 13.0F;
            float itemSpacing = itemSize + 3.0F;
            float itemsY = y + 17.0F;
            float curX = x + contentStartX;

            for (ItemStack stack : allEquip) {
                if (stack != null && !stack.isEmpty()) {
                    pose.pushMatrix();
                    pose.translate(curX, itemsY);
                    pose.scale(itemSize / 16.0F, itemSize / 16.0F);
                    extractor.item(stack, 0, 0);
                    pose.popMatrix();
                }
                curX += itemSpacing;
            }
        }

        // Animated Health Bar
        float barY = y + H - (armorAbove ? 9.0F : 8.0F);
        float barX = x + contentStartX;
        float barW = W - contentStartX - rightPad;
        float barH = 5.0F;
        float barRadius = 2.5F;

        Render2D.drawRoundedRect(barX, barY, barW, barH, barRadius, ColorUtil.rgba(30, 32, 42, 220));

        float totalMax = maxBaseHp + (animAbs > 0 ? animAbs : 0.0F);
        float baseRatio = Math.clamp(animBaseHp / totalMax, 0.0F, 1.0F);
        float absRatio = Math.clamp(animAbs / totalMax, 0.0F, 1.0F);

        float baseFillW = barW * baseRatio;
        if (baseFillW > 0.5F) {
            Render2D.drawGradientRound(barX, barY, baseFillW, barH, barRadius,
                    Theme.getAccentColor(), ColorUtil.rgba(70, 220, 130, 255), ColorUtil.rgba(70, 220, 130, 255), Theme.getAccentColor());
        }

        if (animAbs > 0.0F) {
            float absFillW = barW * absRatio;
            float goldX = barX + baseFillW;
            if (goldX + absFillW > barX + barW) {
                absFillW = (barX + barW) - goldX;
            }
            if (absFillW > 0.5F) {
                int gold1 = ColorUtil.rgba(255, 215, 0, 255);
                int gold2 = ColorUtil.rgba(245, 160, 20, 255);
                Render2D.drawGradientRound(goldX, barY, absFillW, barH, barRadius, gold1, gold2, gold2, gold1);
            }
        }

        // Render Right-Click Settings Panel in ChatScreen
        if (isChat && settingsOpen) {
            renderSettingsPanel();
        }
    }

    private void renderSettingsPanel() {
        float S = 1.0F;
        panelW = 130.0F;
        panelH = 48.0F;
        panelX = x + width + 6.0F;
        panelY = y;

        int panelBg = ColorUtil.rgba(16, 18, 26, 240);
        int borderCol = ColorUtil.rgba(255, 255, 255, 45);
        int accent = Theme.getAccentColor();

        Render2D.drawBlur(panelX, panelY, panelW, panelH, 7.0F, panelBg, 1.0F);
        Render2D.drawRoundedRectWithOutline(panelX, panelY, panelW, panelH, 7.0F, panelBg, 1.0F, borderCol);

        Fonts.drawString(Fonts.SF_MEDIUM, "TargetHUD Настройки", panelX + 6.0F, panelY + 5.0F, 6.5F, ColorUtil.rgba(240, 240, 250, 255));

        // Mode Row ("Полоска" / "Кружочек")
        float btnW = (panelW - 16.0F) / 2.0F;
        float btnH = 12.0F;

        float mode1X = panelX + 6.0F;
        float mode2X = mode1X + btnW + 4.0F;
        float modeY = panelY + 16.0F;

        boolean isBar = "Полоска".equalsIgnoreCase(mode);
        Render2D.drawRoundedRect(mode1X, modeY, btnW, btnH, 3.5F, isBar ? accent : ColorUtil.rgba(30, 34, 48, 200));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Полоска", mode1X + btnW / 2.0F, modeY + 3.0F, 5.5F, ColorUtil.rgba(255, 255, 255, 255));

        Render2D.drawRoundedRect(mode2X, modeY, btnW, btnH, 3.5F, !isBar ? accent : ColorUtil.rgba(30, 34, 48, 200));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Кружочек", mode2X + btnW / 2.0F, modeY + 3.0F, 5.5F, ColorUtil.rgba(255, 255, 255, 255));

        // Armor Pos Row ("Сверху" / "Внутри")
        float armorY = modeY + btnH + 4.0F;
        boolean isTop = "Сверху".equalsIgnoreCase(armorPos);
        Render2D.drawRoundedRect(mode1X, armorY, btnW, btnH, 3.5F, isTop ? accent : ColorUtil.rgba(30, 34, 48, 200));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Сверху", mode1X + btnW / 2.0F, armorY + 3.0F, 5.5F, ColorUtil.rgba(255, 255, 255, 255));

        Render2D.drawRoundedRect(mode2X, armorY, btnW, btnH, 3.5F, !isTop ? accent : ColorUtil.rgba(30, 34, 48, 200));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Внутри", mode2X + btnW / 2.0F, armorY + 3.0F, 5.5F, ColorUtil.rgba(255, 255, 255, 255));
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (!(screen() instanceof ChatScreen)) return false;

        // Right-Click (button 1): Toggle settings panel
        if (button == 1) {
            if (isHovered(mouseX, mouseY)) {
                settingsOpen = !settingsOpen;
                return true;
            }
        }

        // Left-Click (button 0): Click inside settings panel
        if (button == 0 && settingsOpen) {
            float btnW = (panelW - 16.0F) / 2.0F;
            float btnH = 12.0F;

            float mode1X = panelX + 6.0F;
            float mode2X = mode1X + btnW + 4.0F;
            float modeY = panelY + 16.0F;
            float armorY = modeY + btnH + 4.0F;

            if (mouseX >= mode1X && mouseX <= mode1X + btnW && mouseY >= modeY && mouseY <= modeY + btnH) {
                mode = "Полоска";
                return true;
            }
            if (mouseX >= mode2X && mouseX <= mode2X + btnW && mouseY >= modeY && mouseY <= modeY + btnH) {
                mode = "Кружочек";
                return true;
            }
            if (mouseX >= mode1X && mouseX <= mode1X + btnW && mouseY >= armorY && mouseY <= armorY + btnH) {
                armorPos = "Сверху";
                return true;
            }
            if (mouseX >= mode2X && mouseX <= mode2X + btnW && mouseY >= armorY && mouseY <= armorY + btnH) {
                armorPos = "Внутри";
                return true;
            }
        }

        return false;
    }

    private LivingEntity getTarget() {
        if (AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            return AuraModule.INSTANCE.getTarget();
        }
        return null;
    }
}
