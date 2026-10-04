package error.util.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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

public final class FreddyModel {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("error", "models/skycore/freddy.png");
    private static final Map<Integer, FreddyModel> MODELS = new HashMap<>();

    private final ModelPart root;
    private final ModelPart fredbody;
    private final ModelPart fredhead;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;

    private FreddyModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition data = mesh.getRoot();

        PartDefinition freddyRoot = data.addOrReplaceChild("freddy_root", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = freddyRoot.addOrReplaceChild("fredbody",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -14.0F, -1.0F, 2.0F, 24.0F, 2.0F),
                PartPose.offset(0.0F, -9.0F, 0.0F));

        body.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(8, 0).addBox(-6.0F, -9.0F, -4.0F, 12.0F, 18.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, (float) (Math.PI / 180.0), 0.0F, 0.0F));

        body.addOrReplaceChild("crotch",
                CubeListBuilder.create().texOffs(56, 0).addBox(-5.5F, 0.0F, -3.5F, 11.0F, 3.0F, 7.0F),
                PartPose.offset(0.0F, 9.5F, 0.0F));

        PartDefinition legR = body.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(90, 8).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-3.3F, 12.5F, 0.0F));
        legR.addOrReplaceChild("leg_right_pad",
                CubeListBuilder.create().texOffs(73, 33).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition legR2 = legR.addOrReplaceChild("leg_right_2",
                CubeListBuilder.create().texOffs(20, 35).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, 0.034906585F, 0.0F, 0.0F));
        legR2.addOrReplaceChild("leg_right_pad_2",
                CubeListBuilder.create().texOffs(0, 39).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        legR2.addOrReplaceChild("foot_right",
                CubeListBuilder.create().texOffs(22, 39).addBox(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.034906585F, 0.0F, 0.0F));

        PartDefinition legL = body.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(54, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(3.3F, 12.5F, 0.0F));
        legL.addOrReplaceChild("leg_left_pad",
                CubeListBuilder.create().texOffs(48, 39).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition legL2 = legL.addOrReplaceChild("leg_left_2",
                CubeListBuilder.create().texOffs(72, 48).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, 0.034906585F, 0.0F, 0.0F));
        legL2.addOrReplaceChild("leg_left_pad_2",
                CubeListBuilder.create().texOffs(16, 50).addBox(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        legL2.addOrReplaceChild("foot_left",
                CubeListBuilder.create().texOffs(72, 50).addBox(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, -0.034906585F, 0.0F, 0.0F));

        PartDefinition armL = body.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(62, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offsetAndRotation(6.5F, -8.0F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 12)));
        armL.addOrReplaceChild("arm_left_pad",
                CubeListBuilder.create().texOffs(38, 54).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition armL2 = armL.addOrReplaceChild("arm_left_2",
                CubeListBuilder.create().texOffs(90, 48).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, (float) (-Math.PI / 18), 0.0F, 0.0F));
        armL2.addOrReplaceChild("arm_left_pad_2",
                CubeListBuilder.create().texOffs(0, 58).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        armL2.addOrReplaceChild("hand_left",
                CubeListBuilder.create().texOffs(58, 56).addBox(-1.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.05235988F));

        PartDefinition armR = body.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offsetAndRotation(-6.5F, -8.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 12)));
        armR.addOrReplaceChild("arm_right_pad",
                CubeListBuilder.create().texOffs(70, 10).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition armR2 = armR.addOrReplaceChild("arm_right_2",
                CubeListBuilder.create().texOffs(90, 20).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 9.6F, 0.0F, (float) (-Math.PI / 18), 0.0F, 0.0F));
        armR2.addOrReplaceChild("arm_right_pad_2",
                CubeListBuilder.create().texOffs(0, 26).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        armR2.addOrReplaceChild("hand_right",
                CubeListBuilder.create().texOffs(20, 26).addBox(-2.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.05235988F));

        PartDefinition head = body.addOrReplaceChild("fredhead",
                CubeListBuilder.create().texOffs(39, 22).addBox(-5.5F, -8.0F, -4.5F, 11.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, -13.0F, -0.5F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(49, 65).addBox(-5.0F, 0.0F, -4.5F, 10.0F, 3.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 0.5F, 0.0F, 0.08726646F, 0.0F, 0.0F));
        head.addOrReplaceChild("frednose",
                CubeListBuilder.create().texOffs(17, 67).addBox(-4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, -2.0F, -4.5F));

        PartDefinition earR = head.addOrReplaceChild("ear_right",
                CubeListBuilder.create().texOffs(8, 0).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, (float) (-Math.PI / 3)));
        earR.addOrReplaceChild("ear_right_pad",
                CubeListBuilder.create().texOffs(85, 0).addBox(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition earL = head.addOrReplaceChild("ear_left",
                CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, (float) (Math.PI / 3)));
        earL.addOrReplaceChild("ear_left_pad",
                CubeListBuilder.create().texOffs(40, 39).addBox(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -1.0F, 0.0F));

        PartDefinition hat = head.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(70, 24).addBox(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, -8.4F, 0.0F, (float) (-Math.PI / 180.0), 0.0F, 0.0F));
        hat.addOrReplaceChild("hat_top",
                CubeListBuilder.create().texOffs(78, 61).addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.1F, 0.0F, (float) (-Math.PI / 180.0), 0.0F, 0.0F));

        this.root = LayerDefinition.create(mesh, 100, 80).bakeRoot();
        ModelPart fredRoot = this.root.getChild("freddy_root");
        this.fredbody = fredRoot.getChild("fredbody");
        this.fredhead = this.fredbody.getChild("fredhead");
        this.armLeft = this.fredbody.getChild("arm_left");
        this.armRight = this.fredbody.getChild("arm_right");
        this.legLeft = this.fredbody.getChild("leg_left");
        this.legRight = this.fredbody.getChild("leg_right");
    }

    public static FreddyModel of(int entityId) {
        if (MODELS.size() > 128) {
            MODELS.clear();
        }
        return MODELS.computeIfAbsent(entityId, ignored -> new FreddyModel());
    }

    public void submit(PlayerModel vanilla, AvatarRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, int light, int overlay, int color, int outlineColor, Identifier texture) {
        vanilla.setupAnim(state);
        copyAngles(vanilla);

        pose.pushPose();
        pose.scale(0.75F, 0.65F, 0.75F);
        pose.translate(0.0F, 0.85F, 0.0F);
        RenderType layer = RenderTypes.entityTranslucent(texture != null ? texture : TEXTURE);
        collector.submitModelPart(this.root, pose, layer, light, overlay, (TextureAtlasSprite) null, color,
                (ModelFeatureRenderer.CrumblingOverlay) null, outlineColor);
        pose.popPose();
    }

    public void submit(PlayerModel vanilla, AvatarRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, int light, int overlay, int color, int outlineColor) {
        submit(vanilla, state, pose, collector, light, overlay, color, outlineColor, TEXTURE);
    }

    private void copyAngles(PlayerModel vanilla) {
        this.fredhead.xRot = vanilla.head.xRot;
        this.fredhead.yRot = vanilla.head.yRot;
        this.fredhead.zRot = vanilla.head.zRot;

        this.armLeft.xRot = vanilla.leftArm.xRot;
        this.armLeft.yRot = vanilla.leftArm.yRot;
        this.armLeft.zRot = vanilla.leftArm.zRot;

        this.armRight.xRot = vanilla.rightArm.xRot;
        this.armRight.yRot = vanilla.rightArm.yRot;
        this.armRight.zRot = vanilla.rightArm.zRot;

        this.legLeft.xRot = vanilla.leftLeg.xRot;
        this.legLeft.yRot = vanilla.leftLeg.yRot;
        this.legLeft.zRot = vanilla.leftLeg.zRot;

        this.legRight.xRot = vanilla.rightLeg.xRot;
        this.legRight.yRot = vanilla.rightLeg.yRot;
        this.legRight.zRot = vanilla.rightLeg.zRot;

        this.fredbody.xRot = vanilla.body.xRot * 0.08F;
        this.fredbody.yRot = vanilla.body.yRot * 0.08F;
    }
}
