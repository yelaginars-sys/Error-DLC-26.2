package error.util.display.image;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class ImagePipeline {
    public static final float PADDING = 1.0F;

    public static final RenderPipeline INSTANCE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("example", "pipeline/image"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("example", "core/image"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("example", "core/image"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .build()
    );

    private ImagePipeline() {
    }

    public static void init() {
    }
}
