package error.module.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.math.anim.Easings;

/**
 * Create by daun kvass
 */
public final class SwingAnimation extends Module {

    public static SwingAnimation INSTANCE;

    public final ModeSetting mode = mode(
            "Режим",
            "Smooth",
            "Smooth",
            "Block",
            "BlockHit",
            "Pander",
            "StraightDown",
            "Akrien",
            "Touch",
            "Self",
            "Self2"
    );

    public final ModeSetting easing = mode("Интерполяция", "QuadInOut", Easings.NAMES);

    public final SliderSetting strength = slider("Сила", 1.0F, 0.1F, 3.0F, 0.05F);
    public final SliderSetting speed = slider("Скорость", 1.0F, 0.2F, 3.0F, 0.05F);
    public final CheckBox offhand = checkbox("Левая рука", true);
    public final CheckBox noReequip = checkbox("Не опускать руку", true);
    public final CheckBox noEat = checkbox("Не анимировать еду", false);

    private long rightSwingTime = 0L;
    private long leftSwingTime = 0L;
    private float prevRightAttack = 0.0F;
    private float prevLeftAttack = 0.0F;

    public SwingAnimation() {
        super("SwingAnimation", "Кастомные анимации руки", Category.RENDER);
        INSTANCE = this;
    }

    public void applySwing(float rawAttack, PoseStack poseStack, int invert, HumanoidArm arm) {
        boolean isRight = arm == HumanoidArm.RIGHT;
        long now = System.currentTimeMillis();

        float prevAttack = isRight ? this.prevRightAttack : this.prevLeftAttack;

        if (rawAttack > 0.0F && (prevAttack == 0.0F || rawAttack < prevAttack * 0.6F)) {
            if (isRight) {
                this.rightSwingTime = now;
            } else {
                this.leftSwingTime = now;
            }
        }

        if (isRight) {
            this.prevRightAttack = rawAttack;
        } else {
            this.prevLeftAttack = rawAttack;
        }

        long startTime = isRight ? this.rightSwingTime : this.leftSwingTime;
        float factor = this.strength.getValue();
        float animSpeed = Math.max(0.1F, this.speed.getValue());

        long duration = (long) (300.0F / animSpeed);
        long elapsed = now - startTime;

        float wave = 0.0F;
        if (startTime > 0L && elapsed >= 0L && elapsed < duration) {
            float linearProgress = (float) elapsed / (float) duration;
            if (linearProgress < 0.5F) {
                float half = linearProgress * 2.0F;
                wave = Easings.ease(this.easing.getValue(), half);
            } else {
                float half = (1.0F - linearProgress) * 2.0F;
                wave = Easings.ease(this.easing.getValue(), half);
            }
            wave = Mth.clamp(wave, 0.0F, 1.0F);
        }

        switch (this.mode.getValue()) {
            case "Smooth" -> {
                float swingPower = factor * 45.0F;
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * (45.0F + wave * (-swingPower / 4.0F))));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) invert * (wave * -(swingPower / 4.0F))));
                poseStack.mulPose(Axis.XP.rotationDegrees(wave * -swingPower));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * -45.0F));
            }

            case "Akrien" -> {
                if (wave > 0.001F) {
                    poseStack.translate(0.0F, 0.02F, 0.02F);
                    poseStack.mulPose(Axis.YP.rotationDegrees((float) (45.0F * invert)));
                    poseStack.mulPose(Axis.XP.rotationDegrees(wave * -85.0F * factor));
                    poseStack.translate((float) invert * -0.1F, 0.28F, 0.2F);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-85.0F));
                }
            }

            case "Touch" -> {
                if (wave > 0.001F) {
                    poseStack.scale(1.0F, 1.0F, 1.0F + (wave * factor / 4.0F));
                    poseStack.translate(0.0F, 0.0F, -0.265F * wave * factor);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F * wave));
                }
            }

            case "Block" -> {
                poseStack.mulPose(Axis.XP.rotationDegrees((float) (invert * -45.0F)));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) (invert * 45.0F) + (90.0F * factor * wave)));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * -90.0F));
            }

            case "BlockHit" -> {
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * 45.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(wave * -20.0F * factor));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) invert * wave * -20.0F * factor));
                poseStack.mulPose(Axis.XP.rotationDegrees(wave * -80.0F * factor));

                poseStack.translate((float) invert * 0.4F * factor * wave, 0.2F * factor * wave, 0.2F * factor * wave);
                poseStack.translate((float) invert * -0.5F, 0.08F, 0.0F);

                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * 20.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * 20.0F));
            }

            case "Pander" -> {
                poseStack.scale(0.8F, 0.8F, 0.8F);
                poseStack.translate(
                        (float) invert * (0.3F - wave * 0.15F * factor),
                        0.2F,
                        -0.15F - wave * 0.13F * factor
                );
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * (76.0F - (10.0F * wave * factor))));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) invert * (-16.0F - (8.0F * wave * factor))));
                poseStack.mulPose(Axis.XP.rotationDegrees(-83.0F - (26.0F * wave * factor)));
            }

            case "StraightDown" -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(wave * -125.0F * factor));
            }

            case "Self" -> {
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * 90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) invert * -30.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F - (45.0F * factor * wave)));
            }

            case "Self2" -> {
                poseStack.mulPose(Axis.YP.rotationDegrees((float) invert * 90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees((float) invert * -60.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-45.0F - (50.0F * factor * wave)));
            }
        }
    }
}