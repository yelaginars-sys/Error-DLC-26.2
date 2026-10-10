package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import error.event.Event;
import error.event.list.PlayerTickEvent;
import zako.opt.ZakoOptConfig;

public class Optimization extends Module {

    public static Optimization INSTANCE;

    public final CheckBox entityLod = checkbox("Entity LOD", true);
    public final SliderSetting entityLodDistance = slider("Дистанция Entity LOD", 32.0F, 8.0F, 64.0F, 4.0F).visible(entityLod::getValue);
    public final CheckBox itemLod = checkbox("Item LOD", true);
    public final SliderSetting itemLodDistance = slider("Дистанция Item LOD", 16.0F, 4.0F, 48.0F, 2.0F).visible(itemLod::getValue);
    public final CheckBox playerLod = checkbox("Player LOD", true);
    public final CheckBox animFreeze = checkbox("Anim Freeze", true);
    public final SliderSetting animFreezeDist = slider("Дистанция Anim Freeze", 32.0F, 8.0F, 64.0F, 4.0F).visible(animFreeze::getValue);
    public final CheckBox spawnerReplay = checkbox("Spawner Replay", true);
    public final CheckBox spawnerCull = checkbox("Spawner Cull", true);
    public final CheckBox tileEntityCache = checkbox("Tile Entity Cache", true);
    public final CheckBox parallelModels = checkbox("Parallel Models", true);
    public final CheckBox parallelParticles = checkbox("Parallel Particles", true);
    public final CheckBox particleLod = checkbox("Particle LOD", true);
    public final CheckBox preparedTextCache = checkbox("Text Cache", true);
    public final CheckBox skinAtlas = checkbox("Skin Atlas", true);
    public final CheckBox frameFence = checkbox("Frame Fence", true);
    public final CheckBox fboShare = checkbox("FBO Share", true);
    public final CheckBox movingBlockCache = checkbox("Moving Block Cache", true);

    public Optimization() {
        super("Optimization", "Оптимизация рендера, сущностей, моделей и частиц (ZakoOpt)", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        applySettings();
    }

    @Override
    protected void onDisable() {
        resetSettings();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof PlayerTickEvent) {
            if (isEnabled()) {
                applySettings();
            }
        }
    }

    public void applySettings() {
        try {
            if (ZakoOptConfig.values == null) return;
            ZakoOptConfig.values.entityLod = entityLod.getValue();
            ZakoOptConfig.values.entityLodDistance = entityLodDistance.getValue().intValue();
            ZakoOptConfig.values.itemLod = itemLod.getValue();
            ZakoOptConfig.values.itemLodDistance = itemLodDistance.getValue().intValue();
            ZakoOptConfig.values.playerLod = playerLod.getValue();
            ZakoOptConfig.values.entityAnimFreeze = animFreeze.getValue();
            ZakoOptConfig.values.entityAnimDistance = animFreezeDist.getValue().intValue();
            ZakoOptConfig.values.spawnerReplay = spawnerReplay.getValue();
            ZakoOptConfig.values.spawnerCull = spawnerCull.getValue();
            ZakoOptConfig.values.blockEntityCache = tileEntityCache.getValue();
            ZakoOptConfig.values.parallelModels = parallelModels.getValue();
            ZakoOptConfig.values.parallelParticles = parallelParticles.getValue();
            ZakoOptConfig.values.particleLod = particleLod.getValue();
            ZakoOptConfig.values.preparedTextCache = preparedTextCache.getValue();
            ZakoOptConfig.values.skinAtlas = skinAtlas.getValue();
            ZakoOptConfig.values.frameFence = frameFence.getValue();
            ZakoOptConfig.values.fboShare = fboShare.getValue();
            ZakoOptConfig.values.movingBlockCache = movingBlockCache.getValue();
        } catch (Throwable ignored) {}
    }

    public void resetSettings() {
        try {
            if (ZakoOptConfig.values == null) return;
            ZakoOptConfig.values.entityLod = false;
            ZakoOptConfig.values.itemLod = false;
            ZakoOptConfig.values.playerLod = false;
            ZakoOptConfig.values.entityAnimFreeze = false;
            ZakoOptConfig.values.spawnerReplay = false;
            ZakoOptConfig.values.spawnerCull = false;
            ZakoOptConfig.values.blockEntityCache = false;
            ZakoOptConfig.values.parallelModels = false;
            ZakoOptConfig.values.parallelParticles = false;
            ZakoOptConfig.values.particleLod = false;
            ZakoOptConfig.values.preparedTextCache = false;
            ZakoOptConfig.values.skinAtlas = false;
            ZakoOptConfig.values.frameFence = false;
            ZakoOptConfig.values.fboShare = false;
            ZakoOptConfig.values.movingBlockCache = false;
        } catch (Throwable ignored) {}
    }
}
