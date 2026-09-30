package error.module.impl.misc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import error.util.client.persiki.KeyUtil;
import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.util.client.persiki.ChatUtil;

/**
 * Create by daun kvass
 */
public class ClickFriend extends Module {

    private final BindSetting key = bind("Bind", KeyUtil.UNBOUND);

    public ClickFriend() {
        super("Friends", "Друзья", Category.MISC);
    }

    @EventTarget
    public void onKey(KeyboardInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && key.matches(event.getKey())) {
            handleAction();
        }
    }

    @EventTarget
    public void onMouse(MouseInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && key.matchesMouse(event.getButton())) {
            if (handleAction()) {
                event.cancel();
            }
        }
    }

    private boolean handleAction() {
        if (mc.player == null || mc.level == null || mc.gui.screen() != null) return false;

        Entity target = getTargetEntity(6.0);
        if (target instanceof Player player && player != mc.player) {
            String name = player.getGameProfile().name();
            boolean added = FriendManager.getInstance().toggle(name);
            if (added) {
                ChatUtil.success("Игрок §a" + name + "§r добавлен в список друзей!");
            } else {
                ChatUtil.error("Игрок §c" + name + "§r удален из списка друзей!");
            }
            return true;
        }
        return false;
    }

    private Entity getTargetEntity(double distance) {
        if (mc.player == null || mc.level == null) return null;

        if (mc.crosshairPickEntity instanceof Player) {
            if (mc.player.distanceTo(mc.crosshairPickEntity) <= distance) {
                return mc.crosshairPickEntity;
            }
        }

        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 endPos = eyePos.add(lookVec.scale(distance));
        AABB box = mc.player.getBoundingBox().expandTowards(lookVec.scale(distance)).inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.player, eyePos, endPos, box,
                e -> e instanceof Player && !e.isSpectator() && e.isAlive() && e != mc.player,
                distance * distance
        );

        return hit != null ? hit.getEntity() : null;
    }
}