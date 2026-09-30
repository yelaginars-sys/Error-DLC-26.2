package error.module.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import error.event.EventTarget;
import error.event.list.AttackEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ColorSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.world.module.HitEffectRenderer;

import java.util.List;

/**
 * Create by daun kvass
 */
public final class HitEffect extends Module {

    public static final String MODE_TARGET = "Per Player";
    public static final String MODE_WORLD = "Per World";

    public final MultiModeSetting modes = multiMode(
            "Режим",
            List.of(MODE_TARGET, MODE_WORLD),
            MODE_TARGET, MODE_WORLD
    );

    public final CheckBox themeColor = checkbox("Color From Theme", true);
    public final ColorSetting color = color("Color", ColorUtil.rgba(0, 200, 255, 255)).visible(()->!themeColor.getValue());
    public final SliderSetting targetRadius = slider("Radius Bulk", 1.5f, 0.5f, 3.5f, 0.1f).visible(()->modes.isEnabled(MODE_TARGET));
    public final SliderSetting worldRadius = slider("Radius wave", 8.0f, 2.0f, 25.0f, 0.5f).visible(()->modes.isEnabled(MODE_WORLD));
    public final SliderSetting distortion = slider("Strength distortion", 1.6f, 0.2f, 4.0f, 0.1f);
    public final SliderSetting glow = slider("Brightness glow", 1.2f, 0.1f, 3.0f, 0.1f);
    public final SliderSetting duration = slider("Duration ", 0.5f, 0.2f, 1.5f, 0.05f);

    private final HitEffectRenderer renderer = new HitEffectRenderer();

    public HitEffect() {
        super("HitEffect", "Булькс", Category.RENDER);
    }

    public HitEffectRenderer getRenderer() {
        return this.renderer;
    }

    public int getColor() {
        return this.themeColor.getValue() ? Theme.getAccentColor() : this.color.getValue();
    }

    public boolean hasActiveHits() {
        return this.renderer.hasHits();
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        Entity target = event.getTarget();
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 hitPos;

        if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() == target) {
            hitPos = entityHit.getLocation();
        } else {
            hitPos = target.position().add(0, target.getBbHeight() * 0.55D, 0);
        }

        if (this.modes.isEnabled(MODE_TARGET)) {
            this.renderer.addHit(hitPos, this.targetRadius.getValue().floatValue(), false, this);
        }
        if (this.modes.isEnabled(MODE_WORLD)) {
            this.renderer.addHit(hitPos, this.worldRadius.getValue().floatValue(), true, this);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.renderer.clear();
    }
}