package error.util.display.rounded;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class RoundedRectPipeline {
    public static final float PADDING = 1.0F;

    public static final RenderPipeline INSTANCE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("example", "pipeline/rounded_rect"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("example", "core/rounded_rect"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("example", "core/rounded_rect"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .build()
    );

    private RoundedRectPipeline() {
    }

    public static void init() {
    }
}