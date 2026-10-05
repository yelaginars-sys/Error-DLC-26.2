package error.cosmetic.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import error.cosmetic.CosmeticItem;
import error.cosmetic.CosmeticType;
import error.cosmetic.CosmeticsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CosmeticRenderer {

    private static final Map<String, GeoModel> MODEL_CACHE = new HashMap<>();

    private CosmeticRenderer() {
    }

    public static void renderCosmetics(SubmitNodeCollector collector, PoseStack pose,
                                        AvatarRenderState avatarState, PlayerModel playerModel,
                                        int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        // Render for local player (both in-world and in ClickGUI preview)
        boolean isLocal = (mc.player == null || avatarState.id == mc.player.getId() || avatarState.id <= 0);
        if (!isLocal) return;

        List<CosmeticItem> equipped = CosmeticsManager.getInstance().getEquippedCosmetics();
        if (equipped.isEmpty()) return;

        for (CosmeticItem item : equipped) {
            if (item.getType() == CosmeticType.MODEL) {
                // Models handled by CustomModels
                continue;
            }

            GeoModel model = getOrLoadModel(item);
            if (model == null) continue;

            Identifier texture = getTextureIdentifier(item);
            if (texture == null) continue;

            pose.pushPose();
            try {
                switch (item.getType()) {
                    case WINGS -> {
                        // Attach to torso/body
                        playerModel.body.translateAndRotate(pose);
                        pose.translate(0.0F, 0.0F, 0.12F);
                        pose.scale(1.0F, -1.0F, 1.0F);
                        pose.translate(0.0F, -24.0F / 16.0F, 0.0F);

                        float flap = (float) Math.sin((System.currentTimeMillis() + avatarState.id * 120) * 0.0055F) * 0.35F;
                        renderWingModel(model, pose, collector, texture, light, overlay, flap);
                    }
                    case HAT -> {
                        // Attach to head (top)
                        playerModel.head.translateAndRotate(pose);
                        pose.scale(1.06F, -1.06F, 1.06F);
                        pose.translate(0.0F, -24.0F / 16.0F, 0.0F);
                        renderGeoModel(model, pose, collector, texture, light, overlay);
                    }
                    case MASK -> {
                        // Attach to head (front face)
                        playerModel.head.translateAndRotate(pose);
                        pose.scale(1.05F, -1.05F, 1.05F);
                        pose.translate(0.0F, -24.0F / 16.0F, -0.05F);
                        renderGeoModel(model, pose, collector, texture, light, overlay);
                    }
                    case BACKPACK -> {
                        // Attach to back
                        playerModel.body.translateAndRotate(pose);
                        pose.translate(0.0F, 0.0F, 0.16F);
                        pose.scale(1.0F, -1.0F, 1.0F);
                        pose.translate(0.0F, -24.0F / 16.0F, 0.0F);
                        renderGeoModel(model, pose, collector, texture, light, overlay);
                    }
                    case PET, CAR -> {
                        // Companion floating or driving near player
                        float bob = (float) Math.sin((System.currentTimeMillis() + avatarState.id * 50) * 0.003F) * 0.06F;
                        pose.translate(0.65F, 0.25F + bob, 0.20F);
                        pose.scale(0.65F, -0.65F, 0.65F);
                        renderGeoModel(model, pose, collector, texture, light, overlay);
                    }
                    default -> {}
                }
            } finally {
                pose.popPose();
            }
        }
    }

    private static GeoModel getOrLoadModel(CosmeticItem item) {
        String path = item.getTexturePath();
        if (path == null || path.isEmpty()) return null;

        if (MODEL_CACHE.containsKey(path)) {
            return MODEL_CACHE.get(path);
        }

        String resPath = "/assets/error/" + path + "/geometry.json";
        try (InputStream stream = CosmeticRenderer.class.getResourceAsStream(resPath)) {
            if (stream != null) {
                GeoModel model = GeoModelParser.parse(stream);
                if (model != null) {
                    MODEL_CACHE.put(path, model);
                    return model;
                }
            }
        } catch (Exception ignored) {}

        // Fallback: check without leading slash
        try (InputStream stream = CosmeticRenderer.class.getClassLoader().getResourceAsStream("assets/error/" + path + "/geometry.json")) {
            if (stream != null) {
                GeoModel model = GeoModelParser.parse(stream);
                if (model != null) {
                    MODEL_CACHE.put(path, model);
                    return model;
                }
            }
        } catch (Exception ignored) {}

        MODEL_CACHE.put(path, null);
        return null;
    }

    private static Identifier getTextureIdentifier(CosmeticItem item) {
        String path = item.getTexturePath();
        if (path == null || path.isEmpty()) return null;
        if (path.endsWith(".png")) {
            return Identifier.fromNamespaceAndPath("error", path);
        }
        return Identifier.fromNamespaceAndPath("error", path + "/texture.png");
    }

    private static void renderWingModel(GeoModel model, PoseStack pose, SubmitNodeCollector collector,
                                        Identifier texture, int light, int overlay, float flap) {
        for (GeoBone bone : model.topLevelBones) {
            renderBoneWithFlap(bone, pose, collector, texture, light, overlay, flap);
        }
    }

    private static void renderBoneWithFlap(GeoBone bone, PoseStack pose, SubmitNodeCollector collector,
                                           Identifier texture, int light, int overlay, float flap) {
        if (bone.isHidden) return;

        pose.pushPose();
        try {
            pose.translate(-bone.getPositionX() / 16.0F, bone.getPositionY() / 16.0F, bone.getPositionZ() / 16.0F);
            pose.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);

            if (bone.getRotationZ() != 0.0F) pose.mulPose(Axis.ZP.rotation(bone.getRotationZ()));
            if (bone.getRotationY() != 0.0F) pose.mulPose(Axis.YP.rotation(bone.getRotationY()));
            if (bone.getRotationX() != 0.0F) pose.mulPose(Axis.XP.rotation(bone.getRotationX()));

            // Flap animation on wing roots
            String lower = bone.getName().toLowerCase();
            if (lower.contains("left") || lower.contains("l_")) {
                pose.mulPose(Axis.YP.rotation(-flap));
            } else if (lower.contains("right") || lower.contains("r_")) {
                pose.mulPose(Axis.YP.rotation(flap));
            }

            pose.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
            pose.translate(-bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);

            renderCubes(bone, pose, collector, texture, light, overlay);

            for (GeoBone child : bone.childBones) {
                renderBoneWithFlap(child, pose, collector, texture, light, overlay, flap * 0.7F);
            }
        } finally {
            pose.popPose();
        }
    }

    private static void renderGeoModel(GeoModel model, PoseStack pose, SubmitNodeCollector collector,
                                       Identifier texture, int light, int overlay) {
        for (GeoBone bone : model.topLevelBones) {
            renderBone(bone, pose, collector, texture, light, overlay);
        }
    }

    private static void renderBone(GeoBone bone, PoseStack pose, SubmitNodeCollector collector,
                                   Identifier texture, int light, int overlay) {
        if (bone.isHidden) return;

        pose.pushPose();
        try {
            pose.translate(-bone.getPositionX() / 16.0F, bone.getPositionY() / 16.0F, bone.getPositionZ() / 16.0F);
            pose.translate(bone.getPivotX() / 16.0F, bone.getPivotY() / 16.0F, bone.getPivotZ() / 16.0F);

            if (bone.getRotationZ() != 0.0F) pose.mulPose(Axis.ZP.rotation(bone.getRotationZ()));
            if (bone.getRotationY() != 0.0F) pose.mulPose(Axis.YP.rotation(bone.getRotationY()));
            if (bone.getRotationX() != 0.0F) pose.mulPose(Axis.XP.rotation(bone.getRotationX()));

            pose.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
            pose.translate(-bone.getPivotX() / 16.0F, -bone.getPivotY() / 16.0F, -bone.getPivotZ() / 16.0F);

            renderCubes(bone, pose, collector, texture, light, overlay);

            for (GeoBone child : bone.childBones) {
                renderBone(child, pose, collector, texture, light, overlay);
            }
        } finally {
            pose.popPose();
        }
    }

    private static void renderCubes(GeoBone bone, PoseStack pose, SubmitNodeCollector collector,
                                    Identifier texture, int light, int overlay) {
        for (GeoCube cube : bone.childCubes) {
            pose.pushPose();
            try {
                pose.translate(cube.pivot.getX() / 16.0F, cube.pivot.getY() / 16.0F, cube.pivot.getZ() / 16.0F);
                if (cube.rotation.getZ() != 0.0F) pose.mulPose(Axis.ZP.rotation(cube.rotation.getZ()));
                if (cube.rotation.getY() != 0.0F) pose.mulPose(Axis.YP.rotation(cube.rotation.getY()));
                if (cube.rotation.getX() != 0.0F) pose.mulPose(Axis.XP.rotation(cube.rotation.getX()));
                pose.translate(-cube.pivot.getX() / 16.0F, -cube.pivot.getY() / 16.0F, -cube.pivot.getZ() / 16.0F);

                collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(texture), (matrix, consumer) -> {
                    for (GeoQuad quad : cube.quads) {
                        if (quad == null) continue;
                        renderQuad(matrix, consumer, quad, light, overlay);
                    }
                });
            } finally {
                pose.popPose();
            }
        }
    }

    private static void renderQuad(PoseStack.Pose poseEntry, VertexConsumer consumer, GeoQuad quad, int light, int overlay) {
        // Front face
        for (int i = 0; i < 4; i++) {
            GeoVertex v = quad.vertices[i];
            consumer.addVertex(poseEntry, v.position.x, v.position.y, v.position.z)
                    .setColor(0xFFFFFFFF)
                    .setUv(v.textureU, v.textureV)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(poseEntry, quad.normal.x, quad.normal.y, quad.normal.z);
        }
        // Back face (allows double-sided visibility for wings and inverted winding)
        for (int i = 3; i >= 0; i--) {
            GeoVertex v = quad.vertices[i];
            consumer.addVertex(poseEntry, v.position.x, v.position.y, v.position.z)
                    .setColor(0xFFFFFFFF)
                    .setUv(v.textureU, v.textureV)
                    .setOverlay(overlay)
                    .setLight(light)
                    .setNormal(poseEntry, -quad.normal.x, -quad.normal.y, -quad.normal.z);
        }
    }
}
