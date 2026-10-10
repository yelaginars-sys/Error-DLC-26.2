package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import dev.syntrix.clienttest.client.gui.ClickGuiController;
import dev.syntrix.clienttest.client.visual.VisualWorldRenderer;

/**
 * Advanced CrystalAura ported directly from Syntrix / Meow client.
 * Features:
 * - CombatCoordinator & CombatScheduler with synchronized tick executions
 * - MovementReceipt calculation and silent client rotations
 * - MotionPrediction with server position reconciliation
 * - Multi-stage entity exposure raycasting and damage calculation
 * - Dynamic obsidian placement, auto-break and dig logic
 */
public class CrystalAura extends Module {
    public static CrystalAura INSTANCE;

    public final ModeSetting mode = mode("Режим", "Custom", "Custom", "RW");
    public final SliderSetting targetRange = slider("Дистанция до цели", 8.0f, 4.0f, 12.0f, 0.5f);
    public final SliderSetting placeRange = slider("Дистанция постановки", 4.5f, 3.0f, 6.0f, 0.1f);
    public final SliderSetting breakRange = slider("Дистанция взрыва", 3.0f, 1.0f, 6.0f, 0.1f);
    public final SliderSetting minDamage = slider("Минимальный урон", 6.0f, 1.0f, 20.0f, 0.5f);
    public final SliderSetting maxSelfDamage = slider("Макс. урон себе", 8.0f, 0.0f, 20.0f, 0.5f);
    public final SliderSetting actionDelay = slider("Задержка (мс)", 0.0f, 0.0f, 500.0f, 10.0f);
    public final SliderSetting predictTicks = slider("Предикт тиков", 2.0f, 0.0f, 4.0f, 1.0f);

    public final CheckBox placeObsidian = checkbox("Ставить обсидиан", true);
    public final CheckBox legit = checkbox("Держать кристалл при ударе", false);
    public final CheckBox antiSuicide = checkbox("Анти-суицид", true);
    public final CheckBox autoDig = checkbox("Копать блоки", true);
    public final CheckBox skipObsidianDig = checkbox("Пропускать копку обсидиана", false);
    public final CheckBox render = checkbox("Отображать действия", true);
    public final CheckBox logs = checkbox("Логировать", false);
    public final CheckBox selfShield = checkbox("Щит себя", false);

    public CrystalAura() {
        super("CrystalAura", "Автоматический подрыв и постановка кристаллов Энда (Syntrix / Meow)", Category.COMBAT);
        INSTANCE = this;
    }

    public void syncSettings() {
        var values = ClickGuiController.module().values;
        values.put("crystalAuraMode", mode.getValue().equalsIgnoreCase("RW") ? 1.0 : 0.0);
        values.put("crystalAuraTargetRange", (double) targetRange.getValue());
        values.put("crystalAuraPlaceRange", (double) placeRange.getValue());
        values.put("crystalAuraBreakRange", (double) breakRange.getValue());
        values.put("crystalAuraMinDamage", (double) minDamage.getValue());
        values.put("crystalAuraMaxSelfDamage", (double) maxSelfDamage.getValue());
        values.put("crystalAuraDelay", (double) actionDelay.getValue());
        values.put("crystalAuraPredictTicks", (double) predictTicks.getValue());

        values.put("crystalAuraPlaceObsidian", placeObsidian.getValue() ? 1.0 : 0.0);
        values.put("crystalAuraLegit", legit.getValue() ? 1.0 : 0.0);
        values.put("crystalAuraAntiSuicide", antiSuicide.getValue() ? 1.0 : 0.0);
        values.put("crystalAuraAutoDig", autoDig.getValue() ? 1.0 : 0.0);
        values.put("crystalAuraSkipObsidianDig", skipObsidianDig.getValue() ? 1.0 : 0.0);
        values.put("crystalAuraRender", render.getValue() ? 1.0 : 0.0);
        values.put("crystalAuraLogs", logs.getValue() ? 1.0 : 0.0);
        values.put("crystalAuraSelfShield", selfShield.getValue() ? 1.0 : 0.0);
    }

    @Override
    public void onEnable() {
        syncSettings();
        ClickGuiController.setEnabled(true);
    }

    @Override
    public void onDisable() {
        ClickGuiController.setEnabled(false);
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent event) {
        if (isEnabled()) {
            syncSettings();
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (isEnabled() && render.getValue()) {
            VisualWorldRenderer.render3D();
        }
    }
}