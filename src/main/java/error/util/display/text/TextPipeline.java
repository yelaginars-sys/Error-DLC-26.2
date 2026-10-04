package error.util.display.text;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class TextPipeline {
    public static final RenderPipeline INSTANCE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("example", "pipeline/text"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("example", "core/text"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("example", "core/text"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .build()
    );

    private TextPipeline() {
    }

    public static void init() {
    }
}
