package error.util.display.blur;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class BlurPipelines {
    public static final float PADDING = 1.0F;

    public static final RenderPipeline DOWNSAMPLE = fullscreen("downsample", "blur_down", false, false);
    public static final RenderPipeline UPSAMPLE = fullscreen("upsample", "blur_up", false, false);
    public static final RenderPipeline GAUSSIAN_HORIZONTAL = fullscreen("gaussian_h", "blur_separable", false, false);
    public static final RenderPipeline GAUSSIAN_VERTICAL = fullscreen("gaussian_v", "blur_separable", true, false);
    public static final RenderPipeline BOX_HORIZONTAL = fullscreen("box_h", "blur_separable", false, true);
    public static final RenderPipeline BOX_VERTICAL = fullscreen("box_v", "blur_separable", true, true);

    public static final RenderPipeline PANEL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(id("pipeline/blur_panel"))
                    .withVertexShader(id("core/blur_panel"))
                    .withFragmentShader(id("core/blur_panel"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR)
                    .build()
    );

    private BlurPipelines() {
    }

    public static void init() {
    }

    private static RenderPipeline fullscreen(String name, String fragment, boolean vertical, boolean box) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(id("pipeline/blur_" + name))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(id("core/" + fragment))
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(ColorTargetState.DEFAULT);
        RenderPipelines.TRACY_BLIT.getBindGroupLayouts().forEach(builder::withBindGroupLayout);
        if (vertical) {
            builder.withShaderDefine("BLUR_VERTICAL");
        }
        if (box) {
            builder.withShaderDefine("KERNEL_BOX");
        }
        return RenderPipelines.register(builder.build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("example", path);
    }
}
