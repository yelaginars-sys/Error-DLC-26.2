package error.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.util.Angle;
import error.util.PredictUtils;
import error.module.impl.combat.AuraModule;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Unique private final StopWatch elytraRotationTimer = new StopWatch();
    @Unique private final StopWatch elytraGraceTimer = new StopWatch();
    @Unique private boolean elytraFacingTarget = false;
    @Unique private boolean elytraActive = false;
    @Unique private boolean elytraVisualInitialized = false;
    @Unique private float elytraVisualYaw;
    @Unique private float elytraVisualPitch;
    @Unique private float elytraVisualHeadYaw;
    @Unique private long elytraLastFrameTime = System.currentTimeMillis();

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void submitCustomModel(net.minecraft.client.renderer.SubmitNodeCollector collector,
                                   net.minecraft.client.model.Model<?> model, Object state,
                                   com.mojang.blaze3d.vertex.PoseStack pose,
                                   net.minecraft.client.renderer.rendertype.RenderType renderType,
                                   int light, int overlay, int color,
                                   net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
                                   int outlineColor,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        String custom = state instanceof error.interfaces.CustomModelCarrier carrier ? carrier.error$customModel() : null;
        if (custom != null && !error.module.impl.render.CustomModels.NONE.equalsIgnoreCase(custom) && state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatarState) {
            error.module.impl.render.CustomModels models = error.module.impl.render.CustomModels.INSTANCE;
            if (model instanceof net.minecraft.client.model.player.PlayerModel playerModel) {
                if (error.module.impl.render.CustomModels.RABBIT.equalsIgnoreCase(custom)) {
                    error.util.render.model.RabbitModel.of(avatarState.id).submit(playerModel, avatarState, pose, collector, light, overlay, color, outlineColor, net.minecraft.resources.Identifier.fromNamespaceAndPath("error", "models/skycore/rabbit.png"));
                    return;
                } else if (error.module.impl.render.CustomModels.AMOGUS.equalsIgnoreCase(custom)) {
                    error.util.render.model.AmogusModel.of(avatarState.id).submit(playerModel, avatarState, pose, collector, light, overlay, color, outlineColor, net.minecraft.resources.Identifier.fromNamespaceAndPath("error", "models/skycore/amogus.png"));
                    return;
                } else if (error.module.impl.render.CustomModels.FREDDY.equalsIgnoreCase(custom)) {
                    error.util.render.model.FreddyModel.of(avatarState.id).submit(playerModel, avatarState, pose, collector, light, overlay, color, outlineColor, net.minecraft.resources.Identifier.fromNamespaceAndPath("error", "models/skycore/freddy.png"));
                    return;
                } else if (error.module.impl.render.CustomModels.RED_DEMON.equalsIgnoreCase(custom)) {
                    error.util.render.model.DemonModel.of(avatarState.id).submit(playerModel, avatarState, pose, collector, light, overlay, color, outlineColor, net.minecraft.resources.Identifier.fromNamespaceAndPath("error", "models/skycore/reddemon.png"));
                    return;
                } else if (error.module.impl.render.CustomModels.WHITE_DEMON.equalsIgnoreCase(custom)) {
                    error.util.render.model.DemonModel.of(avatarState.id).submit(playerModel, avatarState, pose, collector, light, overlay, color, outlineColor, net.minecraft.resources.Identifier.fromNamespaceAndPath("error", "models/skycore/whitedemon.png"));
                    return;
                }
            }
            if (error.module.impl.render.CustomModels.VERITY.equalsIgnoreCase(custom) && models != null) {
                error.util.render.model.VerityMesh.submit(avatarState, pose, collector, light, overlay, models.verityTexture(), models.size());
                return;
            } else if (error.module.impl.render.CustomModels.CHICKEN.equalsIgnoreCase(custom)) {
                error.util.render.model.ChickenMesh.submit(avatarState, pose, collector, light, overlay);
                return;
            }
        }
        collector.submitModel((net.minecraft.client.model.Model) model, state, pose, renderType, light, overlay, color, sprite, outlineColor, crumbling);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void applyElytraVisualRotation(T livingEntity, S renderState, float partialTicks, CallbackInfo ci) {
        LocalPlayer self = Minecraft.getInstance().player;
        if (self == null || livingEntity != self) return;

        float deltaSeconds = getFrameDeltaSeconds();
        float baseBodyYaw = renderState.bodyRot;
        float baseRelativeHeadYaw = renderState.yRot;
        float basePitch = renderState.xRot;

        AuraModule aura = AuraModule.INSTANCE;
        boolean enabled = aura != null && aura.isEnabled();
        LivingEntity target = enabled ? aura.getTarget() : null;

        boolean shouldRotate = false;
        float targetYaw = baseBodyYaw;
        float targetPitch = basePitch;

        if (enabled && livingEntity.isFallFlying() && target != null && target.isAlive() && target.isFallFlying()) {
            Vec3 playerPos = livingEntity.getPosition(partialTicks);
            Vec3 targetPos = target.getPosition(partialTicks);
            Vec3 targetLook = target.getViewVector(partialTicks).normalize();
            Vec3 targetToPlayer = playerPos.subtract(targetPos);
            double dot = targetToPlayer.dot(targetLook);

            PredictUtils.Type type = aura.getPredictType();
            Vec3 predict = PredictUtils.realPredict(target, type);
            double distToPredict = playerPos.distanceTo(predict);

            if (dot > 0.0 && distToPredict < 6.0) {
                shouldRotate = true;
                Angle rotation = Angle.fromVec(playerPos.add(0, self.getEyeHeight(), 0), targetPos.add(0.0, target.getBbHeight() * 0.5, 0.0));
                targetYaw = rotation.getYaw();
                targetPitch = rotation.getPitch();
            }
        }

        if (shouldRotate) {
            elytraGraceTimer.reset();

            if (!elytraActive) {
                elytraActive = true;
                elytraFacingTarget = true;
                elytraRotationTimer.reset();
                if (!elytraVisualInitialized) {
                    elytraVisualYaw = baseBodyYaw;
                    elytraVisualPitch = basePitch;
                    elytraVisualHeadYaw = baseRelativeHeadYaw;
                    elytraVisualInitialized = true;
                }
            } else if (elytraRotationTimer.finished(300)) {
                elytraFacingTarget = !elytraFacingTarget;
                elytraRotationTimer.reset();
            }
        } else {
            if (elytraActive) {
                if (elytraGraceTimer.finished(150)) {
                    elytraActive = false;
                    elytraFacingTarget = false;
                }
            }
        }

        if (elytraVisualInitialized) {
            float desiredYaw = elytraFacingTarget ? targetYaw : baseBodyYaw;
            float desiredPitch = elytraFacingTarget ? targetPitch : basePitch;
            float desiredHeadYaw = elytraFacingTarget ? 0.0F : baseRelativeHeadYaw;
            float smoothStep = Mth.clamp(deltaSeconds * 25.0F, 0.0F, 1.0F);

            elytraVisualYaw = Mth.rotLerp(smoothStep, elytraVisualYaw, desiredYaw);
            elytraVisualPitch = Mth.lerp(smoothStep, elytraVisualPitch, desiredPitch);
            elytraVisualHeadYaw = Mth.lerp(smoothStep, elytraVisualHeadYaw, desiredHeadYaw);

            renderState.bodyRot = elytraVisualYaw;
            renderState.xRot = elytraVisualPitch;
            renderState.yRot = elytraVisualHeadYaw;

            if (!elytraFacingTarget && !elytraActive) {
                float yawDiff = Math.abs(Mth.wrapDegrees(elytraVisualYaw - baseBodyYaw));
                float pitchDiff = Math.abs(elytraVisualPitch - basePitch);
                if (yawDiff < 1.0F && pitchDiff < 1.0F) {
                    elytraVisualInitialized = false;
                }
            }
        }
    }

    @Unique
    private float getFrameDeltaSeconds() {
        long now = System.currentTimeMillis();
        float deltaSeconds = Mth.clamp((now - elytraLastFrameTime) / 1000.0F, 0.0F, 0.1F);
        elytraLastFrameTime = now;
        return deltaSeconds;
    }

    @Unique
    private static class StopWatch {
        private long time = System.currentTimeMillis();
        public boolean finished(long ms) { return System.currentTimeMillis() - time >= ms; }
        public void reset() { time = System.currentTimeMillis(); }
    }
}