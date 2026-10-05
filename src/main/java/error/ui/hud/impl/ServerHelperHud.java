package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.misc.ServerHelper;
import error.module.impl.render.Interface;
import error.setting.impl.BindSetting;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.KeyUtil;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ServerHelperHud extends HudElement {

    public record HelperEntry(Item item, String keyName, ItemStack stack) {}

    private final Animation widthAnim = new Animation(80.0F, 0.20F);
    private static final float PILL_H = 14.0F;
    private static final float ICON_SIZE = 9.5F;
    private static final float ITEM_SCALE = 0.59375F; // 9.5 / 16

    public ServerHelperHud() {
        super("server_helper_hud", "Server Helper", 10.0F, 220.0F, 120.0F, PILL_H, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.serverHelper.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    private boolean playerHasItem(Item item) {
        if (error.IMinecraft.mc.player == null) return false;
        Inventory inv = error.IMinecraft.mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(item)) {
                return true;
            }
        }
        return false;
    }

    private List<HelperEntry> getActiveEntries() {
        List<HelperEntry> list = new ArrayList<>();
        ServerHelper sh = ServerHelper.INSTANCE;
        if (sh == null) return list;

        if (sh.spookyMode.is("Funtime")) {
            checkAndAdd(list, Items.ENDER_EYE, sh.bindDisorient);
            checkAndAdd(list, Items.NETHERITE_SCRAP, sh.bindTrap);
            checkAndAdd(list, Items.DRIED_KELP, sh.bindPlast);
            checkAndAdd(list, Items.SUGAR, sh.bindDust);
            checkAndAdd(list, Items.SNOWBALL, sh.bindSnow);
            checkAndAdd(list, Items.WIND_CHARGE, sh.bindWindCharge);
        } else if (sh.spookyMode.is("HolyWorld")) {
            checkAndAdd(list, Items.NETHER_STAR, sh.bindStun);
            checkAndAdd(list, Items.PRISMARINE_SHARD, sh.bindExpTrap);
            checkAndAdd(list, Items.POPPED_CHORUS_FRUIT, sh.bindNormTrap);
            checkAndAdd(list, Items.FIRE_CHARGE, sh.bindBomb);
            checkAndAdd(list, Items.SNOWBALL, sh.bindHwSnow);
        } else if (sh.spookyMode.is("ReallyWorld")) {
            checkAndAdd(list, Items.HEART_OF_THE_SEA, sh.bindRwTrap);
            checkAndAdd(list, Items.ENDER_EYE, sh.bindRwEnderTrap);
            checkAndAdd(list, Items.PHANTOM_MEMBRANE, sh.bindRwAntiFly);
        } else if (sh.spookyMode.is("LonyGrief")) {
            checkAndAdd(list, Items.MAGMA_CREAM, sh.bindLgLeave);
            checkAndAdd(list, Items.CLAY_BALL, sh.bindLgPlatformLeave);
            checkAndAdd(list, Items.CRYING_OBSIDIAN, sh.bindLgTrap);
            checkAndAdd(list, Items.FEATHER, sh.bindLgFeather);
        }

        return list;
    }

    private void checkAndAdd(List<HelperEntry> list, Item item, BindSetting bind) {
        if (bind == null || bind.getValue().isEmpty()) return;
        int key = bind.getValue().get(0);
        if (!BindSetting.isValidKey(key)) return;

        if (playerHasItem(item)) {
            String kn = KeyUtil.getKeyName(key);
            list.add(new HelperEntry(item, kn, new ItemStack(item)));
        }
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean inChat = error.IMinecraft.mc.gui != null && error.IMinecraft.mc.gui.screen() instanceof ChatScreen;
        List<HelperEntry> entries = getActiveEntries();

        if (entries.isEmpty()) {
            if (!inChat) {
                this.width = 0;
                this.height = 0;
                return;
            }

            // ChatScreen Preview
            ServerHelper sh = ServerHelper.INSTANCE;
            if (sh != null && sh.spookyMode.is("ReallyWorld")) {
                entries = List.of(
                        new HelperEntry(Items.HEART_OF_THE_SEA, "M5", new ItemStack(Items.HEART_OF_THE_SEA)),
                        new HelperEntry(Items.ENDER_EYE, "M4", new ItemStack(Items.ENDER_EYE)),
                        new HelperEntry(Items.PHANTOM_MEMBRANE, "X", new ItemStack(Items.PHANTOM_MEMBRANE))
                );
            } else if (sh != null && sh.spookyMode.is("LonyGrief")) {
                entries = List.of(
                        new HelperEntry(Items.MAGMA_CREAM, "M5", new ItemStack(Items.MAGMA_CREAM)),
                        new HelperEntry(Items.CLAY_BALL, "M4", new ItemStack(Items.CLAY_BALL)),
                        new HelperEntry(Items.CRYING_OBSIDIAN, "X", new ItemStack(Items.CRYING_OBSIDIAN)),
                        new HelperEntry(Items.FEATHER, "Z", new ItemStack(Items.FEATHER))
                );
            } else if (sh != null && sh.spookyMode.is("HolyWorld")) {
                entries = List.of(
                        new HelperEntry(Items.NETHER_STAR, "M5", new ItemStack(Items.NETHER_STAR)),
                        new HelperEntry(Items.PRISMARINE_SHARD, "M4", new ItemStack(Items.PRISMARINE_SHARD)),
                        new HelperEntry(Items.POPPED_CHORUS_FRUIT, "X", new ItemStack(Items.POPPED_CHORUS_FRUIT)),
                        new HelperEntry(Items.FIRE_CHARGE, "Z", new ItemStack(Items.FIRE_CHARGE))
                );
            } else {
                entries = List.of(
                        new HelperEntry(Items.ENDER_EYE, "MOUSE5", new ItemStack(Items.ENDER_EYE)),
                        new HelperEntry(Items.NETHERITE_SCRAP, "MOUSE4", new ItemStack(Items.NETHERITE_SCRAP)),
                        new HelperEntry(Items.DRIED_KELP, "X", new ItemStack(Items.DRIED_KELP)),
                        new HelperEntry(Items.SUGAR, "Z", new ItemStack(Items.SUGAR)),
                        new HelperEntry(Items.SNOWBALL, "СКМ", new ItemStack(Items.SNOWBALL))
                );
            }
        }

        // Measure width
        float pad = 6.0F;
        float gap = 6.0F;
        float iconTextGap = 3.0F;

        float contentW = pad * 2.0F;
        for (int i = 0; i < entries.size(); i++) {
            HelperEntry e = entries.get(i);
            float kw = Fonts.SF_MEDIUM.getWidth(e.keyName, 8.5F);
            contentW += ICON_SIZE + iconTextGap + kw;
            if (i < entries.size() - 1) {
                contentW += gap;
            }
        }

        widthAnim.setTarget(contentW);
        widthAnim.update();

        this.width = widthAnim.getValue();
        this.height = PILL_H;

        // Draw Single Floating Rounded Glass Pill
        Render2D.drawHudPill(this.x, this.y, this.width, PILL_H, 1.0F);

        var extractor = event.getGuiGraphicsExtractor();
        float curX = this.x + pad;
        float itemY = this.y + (PILL_H - ICON_SIZE) / 2.0F;
        float textY = this.y + 2.5F;

        for (int i = 0; i < entries.size(); i++) {
            HelperEntry e = entries.get(i);
            float kw = Fonts.SF_MEDIUM.getWidth(e.keyName, 8.5F);

            // Draw Item
            if (extractor != null) {
                Render2DUtil.flush();
                try {
                    var pose = extractor.pose();
                    pose.pushMatrix();
                    pose.translate(curX, itemY);
                    pose.scale(ITEM_SCALE, ITEM_SCALE);
                    extractor.item(e.stack, 0, 0);
                    pose.popMatrix();
                } catch (Throwable ignored) {}
            }

            // Cooldown overlay
            if (error.IMinecraft.mc.player != null) {
                float cd = error.IMinecraft.mc.player.getCooldowns().getCooldownPercent(e.stack, 0.0F);
                if (cd > 0.01F) {
                    float cdH = ICON_SIZE * cd;
                    Render2D.drawRect(curX, itemY + (ICON_SIZE - cdH), ICON_SIZE, cdH, ColorUtil.rgba(255, 255, 255, 110));
                }
            }

            // Draw Key Name
            float textX = curX + ICON_SIZE + iconTextGap;
            Fonts.drawString(Fonts.SF_MEDIUM, e.keyName, textX, textY, 8.5F, 0xFFFFFFFF);

            curX += ICON_SIZE + iconTextGap + kw + gap;
        }
    }
}
