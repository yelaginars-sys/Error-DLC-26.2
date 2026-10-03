package error.module.impl.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import error.mixin.accessor.ItemInHandRendererAccessor;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

public class HoldMyItems extends Module {

    public static HoldMyItems INSTANCE;

    public final ModeSetting animationType = mode("Animation", "Обычный", "Обычный", "Шарп");
    public final SliderSetting smoothness = slider("Smoothness", 1.0F, 0.35F, 2.5F, 0.05F);
    public final CheckBox swapHands = checkbox("Swap hands", false);
    public final CheckBox climbAndCrawl = checkbox("Climb and crawl", true);
    public final CheckBox swimmingAnimation = checkbox("Swimming animation", true);
    public final CheckBox mb3DCompat = checkbox("3D compat", false);

    private static final java.util.Random RANDOM = new java.util.Random();

    private boolean repPower = false;
    private float prevAge = 0.0F;
    private double previousRotation = 0.0;
    private float swingAngleY = 0.0F;
    private float swingAngleX = 0.0F;
    private float swingVelocityY = 0.0F;
    private float swingVelocityX = 0.0F;
    private float swingVelocityZ = 0.0F;
    private float vertAngleY = 0.0F;
    private float vertVelocityY = 0.0F;
    private float vertVelocityYSlime = 0.0F;
    private float vertAngleYSlime = 0.0F;
    private float riptideCounter = 0.0F;
    private float netherCounter = 0.0F;
    private float fallCounter = 0.0F;
    private float inWaterCounter = 0.0F;
    private float freezeCounter = 0.0F;
    private float clCount = 0.0F;
    private float crawlCount = 0.0F;
    private float directionalCrawlCount = 0.0F;
    private float climbCount = 0.0F;
    private float mouseHolding = 1.0F;
    private boolean isAttacking = false;
    private boolean left = false;

    public HoldMyItems() {
        super("Hold My Items", "Кастомная анимация предмета в руке", Category.PLAYER);
        INSTANCE = this;
    }

    public void handleRenderItem(ItemInHandRenderer renderer, AbstractClientPlayer player, float frameInterp, float pitch,
                                 InteractionHand hand, float swingProgress, ItemStack item, float equipProgress,
                                 PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (!isEnabled() || player == null) return;

        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        float sideFactor = 1.0F;

        if (swapHands.getValue()) {
            arm = arm.getOpposite();
            sideFactor *= -1.0F;
        }

        renderCustomFirstPersonItem(renderer, player, frameInterp, pitch, hand, arm, sideFactor, swingProgress, item,
                equipProgress, poseStack, collector, light);
    }

    private void renderCustomFirstPersonItem(ItemInHandRenderer renderer, AbstractClientPlayer player, float frameInterp,
                                             float pitch, InteractionHand hand, HumanoidArm arm, float sideFactor,
                                             float swingProgress, ItemStack item, float equipProgress,
                                             PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (!isEnabled()) return;
        if (player.isUsingItem() && player.getUseItem().is(Items.SPYGLASS)) return;

        ItemInHandRendererAccessor self = (ItemInHandRendererAccessor) renderer;

        float yaw = player.getYRot();
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        var velocity = player.getDeltaMovement();
        double dotProduct = velocity.x * forwardX + velocity.z * forwardZ;
        double crossProduct = velocity.x * forwardZ - velocity.z * forwardX;
        float al;

        if (player.getXRot() != 0.0F) {
            al = 90.0F / player.getXRot() / 10.0F;
        } else {
            al = 1.0F;
        }

        if (al > 1.0F) al = 1.0F;
        if (al < 0.0F) al = 1.0F;

        boolean bl = hand == InteractionHand.MAIN_HAND;
        poseStack.pushPose();
        poseStack.pushPose();

        double tt = (1.0 / Math.max(mc.getFps(), 1)) * 30.0;
        float smoothnessFactor = Mth.clamp(smoothness.getValue(), 0.35F, 2.5F);
        float hmiProgress = (float) Math.pow(Mth.clamp(swingProgress, 0.0F, 1.0F), smoothnessFactor);
        float swingRot = hmiProgress < 0.6F
                ? Mth.sin(Mth.clamp(hmiProgress, 0.0F, 0.12506F) * 12.56F)
                : Mth.sin(Mth.clamp(hmiProgress, 0.62532F, 0.75038F) * 12.56F);
        float swing = Mth.sin(hmiProgress * 3.14F);
        swing = easeInOutBack(swing);
        boolean sharpSword = item.is(ItemTags.SWORDS) && isSharpAnimation();

        if ((item.is(Items.EXPERIENCE_BOTTLE) || item.is(Items.WIND_CHARGE) || item.is(Items.EGG)
                || item.is(Items.ENDER_EYE) || item.is(Items.SNOWBALL)
                || item.getItem() instanceof SplashPotionItem || item.getItem() instanceof LingeringPotionItem)
                && player.getOffhandItem().isEmpty() && useAnimation(item) != ItemUseAnimation.SPEAR
                && !item.is(Items.FIRE_CHARGE) && !player.isSwimming() && !player.isVisuallyCrawling()
                && !player.onClimbable()) {
            if (player.getMainArm() == HumanoidArm.LEFT) bl = !bl;

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-25.0F * sideFactor));
            poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(25.0F * sideFactor * swing));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
            poseStack.translate(-0.15 * sideFactor, 0.1, 0.1);
            poseStack.translate(0.0F, -0.55 * swing, 0.4 * swing * 3.14F);
            self.hmi$renderPlayerArm(poseStack, collector, light, 0.0F, 0.0F, arm.getOpposite());
            poseStack.popPose();
        }

        if (mc.options.keyAttack.isDown() && !this.isAttacking && swingProgress == 0.0F) {
            this.left = !this.left;
        }

        if (!item.isEmpty()) {
            if (player.getMainArm() == HumanoidArm.LEFT) bl = !bl;

            if ((this.left || item.is(ItemTags.AXES) || useAnimation(item) == ItemUseAnimation.SPEAR
                    || useAnimation(item) == ItemUseAnimation.BLOCK) && !item.is(ItemTags.SHOVELS)) {
                if (sharpSword) {
                    poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.5F * swing);
                    poseStack.mulPose(Axis.XN.rotationDegrees(-30.0F * swingRot));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot * sideFactor));
                    poseStack.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                } else if (!item.is(ItemTags.SWORDS) && !item.is(ItemTags.AXES)) {
                    if (useAnimation(item) == ItemUseAnimation.SPEAR) {
                        poseStack.translate(0.0F, 0.0F, 0.45 * swingRot);
                        poseStack.translate(-0.25F * sideFactor * swing, -0.35 * swingRot, -0.6F * swing);
                        poseStack.translate(0.0F, 0.1 * swing, 0.0F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot * sideFactor));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F * swingRot * sideFactor));
                    } else if (isTool(item) && useAnimation(item) != ItemUseAnimation.BLOCK && !item.is(ItemTags.SHOVELS)) {
                        poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.5F * swing);
                        poseStack.mulPose(Axis.XN.rotationDegrees(-30.0F * swingRot));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot * sideFactor));
                        poseStack.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                    } else if (useAnimation(item) != ItemUseAnimation.BLOCK) {
                        poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.1F * swing);
                        poseStack.mulPose(Axis.XN.rotationDegrees(-30.0F * swingRot));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot * sideFactor));
                        poseStack.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                        poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * swing * sideFactor));
                    } else {
                        poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.2F * swing);
                        poseStack.mulPose(Axis.XN.rotationDegrees(-10.0F * swingRot));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot * sideFactor));
                        poseStack.mulPose(Axis.XN.rotationDegrees(20.0F * swing));
                    }
                } else {
                    poseStack.translate(0.8 * sideFactor * swingRot, 0.3 * swingRot, -0.5F * swing);
                    poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot * sideFactor));
                    poseStack.mulPose(Axis.XN.rotationDegrees(-20.0F * swingRot));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-70.0F * swingRot * sideFactor));
                    poseStack.mulPose(Axis.XN.rotationDegrees(item.is(ItemTags.SWORDS) ? 40.0F * swing : 30.0F * swing));
                }
            } else if (!item.is(ItemTags.SHOVELS)) {
                if (sharpSword) {
                    poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.5F * swing);
                    poseStack.mulPose(Axis.XN.rotationDegrees(-30.0F * swingRot));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot * sideFactor));
                    poseStack.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                } else if (item.is(ItemTags.SWORDS)) {
                    poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.5F * swing);
                    poseStack.mulPose(Axis.XN.rotationDegrees(-30.0F * swingRot));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot * sideFactor));
                    poseStack.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                } else if (isTool(item) && !item.is(ItemTags.SHOVELS)) {
                    poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.5F * swing);
                    poseStack.mulPose(Axis.XN.rotationDegrees(-30.0F * swingRot));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot * sideFactor));
                    poseStack.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                } else {
                    poseStack.translate(0.1 * sideFactor * swingRot, 0.1 * swingRot, -0.1F * swing);
                    poseStack.mulPose(Axis.XN.rotationDegrees(-30.0F * swingRot));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot * sideFactor));
                    poseStack.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                    poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * swing * sideFactor));
                }
            } else {
                poseStack.translate(0.0F, 0.15 * swingRot, -0.25F * swingRot);
                poseStack.translate(0.0F, 0.0F, -0.2 * swing);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
                poseStack.mulPose(Axis.XN.rotationDegrees(-35.0F * swingRot));
                poseStack.mulPose(Axis.XN.rotationDegrees(30.0F * swing));
            }
        } else if (item.getItem() instanceof BlockItem
                && (!isTool(item) || item.is(ItemTags.TRIMMABLE_ARMOR) || item.is(ItemTags.BOOKSHELF_BOOKS)
                || useAnimation(item) == ItemUseAnimation.EAT || !item.isEnchantable())
                && useAnimation(item) != ItemUseAnimation.BOW && useAnimation(item) != ItemUseAnimation.SPYGLASS
                && getAttackDamage(item) == 0.0F && useAnimation(item) != ItemUseAnimation.BLOCK
                && !item.is(Items.WARPED_FUNGUS_ON_A_STICK) && !item.is(Items.CARROT_ON_A_STICK)
                && !item.is(Items.FISHING_ROD) && !item.is(Items.SHEARS)) {
            swingProgress = (float) (swingProgress * 1.2);
            if (swingProgress > 1.0F) swingProgress = 0.0F;
        } else if (!item.is(ItemTags.SHOVELS)) {
            swingProgress = (float) (swingProgress * 1.5F);
            if (swingProgress > 1.0F) swingProgress = 0.0F;
        }

        float speed = (float) velocity.length();
        if (speed >= 0.08F) {
            this.crawlCount = (float) (this.crawlCount + 0.1 * speed * 2.0F * tt);
            this.directionalCrawlCount = (float) (this.directionalCrawlCount + 0.1 * dotProduct * 4.0F * tt);
            this.directionalCrawlCount = (float) (this.directionalCrawlCount + (dotProduct > 0.0F
                    ? 0.1 * Math.abs(crossProduct) * 4.0F * tt
                    : 0.1 * Math.abs(crossProduct) * -1.0F * 4.0F * tt));
        }

        if (velocity.y > 0.0F) this.climbCount = (float) (this.climbCount + 0.1 * tt);
        if (velocity.y < 0.0F) this.climbCount = (float) (this.climbCount - 0.1 * tt);

        if ((player.isVisuallyCrawling() && climbAndCrawl.getValue()
                || player.onClimbable() && !player.onGround() && Math.abs(velocity.y) > 0.0F && climbAndCrawl.getValue())
                && !player.isUsingItem() && swingProgress == 0.0F) {
            this.clCount = (float) (this.clCount + 0.1 * tt);
            if (this.clCount > 1.0F) this.clCount = 1.0F;

            if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F * this.clCount));
            }
        } else {
            this.clCount = (float) (this.clCount * Math.pow(0.88F, tt));
        }

        if (swingProgress == 0.0F) {
            poseStack.translate(bl ? player.getXRot() / 650.0F * this.clCount * -1.0F
                    : player.getXRot() / 650.0F * this.clCount, 0.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(player.getXRot() * this.clCount));
        }

        if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
            poseStack.translate(0.0F, 0.0F, player.getXRot() / 120.0F * this.clCount);
        } else if (swingProgress == 0.0F) {
            poseStack.translate(0.0F, 0.0F, player.getXRot() / 80.0F * this.clCount);
        }

        if (player.onClimbable() && climbAndCrawl.getValue() && !player.onGround()
                && !item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN) && !player.isUsingItem()) {
            poseStack.translate(0.0F, 0.1, -0.2);
        }

        if (player.isInWater() && !player.isSwimming() && !player.isUnderWater()) {
            this.inWaterCounter = (float) (this.inWaterCounter + 0.1 * tt);
            if (this.inWaterCounter >= 1.0F) this.inWaterCounter = 1.0F;
        } else {
            this.inWaterCounter = (float) (this.inWaterCounter * Math.pow(0.88F, tt));
        }

        if (false && player.getTicksFrozen() > 0.1) {
            this.freezeCounter = (float) (this.freezeCounter + 0.1 * tt);
        } else {
            this.freezeCounter = (float) (this.freezeCounter * Math.pow(0.88F, tt));
        }

        poseStack.translate(0.0F, 0.02 * this.inWaterCounter, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(8.0F * sideFactor * this.inWaterCounter));
        poseStack.mulPose(Axis.XP.rotationDegrees(0.3F * Mth.sin(this.freezeCounter * 5.0F)));

        if (velocity.y < -0.85 && item.is(Items.MACE) && player.getMainHandItem() == item) {
            this.fallCounter = (float) (this.fallCounter + 0.1 * tt);
            if (this.fallCounter >= 1.0F) this.fallCounter = 1.0F;
        } else {
            this.fallCounter = (float) (this.fallCounter * Math.pow(0.88F, tt));
        }

        if (bl) {
            poseStack.mulPose(Axis.XP.rotationDegrees(45.0F * this.fallCounter));
            poseStack.translate(0.0F, -0.2 * this.fallCounter, 0.0F);
        }

        this.vertAngleY = (float) (this.vertAngleY + velocity.y * 0.015F * tt);
        this.vertAngleY = (float) (this.vertAngleY - 0.1F * this.vertAngleY * tt);
        this.vertAngleY = (float) (this.vertAngleY * Math.pow(0.88F, tt));
        this.vertVelocityYSlime = (float) (this.vertVelocityYSlime + velocity.y * 0.015F * tt);
        this.vertVelocityYSlime = (float) (this.vertVelocityYSlime - 0.1F * this.vertAngleYSlime * tt);
        this.vertVelocityYSlime = (float) (this.vertVelocityYSlime * Math.pow(0.88F, tt));
        this.vertAngleYSlime = (float) (this.vertAngleYSlime + this.vertVelocityYSlime * tt);
        poseStack.translate(0.0F, this.vertAngleY * -1.0F, 0.0F);
        poseStack.translate(0.0F, Mth.sin(player.tickCount * 0.1F) * 0.007 * sideFactor, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(0.15F * Mth.sin(player.tickCount * 0.15F) * sideFactor));

        if (!item.isEmpty() || player.isVisuallyCrawling() || player.onClimbable() && !player.onGround() || player.isSwimming()) {
            if (player.getMainArm() == HumanoidArm.LEFT) bl = !bl;

            if (useAnimation(item) == ItemUseAnimation.BLOCK) {
                poseStack.translate(0.0F, 0.0F, 0.0F);
            } else {
                poseStack.translate(0.0F, -0.1, 0.1);
            }
        }

        if (item.is(Items.LANTERN) || item.is(Items.SOUL_LANTERN) || item.is(ItemTags.HANGING_SIGNS)) {
            poseStack.translate(0.0F, 0.1, 0.0F);
            if (player.isSwimming()) poseStack.translate(0.0F, -0.1, 0.1);
        }

        if (player.isSwimming() && swingProgress == 0.0F && swimmingAnimation.getValue()) {
            double distance = this.crawlCount;
            double swingAmplitude = 1.5F;
            double frequency = 2.0F;
            double s = distance * frequency;
            double handRotation = Mth.sin(s) * swingAmplitude;
            double smoothRotation = handRotation * 0.8 + this.previousRotation * 0.2;
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (bl ? smoothRotation : -smoothRotation)));
            poseStack.translate(0.0F, 0.0F, smoothRotation * 0.2F);
            double k = this.crawlCount * 2.0F;
            double a = Mth.cos(k);
            double b = a;
            if (a <= 0.0F) b = a * 0.5;

            poseStack.mulPose(Axis.YN.rotationDegrees((float) (bl ? b * 30.0 : b * 30.0 * -1.0)));
            poseStack.translate(0.0F, 0.0F, a * 0.2F);

            if (item.isEmpty() && !bl && !player.isInvisible()) {
                poseStack.translate(1.0F * sideFactor, 0.0F - equipProgress * 0.3, 0.3);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                altSwing(poseStack, arm, swingProgress);
                poseStack.scale(0.9F, 0.9F, 0.9F);
                self.hmi$renderPlayerArm(poseStack, collector, light, 0.0F, 0.0F, arm);
            }

            this.previousRotation = smoothRotation;
        }

        if ((player.onClimbable() && !player.onGround() || player.isVisuallyCrawling() && swingProgress == 0.0F)
                && !player.isUsingItem()) {
            double s = this.climbCount;
            float v = (float) velocity.y;
            float a = Mth.cos((float) s * 2.0F);

            if (player.onClimbable()) {
                if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(20.0F * a * sideFactor));
                } else {
                    poseStack.mulPose(Axis.XP.rotationDegrees(1.0F * a * sideFactor));
                }
            }

            if (player.isVisuallyCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
                float crawlProgress = Mth.sin(this.directionalCrawlCount * 4.0F * this.mouseHolding);
                float upAndDown = Mth.cos(this.directionalCrawlCount * 4.0F * this.mouseHolding);
                if (item.is(Items.LANTERN) || item.is(Items.SOUL_LANTERN)) {
                    crawlProgress *= 0.14F;
                    upAndDown *= 0.14F;
                }

                poseStack.translate(0.2 * crawlProgress, 0.3 * crawlProgress * sideFactor, -0.2 * crawlProgress * sideFactor * al);
                poseStack.mulPose(Axis.YP.rotationDegrees(25.0F * crawlProgress));
                poseStack.mulPose(Axis.XP.rotationDegrees(Mth.clamp(20.0F * upAndDown * sideFactor, 0.0F, 20.0F)));
            }

            if (item.isEmpty() && !bl && !player.isInvisible()
                    && (!player.onGround() && player.onClimbable() || player.isVisuallyCrawling())) {
                poseStack.translate(1.0F * sideFactor, 0.0F - equipProgress * 0.3, 0.3);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                altSwing(poseStack, arm, swingProgress);
                poseStack.scale(0.9F, 0.9F, 0.9F);
                self.hmi$renderPlayerArm(poseStack, collector, light, 0.0F, 0.0F, arm);
            }
        }

        if (item.isEmpty()) {
            if (!player.isInvisible()) {
                float cleanSwing = Mth.sin(Mth.clamp(swingProgress, 0.0F, 1.0F) * (float) Math.PI);
                poseStack.translate(0.05F * sideFactor * cleanSwing, 0.05F * cleanSwing, -0.15F * cleanSwing);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * cleanSwing * sideFactor));
                poseStack.mulPose(Axis.XN.rotationDegrees(-15.0F * cleanSwing));
                self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm);
            }
        } else if (item.has(DataComponents.MAP_ID)) {
            if (bl && mc.player.getMainHandItem().isEmpty()) {
                poseStack.translate(0.0F, 0.1, 0.0F);
                self.hmi$renderTwoHandedMap(poseStack, collector, light, pitch, equipProgress, swingProgress);
            } else {
                poseStack.translate(bl ? -0.1 : 0.1, 0.1, 0.0F);
                self.hmi$renderOneHandedMap(poseStack, collector, light, equipProgress, arm, swingProgress, item);
            }
        } else if (useAnimation(item) == ItemUseAnimation.CROSSBOW) {
            poseStack.pushPose();
            boolean charged = CrossbowItem.isCharged(item);
            boolean bl2 = arm == HumanoidArm.RIGHT;
            int i = bl2 ? 1 : -1;

            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                applyEquipOffset(poseStack, arm, equipProgress);
                poseStack.translate(i * -0.4785682F, -0.24387F, 0.05731531F);
                poseStack.mulPose(Axis.XN.rotationDegrees(-11.935F));
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 65.3F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(i * 9.785F));
                float f = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                float g = f / (float) CrossbowItem.getChargeDuration(item, player);
                if (g > 1.0F) g = 1.0F;

                if (g > 0.1F) {
                    float h = Mth.sin((f - 0.1F) * 1.3F);
                    float k = (g - 0.1F) * h;
                    poseStack.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                }

                poseStack.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                poseStack.scale(1.0F, 1.0F, 1.0F);
                poseStack.mulPose(Axis.YN.rotationDegrees(i * 45.0F));
            } else {
                swingArm(swingProgress, poseStack, i, equipProgress, arm);
                if (charged && swingProgress < 0.001F && bl) {
                    poseStack.translate(i * -0.341864F, 0.0F, 0.0F);
                    poseStack.mulPose(Axis.YP.rotationDegrees(i * 10.0F));
                }
            }

            poseStack.translate(0.0F, 0.0F, -1.0F);
            poseStack.translate(-0.45 * i, 0.45, 1.7);
            poseStack.translate(1.0F * sideFactor, 0.0F - equipProgress * 0.3, 0.3);
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            altSwing(poseStack, arm, swingProgress);
            poseStack.scale(0.9F, 0.9F, 0.9F);
            self.hmi$renderPlayerArm(poseStack, collector, light, 0.0F, 0.0F, arm);
            poseStack.translate(-0.25F * i, 1.25, 0.05);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90 * i));
            poseStack.mulPose(Axis.XP.rotationDegrees(77.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(85 * i));
            poseStack.scale(1.2F, 1.2F, 1.2F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));
            poseStack.translate(0.0F, -0.15, 0.15);
            renderItem(renderer, player, item, bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                    : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, collector, light);
            poseStack.popPose();

            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                float f = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                float g = f / (float) CrossbowItem.getChargeDuration(item, player);
                if (g > 1.0F) g = 1.0F;

                if (g > 0.1F) {
                    float h = Mth.sin((f - 0.1F) * 1.3F);
                    float k = (g - 0.1F) * h;
                    poseStack.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                }

                poseStack.mulPose(Axis.YN.rotationDegrees(g <= 0.2F ? 75.0F * g * 5.0F * i : 75 * i));
                poseStack.mulPose(Axis.XN.rotationDegrees(10.0F * g * 1.5F));
                poseStack.translate(-0.37 * i, 0.0F, 0.6);
                poseStack.translate(0.15 * g * i, 0.0F, 0.0F);
                self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm.getOpposite());
            }
        } else {
            boolean bl2 = arm == HumanoidArm.RIGHT;
            int l = bl2 ? 1 : -1;

            if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                switch (useAnimation(item)) {
                    case NONE -> applyEquipOffset(poseStack, arm, equipProgress);
                    case EAT, DRINK -> {
                        float u = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                        float y = u / 5.0F;
                        if (y > 1.0F) y = 1.0F;

                        float q = Mth.sin(u / 2.0F * 3.14F);
                        q /= 10.0F;
                        poseStack.translate(1 * l, 0.1, 0.3);
                        poseStack.translate(0.2 * l * y, -0.7 * y, -0.2 * y);
                        poseStack.translate(0.0F, -0.2 * q, -0.2 * q);
                        poseStack.translate(0.0F, 0.1 * easeInOutBack(Mth.sin(y * 3.14F)), 0.0F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(45 * l));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-40 * l));
                        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                        altSwing(poseStack, arm, swingProgress);
                        poseStack.scale(0.9F, 0.9F, 0.9F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * y * l));
                        self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm);
                    }
                    case BLOCK -> {
                        float k = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                        float s = k / 4.0F;
                        float s2 = k / 6.0F;
                        if (s > 1.0F) s = 1.0F;
                        if (s2 > 1.0F) s2 = 1.0F;

                        poseStack.translate(0.0F, -0.2, 0.0F);
                        poseStack.translate(1 * l, 0.0F, 0.3);
                        poseStack.translate(0.7 * s * l, 0.0F, -1.3 * s);
                        poseStack.translate(-0.2 * l * s2, 0.0F, 0.0F);
                        poseStack.mulPose(Axis.XN.rotationDegrees((float) (10.0F * Mth.sin(s2 * 3.14F))));
                        poseStack.mulPose(Axis.YP.rotationDegrees(70.0F * s * l));
                        poseStack.mulPose(Axis.YP.rotationDegrees(45 * l));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-40 * l));
                        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                        poseStack.mulPose(Axis.YP.rotationDegrees(5 * l * s));
                        poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F * s));
                        poseStack.translate(0.0F, 0.0F, -0.2 * s);
                        altSwing(poseStack, arm, swingProgress);
                        poseStack.scale(0.9F, 0.9F, 0.9F);
                        self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm);
                        poseStack.translate(0.35 * l, -0.13, -0.12);
                        poseStack.mulPose(Axis.ZP.rotationDegrees(10.0F * l));
                        poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * l));
                        poseStack.mulPose(Axis.XP.rotationDegrees(0.0F));
                        poseStack.translate(-0.2 * l, -0.04, 0.15);
                        poseStack.scale(1.0F, 1.0F, 1.0F);
                    }
                    case BOW -> {
                        poseStack.pushPose();
                        if (player.getMainArm() == HumanoidArm.LEFT) bl = !bl;

                        float m1 = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                        float f1 = m1 / 20.0F;
                        float f = (f1 * f1 + f1 * 2.0F) / 3.0F;
                        if (f1 > 1.0F) f1 = 1.0F;

                        if (f1 > 0.1F) {
                            float g1 = Mth.sin((m1 - 0.1F) * 1.3F);
                            float j1 = g1 * f1;
                            poseStack.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                        }

                        poseStack.translate(bl ? -0.1 : 0.1, 0.0F, f1 * 0.15);
                        self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm);
                        poseStack.popPose();
                        poseStack.translate(bl ? -0.5F : 0.5F, -0.45, 0.1);
                        poseStack.mulPose(Axis.XP.rotation(0.3F));
                        if (bl) {
                            poseStack.mulPose(Axis.ZN.rotation(-0.3F));
                            poseStack.mulPose(Axis.YN.rotation(1.0F));
                        } else {
                            poseStack.mulPose(Axis.ZP.rotation(-0.3F));
                            poseStack.mulPose(Axis.YP.rotation(1.0F));
                        }

                        self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm.getOpposite());
                        poseStack.mulPose(bl ? Axis.YN.rotation(2.5F) : Axis.YP.rotation(2.5F));
                        poseStack.translate(bl ? -0.65F : 0.65F, -0.35, 0.27);
                        if (f1 > 1.0F) f1 = 1.0F;

                        poseStack.popPose();
                        if (mb3DCompat.getValue()) {
                            poseStack.mulPose(Axis.YP.rotationDegrees(10 * l));
                        }

                        poseStack.mulPose(Axis.XN.rotationDegrees(75.0F));
                        poseStack.mulPose(Axis.ZN.rotationDegrees(-15 * l));
                        poseStack.translate(0.8 * l, 0.0F - equipProgress * 0.3F, -0.1);

                        if (f > 0.1F) {
                            float g1 = Mth.sin((m1 - 0.1F) * 1.3F);
                            float h1 = f1 - 0.1F;
                            float j1 = g1 * h1;
                            poseStack.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                        }

                        poseStack.pushPose();
                    }
                    case SPEAR, TRIDENT -> {
                        if (player.getOffhandItem().isEmpty() && !player.isVisuallyCrawling()
                                && !player.isSwimming() && !player.onClimbable()) {
                            poseStack.pushPose();
                            poseStack.mulPose(Axis.YP.rotationDegrees(-25 * l));
                            poseStack.translate(-0.15 * l, 0.1, 0.1);
                            self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm.getOpposite());
                            poseStack.popPose();
                        }

                        float m = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                        float f = m / 10.0F;
                        if (f > 1.0F) f = 1.0F;

                        if (f > 0.1F) {
                            float g = Mth.sin((m - 0.1F) * 1.3F);
                            float h = f - 0.1F;
                            float j = g * h;
                            poseStack.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                        }

                        poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
                        poseStack.mulPose(Axis.YP.rotationDegrees(25 * l));
                        poseStack.translate(0.2 * l, 0.0F, 0.8);
                        self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm);
                        poseStack.mulPose(Axis.XP.rotationDegrees(135.0F));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(-65 * l));
                        poseStack.translate(0.65F * l, -1.0F, -0.6);
                    }
                    case BRUSH -> {
                        float g5 = (float) (player.getUseItemRemainingTicks() % 10);
                        float h5 = g5 - frameInterp + 1.0F;
                        float n = -15.0F + 75.0F * Mth.cos(h5 * 2.0F * (float) Math.PI);
                        float z = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                        float x = z / 4.0F;
                        if (x > 1.0F) x = 1.0F;

                        poseStack.mulPose(Axis.YP.rotationDegrees(25 * l * x));
                        poseStack.translate(0.3F * l * x, 0.3 * x, 0.1 * x);
                        if (x == 1.0F) poseStack.mulPose(Axis.YP.rotationDegrees(n / 20.0F));

                        self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm);
                    }
                        case BUNDLE -> {
                            poseStack.translate(1 * l, 0.0F - equipProgress * 0.3, 0.3);
                            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * l));
                            poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * l));
                        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                        altSwing(poseStack, arm, swingProgress);
                        poseStack.scale(0.9F, 0.9F, 0.9F);
                        self.hmi$renderPlayerArm(poseStack, collector, light, 0.0F, 0.0F, arm);
                    }
                    default -> {
                    }
                }
            } else if (isRiptide(player, item)) {
                this.riptideCounter = (float) (this.riptideCounter + 0.15 * tt);
                float m = (float) item.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
                float f = m / 10.0F;
                if (f > 1.0F) f = 1.0F;

                if (f > 0.1F) {
                    float g = Mth.sin((m - 0.1F) * 1.3F);
                    float h = f - 0.1F;
                    float j = g * h;
                    poseStack.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                }

                poseStack.mulPose(Axis.XP.rotationDegrees(45.0F - this.riptideCounter * 2.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(25 * l));
                poseStack.translate(0.2 * l, 0.0F, 0.75F);
                poseStack.translate(0.0F, 0.0F, 0.01F * Mth.sin(this.riptideCounter * 6.28F));
                self.hmi$renderPlayerArm(poseStack, collector, light, equipProgress, swingProgress, arm);
                poseStack.mulPose(Axis.XP.rotationDegrees(135.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-65 * l));
                poseStack.translate(0.65F * l, -1.0F, -0.6);
            } else {
                this.riptideCounter = 0.0F;

                if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN) && !item.is(ItemTags.HANGING_SIGNS)) {
                    if (useAnimation(item) == ItemUseAnimation.BLOCK) {
                        poseStack.translate(0.0F, -0.2, 0.0F);
                    }
                } else {
                    poseStack.translate(0.1F * l, 0.0F, -0.1);
                    poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
                }

                poseStack.translate(1 * l, 0.0F - equipProgress * 0.3, 0.3);
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * l));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * l));
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                altSwing(poseStack, arm, swingProgress);
                poseStack.scale(0.9F, 0.9F, 0.9F);
                self.hmi$renderPlayerArm(poseStack, collector, light, 0.0F, 0.0F, arm);
            }

            poseStack.translate(-0.3 * l, 0.65, -0.1);
            poseStack.mulPose(Axis.YP.rotationDegrees(-65 * l));
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            if (item.is(ItemTags.WOOL_CARPETS)) {
                poseStack.translate(0.2 * l, -0.1, 0.0F);
            }

            boolean isBlockLike = item.getItem() instanceof BlockItem && !isBucket(item)
                    && useAnimation(item) != ItemUseAnimation.EAT;

            if (isBlockLike) {
                if (item.getHoverName().getString().toLowerCase(java.util.Locale.ROOT).contains("torch")) {
                    poseStack.scale(1.5F, 1.5F, 1.5F);
                    poseStack.mulPose(Axis.YN.rotationDegrees(25 * l));
                    poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(75 * l));
                    poseStack.translate(0.2 * l, 0.2, 0.05);
                } else if ((item.is(Items.STRING) || item.is(Items.REDSTONE) || item.is(Items.LEVER)
                        || item.is(Items.TRIPWIRE_HOOK) || isGlassPane(item)
                        || blockState(item).is(BlockTags.RAILS) || blockState(item).is(BlockTags.CLIMBABLE)
                        || item.is(ItemTags.WOODEN_DOORS)) && !blockState(item).is(BlockTags.LEAVES)
                        && !blockState(item).is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)
                        && !blockState(item).is(BlockTags.BANNERS)) {
                    poseStack.translate(0.0F, 0.0F, -0.1);
                    poseStack.mulPose(Axis.YN.rotationDegrees(5 * l));
                    poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(75 * l));
                } else if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)
                        && !item.is(ItemTags.HANGING_SIGNS)) {
                    poseStack.mulPose(Axis.YN.rotationDegrees(25 * l));
                    poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(75 * l));
                    poseStack.translate(0.2 * l, 0.2, 0.05);
                    if (blockState(item).is(BlockTags.BANNERS)) {
                        poseStack.translate(-0.2 * l, 0.0F, 0.0F);
                        poseStack.scale(1.1F, 1.1F, 1.1F);
                    }
                } else {
                    float yawDelta = player.getYRot() - player.getYHeadRot();
                    float pitchDelta = player.getXRot() - player.getXRot();
                    this.swingVelocityY += yawDelta * 0.015F * (float) tt;
                    this.swingVelocityY += swingProgress * 2.0F * (float) tt;
                    this.swingVelocityX += pitchDelta * 0.015F * (float) tt;
                    this.swingVelocityY -= 0.1F * this.swingAngleY * (float) tt;
                    this.swingVelocityX -= 0.1F * this.swingAngleX * (float) tt;
                    this.swingVelocityY = (float) (this.swingVelocityY * Math.pow(0.88F, tt));
                    this.swingVelocityX = (float) (this.swingVelocityX * Math.pow(0.88F, tt));
                    this.swingAngleY += this.swingVelocityY * (float) tt;
                    this.swingAngleX += this.swingVelocityX * (float) tt;
                    double currentSpeed = velocity.length();
                    this.swingVelocityZ = (float) (this.swingVelocityZ + (bl ? currentSpeed * -1.0F * 15.0F
                            - this.swingVelocityZ : currentSpeed * 15.0F - this.swingVelocityZ) * 0.1F * tt);
                    if ((currentSpeed > 0.09 && player.onGround() || player.isSwimming()
                            || player.onClimbable() && !player.onGround()) && mc.options.bobView().get()) {
                        this.swingVelocityY += (float) ((RANDOM.nextBoolean() ? -5.5F : 5.5F) * currentSpeed * tt);
                    }

                    poseStack.translate(0.0F, 0.0F, -0.1);
                    poseStack.mulPose(Axis.YN.rotationDegrees(35 * l + this.swingAngleY));
                    poseStack.mulPose(Axis.XP.rotationDegrees(15.0F + this.swingAngleX));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(75 * l + this.swingVelocityZ));
                    if (item.is(ItemTags.HANGING_SIGNS)) {
                        poseStack.translate(0.0F, -0.1, 0.0F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(-45 * l));
                    }

                    poseStack.translate(0.3 * l, -0.35, 0.0F);
                    poseStack.translate(0.0F, 0.0F, 0.1);
                    poseStack.scale(1.5F, 1.5F, 1.5F);
                }
            } else {
                if ((!isTool(item) || item.is(ItemTags.TRIMMABLE_ARMOR) || item.is(ItemTags.BOOKSHELF_BOOKS)
                        || useAnimation(item) == ItemUseAnimation.EAT || !item.isEnchantable())
                        && useAnimation(item) != ItemUseAnimation.BOW && useAnimation(item) != ItemUseAnimation.SPYGLASS
                        && getAttackDamage(item) == 0.0F && useAnimation(item) != ItemUseAnimation.BLOCK
                        && !item.is(Items.WARPED_FUNGUS_ON_A_STICK) && !item.is(Items.CARROT_ON_A_STICK)
                        && !item.is(Items.FISHING_ROD) && !item.is(Items.SHEARS) && !mb3DCompat.getValue()) {
                    if (useAnimation(item) == ItemUseAnimation.BRUSH) {
                        poseStack.mulPose(Axis.XN.rotationDegrees(25.0F));
                        poseStack.translate(bl ? 0.0F : 0.35, bl ? 0.0F : 0.25F, bl ? 0.0F : 0.37);
                        if (!bl) {
                            poseStack.scale(0.75F, 0.75F, 0.75F);
                        }

                        poseStack.mulPose(Axis.ZN.rotationDegrees(-75 * l));
                        poseStack.mulPose(Axis.XN.rotationDegrees(35.0F));
                        poseStack.translate(bl ? -0.05 : 0.85, bl ? 0.0F : 0.05, bl ? 0.08 : -0.2);
                    } else {
                        poseStack.mulPose(Axis.YN.rotationDegrees(5 * l));
                        poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(75 * l));
                        poseStack.translate(0.0F, -0.05, -0.1);
                        poseStack.scale(0.7F, 0.7F, 0.7F);
                    }

                    if (item.is(Items.FEATHER) || item.is(Items.SLIME_BALL) || item.is(Items.PUFFERFISH)) {
                        squashSlime(poseStack, player, swingProgress, speed, tt);
                    }
                } else if (useAnimation(item) == ItemUseAnimation.BLOCK
                        && useAnimation(item) != ItemUseAnimation.SPEAR) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(160 * l));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-60 * l));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F));
                    poseStack.scale(0.75F, 0.75F, 0.75F);
                    poseStack.translate(0.15 * l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                    poseStack.translate(0.17 * l, 0.0F, 0.3);
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90 * l));
                } else if (useAnimation(item) == ItemUseAnimation.SPEAR) {
                    poseStack.mulPose(Axis.YN.rotationDegrees(75 * l));
                    poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(45 * l));
                    poseStack.translate(-0.3F * l, 0.0F, 0.0F);
                } else {
                    poseStack.mulPose(Axis.YN.rotationDegrees(75 * l));
                    poseStack.mulPose(Axis.XP.rotationDegrees(70.0F));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(45 * l));
                }

                if (useAnimation(item) != ItemUseAnimation.BLOCK) {
                    poseStack.scale(1.2F, 1.2F, 1.2F);
                }

                if (useAnimation(item) == ItemUseAnimation.BOW && !player.isUsingItem()) {
                    poseStack.translate(-0.1 * l, -0.2, 0.0F);
                }

                if (item.is(Items.MACE)) {
                    if (mb3DCompat.getValue()) {
                        poseStack.translate(-0.08, 0.17, 0.0F);
                        poseStack.mulPose(Axis.XP.rotationDegrees(40.0F));
                    }

                    poseStack.translate(0.1 * l, 0.0F, 0.0F);
                    poseStack.scale(0.9F, 0.9F, 0.9F);
                }
            }

            if (item.getItem() instanceof BlockItem && !isBucket(item) && useAnimation(item) != ItemUseAnimation.EAT
                    && !item.is(ItemTags.BANNERS) && !item.is(Items.STRING) && !item.is(Items.REDSTONE)
                    && !item.is(Items.LEVER) && !item.is(Items.TRIPWIRE_HOOK) && !isGlassPane(item)
                    && !blockState(item).is(BlockTags.RAILS) && !blockState(item).is(BlockTags.CLIMBABLE)
                    && !item.is(ItemTags.WOODEN_DOORS) || blockState(item).is(BlockTags.LEAVES)
                    && !blockState(item).is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                poseStack.pushPose();
                if (!bl2) {
                    poseStack.translate(-0.4F, 0.0F, 0.0F);
                }

                poseStack.scale(0.4F, 0.4F, 0.4F);
                poseStack.translate(-0.9 * l, -0.45, -0.5F);
                if (blockState(item).is(BlockTags.BUTTONS)) {
                    poseStack.translate(0.2 * l, -0.15, -0.2);
                }

                if (blockState(item).is(BlockTags.PRESSURE_PLATES)) {
                    poseStack.translate(0.0F, 0.1, 0.0F);
                }

                if (item.is(Items.SLIME_BLOCK) || item.is(Items.HONEY_BLOCK) || blockState(item).is(BlockTags.FLOWERS)
                        || blockState(item).is(BlockTags.LEAVES) || blockState(item).getBlock() instanceof SaplingBlock
                        || blockState(item).is(BlockTags.SWORD_EFFICIENT)) {
                    squashSlime(poseStack, player, swingProgress, speed, tt);
                }

                if (item.is(ItemTags.BEDS)) {
                    if (bl) {
                        poseStack.translate(0.9, 0.0F, 0.8);
                    }

                    poseStack.mulPose(Axis.YP.rotationDegrees(90 * l));
                }

                renderer.renderItem(player, item, bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, collector, light);
                poseStack.popPose();
            } else {
                if (isTool(item) && !item.is(ItemTags.TRIMMABLE_ARMOR) && !item.is(ItemTags.BOOKSHELF_BOOKS)
                        && useAnimation(item) != ItemUseAnimation.EAT && item.isEnchantable()
                        || useAnimation(item) == ItemUseAnimation.BOW || useAnimation(item) == ItemUseAnimation.SPYGLASS
                        || getAttackDamage(item) != 0.0F || useAnimation(item) == ItemUseAnimation.BLOCK
                        || item.is(Items.WARPED_FUNGUS_ON_A_STICK) || item.is(Items.CARROT_ON_A_STICK)
                        || item.is(Items.FISHING_ROD) || item.is(Items.SHEARS)) {
                    if (item.is(ItemTags.SWORDS) && !sharpSword) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-60.0F * swing));
                        poseStack.translate(0.0F, 0.1 * swing, -0.1 * swing);
                    }

                    if (item.is(ItemTags.SHOVELS)) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F * swingRot));
                        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
                    } else if (useAnimation(item) == ItemUseAnimation.SPEAR) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-40.0F * swingRot));
                        poseStack.translate(0.0F, 0.1 * swingRot, -0.1 * swingRot);
                    } else if (useAnimation(item) != ItemUseAnimation.BLOCK) {
                        poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F * swing));
                        poseStack.translate(0.0F, 0.05 * swing, -0.05 * swing);
                    }
                }

                if (!item.is(Items.NETHER_STAR) && (!item.is(Items.END_CRYSTAL) || !mb3DCompat.getValue())) {
                    this.netherCounter = 0.0F;
                } else {
                    this.netherCounter = (float) (this.netherCounter + 0.9 * tt);
                    poseStack.translate(0.0F, 0.25F + 0.02F * Mth.sin(this.netherCounter * 0.1F), 0.0F);
                    poseStack.mulPose(Axis.XP.rotationDegrees(3.0F * Mth.sin(this.netherCounter * 0.2F)));
                    poseStack.scale(1.0F + 0.01F * Mth.sin(this.netherCounter), 1.0F + 0.01F * Mth.sin(this.netherCounter),
                            1.0F + 0.01F * Mth.sin(this.netherCounter));
                }

                if (mb3DCompat.getValue()) {
                    if (item.is(ItemTags.SWORDS)) {
                        poseStack.translate(0.0F, 0.2, 0.0F);
                    }

                    if (item.is(Items.FEATHER) || item.is(Items.SLIME_BALL) || item.is(Items.PUFFERFISH)) {
                        squashSlime(poseStack, player, swingProgress, speed, tt);
                    }
                }

                if (item.is(ItemTags.SHOVELS)) {
                    poseStack.translate(0.07 * l, 0.0F, 0.05);
                    poseStack.mulPose(Axis.YP.rotationDegrees(90 * l));
                    poseStack.mulPose(Axis.XP.rotationDegrees(-15.0F));
                }

                renderItem(renderer, player, item, bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, collector, light);
            }
        }

        poseStack.popPose();
        poseStack.popPose();
        this.isAttacking = mc.options.keyAttack.isDown();
    }

    private boolean isSharpAnimation() {
        return animationType.is("Шарп");
    }

    private float easeInOutBack(float x) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        return x < 0.5F
                ? (float) (Math.pow(2.0 * x, 2.0) * ((c2 + 1.0F) * 2.0F * x - c2) / 2.0)
                : (float) ((Math.pow(2.0 * x - 2.0, 2.0) * ((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0) / 2.0);
    }

    private float getAttackDamage(ItemStack stack) {
        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return 0.0F;

        float totalDamage = 0.0F;
        for (var entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                totalDamage += (float) entry.modifier().amount();
            }
        }
        return totalDamage;
    }

    private void altSwing(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * 3.14F);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * 0.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
    }

    private void renderItem(ItemInHandRenderer renderer, AbstractClientPlayer player, ItemStack stack,
                            ItemDisplayContext context, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (!stack.isEmpty()) {
            renderer.renderItem(player, stack, context, poseStack, collector, light);
        }
    }

    private void applyEquipOffset(PoseStack poseStack, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }

    private void swingArm(float swingProgress, PoseStack poseStack, int armX, float equipProgress, HumanoidArm arm) {
        float f = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float g = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
        float h = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
        poseStack.translate(armX * f, g, h);
        applyEquipOffset(poseStack, arm, equipProgress);
        applySwingOffset(poseStack, arm, swingProgress);
    }

    private void applySwingOffset(PoseStack poseStack, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
    }

    private void squashSlime(PoseStack poseStack, AbstractClientPlayer player, float swingProgress, float speed, double tt) {
        this.vertVelocityYSlime = (float) (this.vertVelocityYSlime + swingProgress * 0.03 * (float) tt);
        if ((speed > 0.09F && player.onGround() || player.isSwimming() || player.isVisuallyCrawling()
                || player.onClimbable() && !player.onGround()) && mc.options.bobView().get()) {
            this.vertVelocityYSlime += (float) (-0.05 * speed * (float) tt);
        }

        poseStack.scale(1.0F, 1.0F + this.vertAngleYSlime * -2.0F, 1.0F);
    }

    private static ItemUseAnimation useAnimation(ItemStack stack) {
        return stack.getItem().getUseAnimation(stack);
    }

    private static boolean isTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem
                || item instanceof ShearsItem || item instanceof FishingRodItem || item instanceof MaceItem
                || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES);
    }

    private static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET)
                || stack.is(Items.MILK_BUCKET) || stack.is(Items.POWDER_SNOW_BUCKET)
                || stack.is(Items.COD_BUCKET) || stack.is(Items.SALMON_BUCKET) || stack.is(Items.TROPICAL_FISH_BUCKET)
                || stack.is(Items.PUFFERFISH_BUCKET) || stack.is(Items.AXOLOTL_BUCKET) || stack.is(Items.TADPOLE_BUCKET);
    }

    private static boolean isGlassPane(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == Blocks.GLASS_PANE;
    }

    private static net.minecraft.world.level.block.state.BlockState blockState(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState()
                : Blocks.AIR.defaultBlockState();
    }

    private static boolean isRiptide(AbstractClientPlayer player, ItemStack stack) {
        if (!player.isUsingItem() || !stack.is(Items.TRIDENT)) return false;
        int remaining = player.getUseItemRemainingTicks();
        return remaining > 0 && (72000 - remaining) >= TridentItem.THROW_THRESHOLD_TIME;
    }
}
