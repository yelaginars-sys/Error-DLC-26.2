package error.mixin.accessor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {

    @Invoker("renderPlayerArm")
    void hmi$renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int light, float equipProgress,
                             float swingProgress, HumanoidArm arm);

    @Invoker("renderMap")
    void hmi$renderMap(PoseStack poseStack, SubmitNodeCollector collector, int light, ItemStack stack);

    @Invoker("renderMapHand")
    void hmi$renderMapHand(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidArm arm);

    @Invoker("renderOneHandedMap")
    void hmi$renderOneHandedMap(PoseStack poseStack, SubmitNodeCollector collector, int light, float equipProgress,
                                HumanoidArm arm, float swingProgress, ItemStack stack);

    @Invoker("renderTwoHandedMap")
    void hmi$renderTwoHandedMap(PoseStack poseStack, SubmitNodeCollector collector, int light, float pitch,
                                float equipProgress, float swingProgress);
}
