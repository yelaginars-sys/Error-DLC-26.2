package error.module.impl.movement;

import net.minecraft.util.Mth;
import error.event.EventTarget;
import error.event.list.EventFirework;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ElytraBooster extends Module {

    private final ModeSetting mode = mode("Mode", "Default", "Default", "ReallyWorld", "Custom");

    private final List<SliderSetting> customSliders = new ArrayList<>();

    public ElytraBooster() {
        super("ElytraBooster", "Ускоряет феер", Category.MOVEMENT);
        for (int i = 0; i < 45; i += 5) {SliderSetting s = slider("Yaw " + i + "-" + (i + 5), 1.5F, 1.5F, 2.3F, 0.01F).visible(()->mode.is("Custom"));customSliders.add(s);}
        for (int i = 0; i < 90; i += 5) {SliderSetting s = slider("Pitch " + i + "-" + (i + 5), 1.5F, 1.5F, 2.3F, 0.01F).visible(()->mode.is("Custom"));customSliders.add(s);}
        for (int i = 0; i < 45; i += 1) {SliderSetting s = slider("Yaw " + i + "-" + (i + 1), 1.5F, 1.5F, 2.3F, 0.01F).visible(()->mode.is("Custom"));customSliders.add(s);}
        for (int i = 0; i < 90; i += 1) {SliderSetting s = slider("Pitch " + i + "-" + (i + 1), 1.5F, 1.5F, 2.3F, 0.01F).visible(()->mode.is("Custom"));customSliders.add(s);}
    }

    @EventTarget
    public void onFirework(EventFirework event) {
        if (!inGame() || player() == null) return;
        if (event.getBoostedEntity() != player()) return;
        event.setSpeed(getSpeed() - 0.03F);
    }

    public float getSpeed() {
        if (mode.is("Custom")) {
            return getCustomSpeed();
        }

        float finalMultiplierXZ;
        float finalMultiplierY = 0;

        {
            float yaw = Mth.wrapDegrees(player().getYRot());
            float[] targetAngles = {45F, 135F, 225F, 315F};

            float minDistance = 180F;
            for (float targetAngle : targetAngles) {
                float distance = Math.abs(Mth.wrapDegrees(yaw - targetAngle));
                minDistance = Math.min(minDistance, distance);
            }

            float normalizedDistance = minDistance / 45.0F;
            float proximity = 1.0F - normalizedDistance;

            float power = 2.8F;
            float curve = (float) Math.pow(Math.max(0.0F, proximity), power);

            float minMultiplier = 0.13F;
            if (player().getXRot() <= -90F) minMultiplier = 0.126F;
            if (player().getXRot() <= -75F) minMultiplier = 0.123F;

            float maxMultiplier = 0.89F - Math.abs(player().getXRot()) / 90F;
            float speedMultiplier = minMultiplier + (maxMultiplier - minMultiplier) * curve;

            float yawDelta = Math.abs(Mth.wrapDegrees(player().getYRot() - player().yHeadRotO));
            if (yawDelta > 45F) speedMultiplier = 0F;

            if (mode.is("ReallyWorld")) {
                speedMultiplier = Mth.clamp(speedMultiplier, 0F, 0.34F);
            }
            finalMultiplierXZ = speedMultiplier;
        }

        {
            float pitch = player().getXRot();

            if (pitch >= -75F) {
                float distance = Math.abs(pitch - (-45F));
                float normalizedDistance = Math.min(distance / 45.0F, 1.0F);
                float proximity = 1.0F - normalizedDistance;
                proximity = Math.max(0.0F, Math.min(1.0F, proximity));

                float curve = (float) Math.pow(proximity, 1.4F);
                if (pitch <= -50F) curve = (float) Math.pow(proximity, 1.5F);
                if (pitch <= -61F) curve = (float) Math.pow(proximity, 1.9F);

                float speedMultiplier = 0.79F * curve;
                finalMultiplierY = speedMultiplier;
            }
        }

        return 1.5F + Math.max(finalMultiplierXZ, finalMultiplierY);
    }

    public float getCustomSpeed() {
        float yaw = Mth.wrapDegrees(player().getYRot());
        float pitch = player().getXRot();

        float yawMultiplier = 1.5F;
        float pitchMultiplier = 1.5F;

        float yawDistance = 45F;

        float[] centers = {45F, 135F, 225F, 315F};
        float bestYawDistance = Float.MAX_VALUE;

        for (float center : centers) {
            float delta = Math.abs(Mth.wrapDegrees(yaw - center));
            if (delta > 45F) continue;

            float value = Mth.clamp(45F - delta - 0.001F, 0F, 45F);
            float bandValue = getBandValue("Yaw", value);

            if (delta < bestYawDistance) {
                bestYawDistance = delta;
                yawDistance = value;
                yawMultiplier = bandValue;
            }
        }

        float absPitch = Mth.clamp(Math.abs(pitch), 0F, 90F);
        pitchMultiplier = getBandValue("Pitch", absPitch);

        int yawBand = getBandStart("Yaw", yawDistance);
        int pitchBand = getBandStart("Pitch", absPitch);

        if (pitchBand > yawBand) return pitchMultiplier;
        if (yawBand > pitchBand) return yawMultiplier;

        return Math.max(yawMultiplier, pitchMultiplier);
    }

    private int getBandStart(String prefix, float distance) {
        for (SliderSetting slider : customSliders) {
            String name = slider.getName();
            if (!name.startsWith(prefix + " ")) continue;

            String range = name.substring((prefix + " ").length());
            String[] parts = range.split("-");
            if (parts.length != 2) continue;

            try {
                float from = Float.parseFloat(parts[0]);
                float to = Float.parseFloat(parts[1]);

                if (distance >= from && distance <= to) {
                    return (int) from;
                }
            } catch (NumberFormatException ignored) {}
        }

        return -1;
    }

    private float getBandValue(String prefix, float distance) {
        for (SliderSetting slider : customSliders) {
            String name = slider.getName();
            if (!name.startsWith(prefix + " ")) continue;

            String range = name.substring((prefix + " ").length());
            String[] parts = range.split("-");
            if (parts.length != 2) continue;

            try {
                float from = Float.parseFloat(parts[0]);
                float to = Float.parseFloat(parts[1]);

                if (distance >= from && distance <= to) {
                    return slider.getValue();
                }
            } catch (NumberFormatException ignored) {}
        }

        return 1.5F;
    }
}