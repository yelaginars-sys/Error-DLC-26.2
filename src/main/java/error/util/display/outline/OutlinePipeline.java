package error.util.display.outline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class OutlinePipeline {
    public static final float PADDING = 1.0F;

    public static final RenderPipeline INSTANCE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("example", "pipeline/outline"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("example", "core/outline"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("example", "core/outline"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .build()
    );

    private OutlinePipeline() {
    }

    public static void init() {
    }
}
