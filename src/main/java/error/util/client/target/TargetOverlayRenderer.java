package error.util.client.target;

import com.mojang.blaze3d.vertex.PoseStack;
import error.module.impl.render.TargetEsp;
import error.util.client.Annstable;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.Render3DUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class TargetOverlayRenderer {

    private static final Identifier RHOMBUS_TEX = Identifier.fromNamespaceAndPath("client", "textures/visuals/rhombus.png");
    private static final Identifier MARKER_TEX = Identifier.fromNamespaceAndPath("client", "textures/visuals/marker.png");
    private static final Identifier GLOW_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/arrow_gps.png");

    private float spin = 0.0f;
    private float ringAngle = 0.0f;

    public void render(TargetEsp esp, PoseStack poseStack, float tickDelta) {
        if (esp == null || !esp.isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        LivingEntity target = esp.getTarget();
        if (target == null || !target.isAlive()) return;

        String mode = esp.mode.getValue();
        if ("Ромб".equals(mode) || "Кружок".equals(mode) || "Призраки".equals(mode)) {
            return;
        }

        float userSpeed = esp.rotSpeed.get();
        float userSize = esp.size.get();
        float userOpacity = esp.opacity.get();
        boolean colorOnHit = esp.colorOnHit.getValue();

        int baseColor = esp.getEspColor();
        int finalColor = colorOnHit ? Annstable.blend(baseColor, target, userOpacity) : ColorUtil.withAlpha(baseColor, (int) (ColorUtil.alpha(baseColor) * userOpacity));

        Vec3 pos = new Vec3(
                Mth.lerp(tickDelta, target.xOld, target.getX()),
                Mth.lerp(tickDelta, target.yOld, target.getY()) + target.getBbHeight() * 0.5,
                Mth.lerp(tickDelta, target.zOld, target.getZ())
        );

        Render3DUtil.ScreenPoint screen = Render3DUtil.projectToScreen(mc, pos);
        if (screen == null) return;

        float x = screen.x();
        float y = screen.y();

        this.spin += 2.0f * userSpeed;
        this.ringAngle += 3.0f * userSpeed;

        poseStack.pushPose();
        poseStack.translate(x, y, 0);

        switch (mode) {
            case "Картинка" -> {
                float imgSize = 48.0f * userSize;
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(spin));
                Render2D.drawTexture(RHOMBUS_TEX, -imgSize / 2f, -imgSize / 2f, imgSize, imgSize, finalColor);
            }
            case "Кольцо" -> {
                float ringSize = 42.0f * userSize;
                float pulsate = 1.0f + 0.12f * (float) Math.sin(Math.toRadians(spin * 2.0f));
                ringSize *= pulsate;
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(ringAngle));
                Render2D.drawRoundedOutline(-ringSize / 2f, -ringSize / 2f, ringSize, ringSize, ringSize / 2f, 2.5f, finalColor);
                Render2D.drawRoundedOutline(-ringSize / 2f + 4f, -ringSize / 2f + 4f, ringSize - 8f, ringSize - 8f, (ringSize - 8f) / 2f, 1.2f, ColorUtil.withAlpha(finalColor, 120));
            }
            case "Кубики" -> {
                int cubes = 4;
                float dist = 24.0f * userSize;
                float cubeSize = 8.0f * userSize;
                for (int i = 0; i < cubes; i++) {
                    double angle = Math.toRadians(spin + i * (360.0f / cubes));
                    float cx = (float) (Math.cos(angle) * dist);
                    float cy = (float) (Math.sin(angle) * dist);
                    Render2D.drawRoundedRect(cx - cubeSize / 2f, cy - cubeSize / 2f, cubeSize, cubeSize, 2.0f, finalColor);
                }
            }
            case "Кристаллы" -> {
                int crystals = 3;
                float dist = 26.0f * userSize;
                float crSize = 10.0f * userSize;
                for (int i = 0; i < crystals; i++) {
                    double angle = Math.toRadians(-spin * 1.5f + i * (360.0f / crystals));
                    float cx = (float) (Math.cos(angle) * dist);
                    float cy = (float) (Math.sin(angle) * dist);
                    poseStack.pushPose();
                    poseStack.translate(cx, cy, 0);
                    poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) Math.toDegrees(angle) + 45.0f));
                    Render2D.drawRoundedRect(-crSize / 2f, -crSize / 2f, crSize, crSize, 2.0f, finalColor);
                    poseStack.popPose();
                }
            }
            case "Молнии" -> {
                int spokes = 6;
                float len = 28.0f * userSize;
                for (int i = 0; i < spokes; i++) {
                    double angle = Math.toRadians(spin * 2.5f + i * (360.0f / spokes));
                    float ex = (float) (Math.cos(angle) * len);
                    float ey = (float) (Math.sin(angle) * len);
                    Render2D.drawRoundedRect(ex * 0.3f, ey * 0.3f, 2.0f, 2.0f, 1.0f, finalColor);
                    Render2D.drawRoundedRect(ex * 0.7f, ey * 0.7f, 2.5f, 2.5f, 1.0f, finalColor);
                    Render2D.drawRoundedRect(ex, ey, 3.0f, 3.0f, 1.5f, finalColor);
                }
            }
            case "Цепи" -> {
                int links = 8;
                float chainR = 25.0f * userSize;
                for (int i = 0; i < links; i++) {
                    double angle = Math.toRadians(spin + i * (360.0f / links));
                    float lx = (float) (Math.cos(angle) * chainR);
                    float ly = (float) (Math.sin(angle) * chainR);
                    Render2D.drawRoundedOutline(lx - 3f, ly - 3f, 6f, 6f, 3f, 1.5f, finalColor);
                }
            }
            default -> {}
        }

        poseStack.popPose();
    }

    public void reset() {
        spin = 0.0f;
        ringAngle = 0.0f;
    }
}
