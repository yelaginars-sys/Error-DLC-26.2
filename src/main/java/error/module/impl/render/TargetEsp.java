package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.module.impl.combat.AuraModule;
import error.module.impl.combat.TriggerBot;
import error.setting.impl.CheckBox;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.target.TargetMarkers;
import net.minecraft.world.entity.LivingEntity;

public class TargetEsp extends Module {
    public static TargetEsp INSTANCE;

    public final ModeSetting mode = mode("Режим", "Ромб", "Ромб", "Кружок", "Crystal", "Призраки", "Призраки 2");
    public final CheckBox colorOnHit = checkbox("Краснеть при ударе", true);

    public final SliderSetting size = slider("Размер", 1.0F, 0.4F, 2.0F, 0.05F);
    public final SliderSetting rotSpeed = slider("Скорость вращения", 1.0F, 0.2F, 3.0F, 0.05F);
    public final SliderSetting opacity = slider("Прозрачность", 1.0F, 0.1F, 1.0F, 0.05F);
    public final CheckBox onHover = checkbox("При наводке", true);

    public final ModeSetting colorMode = mode("Цвет", "Тема", "Тема", "Кастом");
    public final ColorSetting customColor = color("Цвет кастом", ColorUtil.rgba(0, 220, 255, 255)).visible(() -> colorMode.is("Кастом"));

    private LivingEntity hoverTarget = null;
    private long lastHoverTime = 0L;

    public TargetEsp() {
        super("Target ESP", "Подсветка цели атаки", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        hoverTarget = null;
        lastHoverTime = 0L;
        TargetMarkers.INSTANCE.release();
    }

    public LivingEntity getTarget() {
        if (AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            LivingEntity auraTarget = AuraModule.INSTANCE.getTarget();
            if (auraTarget.isAlive()) {
                hoverTarget = auraTarget;
                lastHoverTime = System.currentTimeMillis();
                return auraTarget;
            }
        }

        if (TriggerBot.INSTANCE != null && TriggerBot.INSTANCE.isEnabled() && TriggerBot.INSTANCE.getTarget() != null) {
            LivingEntity tbTarget = TriggerBot.INSTANCE.getTarget();
            if (tbTarget.isAlive()) {
                hoverTarget = tbTarget;
                lastHoverTime = System.currentTimeMillis();
                return tbTarget;
            }
        }

        if (onHover.getValue() && mc != null) {
            if (mc.crosshairPickEntity instanceof LivingEntity living && living != mc.player && living.isAlive()) {
                hoverTarget = living;
                lastHoverTime = System.currentTimeMillis();
            }
            if (hoverTarget != null && hoverTarget.isAlive() && System.currentTimeMillis() - lastHoverTime < 600L) {
                return hoverTarget;
            }
        }

        return null;
    }

    public int getEspColor() {
        if ("Кастом".equalsIgnoreCase(colorMode.getValue())) {
            return customColor.getValue();
        }
        return Theme.getAccentColor();
    }
}
