package error.module.impl.render;

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import error.event.EventTarget;
import error.event.list.AttackEvent;
import error.event.list.PacketEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.world.module.PopEffectRenderer;

/**
 */
public final class PopEffect extends Module {

    public final CheckBox onlyPlayers = checkbox("Только игроки", true);

    public final ModeSetting totemEffect = mode("Эффект тотема", "Beam", "Beam", "Взрыв", "Выкл");
    public final ModeSetting killEffect = mode("Эффект убийства", "Beam", "Beam", "Взрыв", "Blood", "Выкл");
    public final ModeSetting hitEffect = mode("Эффект при ударе", "Молния", "Лень", "Молния", "Выкл");

    public final CheckBox themeColor = checkbox("Цвет от темы", true);
    public final ColorSetting customColor = color("Свой цвет", ColorUtil.rgba(255, 60, 60, 255)).visible(() -> !themeColor.getValue());

    public final SliderSetting count = slider("Количество частиц", 60.0F, 15.0F, 150.0F, 5.0F);
    public final SliderSetting size = slider("Размер частиц", 0.4F, 0.1F, 1.2F, 0.05F);
    public final SliderSetting lifetime = slider("Время жизни", 1.8F, 0.5F, 4.0F, 0.1F);
    public final SliderSetting speed = slider("Скорость", 1.0F, 0.2F, 3.0F, 0.1F);
    public final SliderSetting brightness = slider("Яркость свечения", 3.0F, 0.5F, 6.0F, 0.2F);
    public final CheckBox throughWalls = checkbox("Сквозь стены", true);

    private final PopEffectRenderer renderer = new PopEffectRenderer();

    public PopEffect() {
        super("PopEffect", "Бля прикольно от разных действий приколы особоно от игрока", Category.RENDER);
    }

    public PopEffectRenderer getRenderer() {
        return this.renderer;
    }

    public int getEffectColor() {
        return this.themeColor.getValue() ? Theme.getAccentColor() : this.customColor.getValue();
    }

    public boolean hasActiveEffects() {
        return this.renderer.hasActive();
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        Entity target = event.getTarget();
        if (target instanceof LivingEntity living) {
            if (this.onlyPlayers.getValue() && !(living instanceof Player)) {
                return;
            }

            String mode = this.hitEffect.getValue();
            if ("Молния".equalsIgnoreCase(mode)) {
                this.renderer.spawnLightning(living.position().add(0, living.getBbHeight() * 0.5, 0), this);
            } else if ("Призрак".equalsIgnoreCase(mode)) {
                this.renderer.spawnGhost(living, this);
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.level == null) return;

        if (event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEventId() == 35) {
                Entity entity = packet.getEntity(mc.level);
                if (entity != null) {
                    onTotemPop(entity);
                }
            }
        }
    }

    public void onTotemPop(Entity entity) {
        if (entity == null) return;
        if (this.onlyPlayers.getValue() && !(entity instanceof Player)) return;

        String mode = this.totemEffect.getValue();
        if ("Взрыв".equalsIgnoreCase(mode)) {
            this.renderer.spawnTotemExplosion(entity, this);
        } else if ("Beam".equalsIgnoreCase(mode)) {
            this.renderer.spawnTotemBeam(entity, this);
        }
    }

    public void onEntityKill(Entity entity) {
        if (entity == null) return;
        if (this.onlyPlayers.getValue() && !(entity instanceof Player)) return;

        String mode = this.killEffect.getValue();
        Vec3 pos = entity.position().add(0, entity.getBbHeight() * 0.5, 0);

        if ("Взрыв".equalsIgnoreCase(mode)) {
            this.renderer.spawnKillExplosion(pos, this);
        } else if ("Beam".equalsIgnoreCase(mode)) {
            this.renderer.spawnKillBeam(pos, this);
        } else if ("Blood".equalsIgnoreCase(mode)) {
            this.renderer.spawnBloodKill(entity.position(), entity.getBbWidth(), entity.getBbHeight(), this);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.renderer.clear();
    }
}