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

public final class AmogusModel {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("error", "models/skycore/amogus.png");
    private static final Map<Integer, AmogusModel> MODELS = new HashMap<>();

    private final ModelPart root;
    private final ModelPart bodyShell;
    private final ModelPart visor;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    private AmogusModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition data = mesh.getRoot();

        PartDefinition amogus = data.addOrReplaceChild("amogus_root", CubeListBuilder.create(), PartPose.ZERO);

        this.bodyShell = amogus.addOrReplaceChild("body_shell", CubeListBuilder.create()
                        .texOffs(34, 8).addBox(-4.0F, 6.0F, -3.0F, 8.0F, 12.0F, 6.0F)
                        .texOffs(15, 10).addBox(-3.0F, 9.0F, 3.0F, 6.0F, 8.0F, 3.0F)
                        .texOffs(26, 0).addBox(-3.0F, 5.0F, -3.0F, 6.0F, 1.0F, 6.0F),
                PartPose.ZERO).bake(64, 64);

        this.visor = amogus.addOrReplaceChild("visor", CubeListBuilder.create()
                        .texOffs(0, 10).addBox(-3.0F, 7.0F, -4.0F, 6.0F, 4.0F, 1.0F),
                PartPose.ZERO).bake(64, 64);

        this.leftLeg = amogus.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(0.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(2.0F, 18.0F, 0.0F)).bake(64, 64);

        this.rightLeg = amogus.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(13, 0).addBox(-3.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-2.0F, 18.0F, 0.0F)).bake(64, 64);

        this.root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    public static AmogusModel of(int entityId) {
        if (MODELS.size() > 128) {
            MODELS.clear();
        }
        return MODELS.computeIfAbsent(entityId, ignored -> new AmogusModel());
    }

    public void submit(PlayerModel vanilla, AvatarRenderState state, PoseStack pose,
                       SubmitNodeCollector collector, int light, int overlay, int color, int outlineColor, Identifier texture) {
        vanilla.setupAnim(state);
        copyAngles(vanilla);

        pose.pushPose();
        pose.scale(1.0F, 1.0F, 1.0F);
        pose.translate(0.0F, -0.5F, 0.0F);
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
        ModelPart amogus = this.root.getChild("amogus_root");
        ModelPart shell = amogus.getChild("body_shell");
        ModelPart vis = amogus.getChild("visor");
        ModelPart lLeg = amogus.getChild("left_leg");
        ModelPart rLeg = amogus.getChild("right_leg");

        shell.xRot = vanilla.body.xRot * 0.5F;
        shell.yRot = vanilla.body.yRot * 0.35F;

        // Lock visor (eyes) to body shell so they stay anchored and do not detach
        vis.xRot = shell.xRot;
        vis.yRot = shell.yRot;
        vis.zRot = shell.zRot;
        vis.x = shell.x;
        vis.y = shell.y;
        vis.z = shell.z;

        lLeg.xRot = vanilla.leftLeg.xRot;
        lLeg.yRot = vanilla.leftLeg.yRot;
        lLeg.zRot = vanilla.leftLeg.zRot;

        rLeg.xRot = vanilla.rightLeg.xRot;
        rLeg.yRot = vanilla.rightLeg.yRot;
        rLeg.zRot = vanilla.rightLeg.zRot;
    }
}
