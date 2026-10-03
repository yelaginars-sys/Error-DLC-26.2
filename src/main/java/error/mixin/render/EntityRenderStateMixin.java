package error.mixin.render;

import error.interfaces.CustomModelCarrier;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements CustomModelCarrier {

    @Unique
    private String error$customModel;

    @Override
    public String error$customModel() {
        return this.error$customModel;
    }

    @Override
    public void error$setCustomModel(String model) {
        this.error$customModel = model;
    }
}
