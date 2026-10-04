package error.util.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public final class DemonModel {

    public static final Identifier RED_TEXTURE = Identifier.fromNamespaceAndPath("error", "models/skycore/reddemon.png");
    public static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("error", "models/skycore/whitedemon.png");
    private static final Map<Integer, DemonModel> MODELS = new HashMap<>();

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    private DemonModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition data = mesh.getRoot();

        PartDefinition demonRoot = data.addOrReplaceChild("demon_root", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition h = demonRoot.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.3F)),
                PartPose.offset(0.0F, -6.0F, -1.0F));

        h.addOrReplaceChild("left_horn",
                CubeListBuilder.create()
                        .texOffs(32, 8).addBox(13.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.1F))
                        .texOffs(0, 0).addBox(17.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(-8.0F, 8.0F, 0.0F, -0.3927F, 0.3927F, -0.5236F));

        h.addOrReplaceChild("right_horn",
                CubeListBuilder.create().mirror()
                        .texOffs(32, 8).addBox(-19.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.1F))
                        .texOffs(0, 0).addBox(-19.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(8.0F, 8.0F, 0.0F, -0.3927F, -0.3927F, 0.5236F));

        PartDefinition b = demonRoot.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -1.7028F, 1.4696F, 8.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(0.5F, -0.1F, -3.5F, 0.1745F, 0.0F, 0.0F));

        b.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(40, 12).addBox(-7.0072F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
                PartPose.offsetAndRotation(8.25F, -2.0F, 10.0F, 0.0873F, -0.829F, 0.1745F));

        b.addOrReplaceChild("right_wing",
                CubeListBuilder.create().mirror().texOffs(40, 12).addBox(-4.9928F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
                PartPose.offsetAndRotation(-9.25F, -2.0F, 10.0F, 0.0873F, 0.829F, -0.1745F));

        demonRoot.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(24, 16).addBox(-1.1F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offsetAndRotation(5.4F, -1.25F, -2.0F, 0.0F, 0.0F, -0.2182F));

        demonRoot.addOrReplaceChild("right_arm",
                CubeListBuilder.create().mirror().texOffs(24, 16).addBox(-2.9F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offsetAndRotation(-5.4F, -1.25F, -2.0F, 0.0F, 0.0F, 0.2182F));

        PartDefinition legL = demonRoot.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(48, 22).addBox(-3.25F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(3.0F, 10.0F, 0.0F));

        PartDefinition legL1 = legL.addOrReplaceChild("left_leg_1",
                CubeListBuilder.create().texOffs(34, 34).addBox(0.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(-1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));

        legL1.addOrReplaceChild("left_foot",
                CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-0.7F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F)
                        .texOffs(40, 0).addBox(-0.7F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));

        PartDefinition clawRootL = legL1.addOrReplaceChild("left_claw_root",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0F, 0.0F, -2.0F, 0.0F, -0.0873F, -0.2618F));

        clawRootL.addOrReplaceChild("left_claw",
                CubeListBuilder.create()
                        .texOffs(16, 34).addBox(-0.7911F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
                        .texOffs(0, 32).addBox(-0.7911F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(1.9F, 12.0F, 0.25F));

        PartDefinition legR = demonRoot.addOrReplaceChild("right_leg",
                CubeListBuilder.create().mirror().texOffs(48, 22).addBox(-0.75F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(-3.0F, 10.0F, 0.0F));

        PartDefinition legR1 = legR.addOrReplaceChild("right_leg_1",
                CubeListBuilder.create().mirror().texOffs(34, 34).addBox(-3.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));

        legR1.addOrReplaceChild("right_foot",
                CubeListBuilder.create().mirror()
                        .texOffs(26, 0).addBox(-3.3F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F)
                        .texOffs(40, 0).addBox(-3.3F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));

        PartDefinition clawRootR = legR1.addOrReplaceChild("right_claw_root",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.0F, 0.0F, -2.0F, 0.0F, 0.0873F, 0.2618F));

        clawRootR.addOrReplaceChild("right_claw",
                CubeListBuilder.create().mirror()
                        .texOffs(16, 34).addBox(-3.2089F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
                        .texOffs(0, 32).addBox(-3.2089F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(-1.9F, 12.0F, 0.25F));

        this.root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        ModelPart dRoot = this.root.getChild("demon_root");
        this.head = dRoot.getChild("head");
        this.body = dRoot.getChild("body");
        this.leftArm = dRoot.getChild("left_arm");
        this.rightArm = dRoot.getChild("right_arm");
        this.leftLeg = dRoot.getChild("left_leg");
        this.rightLeg = dRoot.getChild("right_leg");
    }

    public static DemonModel of(int entityId) {
        if (MODELS.size() > 128) {
            MODELS.clear();
        }
        return MODELS.computeIfAbsent(entityId, ignored -> new DemonModel());
    }

    public void submit(PlayerModel vanilla, AvatarRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, int light, int overlay, int color, int outlineColor, Identifier texture) {
        vanilla.setupAnim(state);
        copyAngles(vanilla);

        pose.pushPose();
        pose.scale(1.0F, 1.0F, 1.0F);
        pose.translate(0.0F, 0.0F, 0.0F);
        RenderType layer = RenderTypes.entityTranslucent(texture != null ? texture : RED_TEXTURE);
        collector.submitModelPart(this.root, pose, layer, light, overlay, (TextureAtlasSprite) null, color,
                (ModelFeatureRenderer.CrumblingOverlay) null, outlineColor);
        pose.popPose();
    }

    private void copyAngles(PlayerModel vanilla) {
        this.head.xRot = vanilla.head.xRot;
        this.head.yRot = vanilla.head.yRot;
        this.head.zRot = vanilla.head.zRot;

        this.leftArm.xRot = vanilla.leftArm.xRot;
        this.leftArm.yRot = vanilla.leftArm.yRot;
        this.leftArm.zRot = vanilla.leftArm.zRot;

        this.rightArm.xRot = vanilla.rightArm.xRot;
        this.rightArm.yRot = vanilla.rightArm.yRot;
        this.rightArm.zRot = vanilla.rightArm.zRot;

        this.leftLeg.xRot = vanilla.leftLeg.xRot;
        this.leftLeg.yRot = vanilla.leftLeg.yRot;
        this.leftLeg.zRot = vanilla.leftLeg.zRot;

        this.rightLeg.xRot = vanilla.rightLeg.xRot;
        this.rightLeg.yRot = vanilla.rightLeg.yRot;
        this.rightLeg.zRot = vanilla.rightLeg.zRot;

        this.body.xRot = 0.1745F + vanilla.body.xRot * 0.35F;
        this.body.yRot = vanilla.body.yRot * 0.1F;
    }
}
