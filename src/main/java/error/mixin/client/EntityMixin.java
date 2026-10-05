package error.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import error.util.RotationHandler;
import error.event.EventManager;
import error.event.list.EventTravel;
import error.module.impl.player.NoPush;
import error.module.impl.render.FreeLook;

/**
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getViewVector", at = @At("HEAD"), cancellable = true)
    private void onGetViewVector(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        if (self == mc.player && RotationHandler.isActive()) {
            float pitch = RotationHandler.getServerPitch();
            float yaw = RotationHandler.getServerYaw();

            float f = pitch * ((float) Math.PI / 180F);
            float g = -yaw * ((float) Math.PI / 180F);
            float h = Mth.cos(g);
            float i = Mth.sin(g);
            float j = Mth.cos(f);
            float k = Mth.sin(f);

            cir.setReturnValue(new Vec3(i * j, -k, h * j));
        }
    }
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void onEntityTurn(double yRot, double xRot, CallbackInfo ci) {
        if ((Object) this == Minecraft.getInstance().player && FreeLook.INSTANCE != null && FreeLook.INSTANCE.isActive()) {
            FreeLook.INSTANCE.handleTurn(yRot, xRot);
            ci.cancel();
        }
    }
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPush(Entity entity, CallbackInfo ci) {
        if (NoPush.INSTANCE != null && NoPush.INSTANCE.isEnabled() && NoPush.INSTANCE.collisions.isEnabled("Игроки")) {
            Entity self = (Entity) (Object) this;
            Minecraft mc = Minecraft.getInstance();

            if (self == mc.player || entity == mc.player) {
                ci.cancel();
            }
        }
    }
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 onMove(Vec3 movement, MoverType moverType) {
        if ((Object) this instanceof LocalPlayer) {
            EventTravel event = new EventTravel(movement);
            EventManager.call(event);

            if (event.isCancelled()) {
                return Vec3.ZERO;
            }
            return event.getMovement() != null ? event.getMovement() : movement;
        }
        return movement;
    }

    @Inject(method = "isPushedByFluid", at = @At("HEAD"), cancellable = true)
    private void onIsPushedByFluid(CallbackInfoReturnable<Boolean> cir) {
        if (NoPush.INSTANCE != null && NoPush.INSTANCE.isEnabled() && NoPush.INSTANCE.collisions.isEnabled("Вода")) {
            Entity self = (Entity) (Object) this;
            if (self == Minecraft.getInstance().player) {
                cir.setReturnValue(false);
            }
        }
    }
}