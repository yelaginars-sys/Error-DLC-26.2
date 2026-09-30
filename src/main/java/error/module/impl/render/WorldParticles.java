package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

/**
 * Create by daun kvass
 */
public final class WorldParticles extends Module {

    public final ModeSetting mode = mode("Режим", "Кубики", "Кубики", "Светлячки");

    public final SliderSetting fireflyCount = slider("Количество светлячков", 25.0F, 5.0F, 80.0F, 1.0F)
            .visible(() -> mode.getValue().equals("Светлячки"));

    public final SliderSetting count = slider("Count", 35.0F, 5.0F, 100.0F, 1.0F)
            .visible(() -> mode.getValue().equals("Кубики"));
    public final SliderSetting radius = slider("Radius", 8.0F, 3.0F, 20.0F, 1.0F)
            .visible(() -> mode.getValue().equals("Кубики"));
    public final SliderSetting size = slider("Size", 0.35f, 0.10f, 1.0f, 0.05f)
            .visible(() -> mode.getValue().equals("Кубики"));
    public final SliderSetting lifetime = slider("Lifetime", 5.0F, 2.0F, 15.0F, 0.5F)
            .visible(() -> mode.getValue().equals("Кубики"));

    public final CheckBox glow = checkbox("Glow Aura", true)
            .visible(() -> mode.getValue().equals("Кубики"));
    public final CheckBox innerGlow = checkbox("Inner Core Glow", true)
            .visible(() -> mode.getValue().equals("Кубики"));
    public final CheckBox diagonals = checkbox("Inner Diagonals", true)
            .visible(() -> mode.getValue().equals("Кубики"));
    public final CheckBox cornerEdges = checkbox("Corner Brackets", true)
            .visible(() -> mode.getValue().equals("Кубики"));

    public WorldParticles() {
        super("WorldParticles", "СЮЮЮ ПРАНК", Category.RENDER);
    }
}