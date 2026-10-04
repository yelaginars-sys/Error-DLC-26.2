package error.util.display.head;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class PlayerHeadPipeline {
    public static final float PADDING = 1.0F;

    public static final RenderPipeline INSTANCE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("example", "pipeline/player_head"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("example", "core/head"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("example", "core/head"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .build()
    );

    private PlayerHeadPipeline() {
    }

    public static void init() {
    }
}
