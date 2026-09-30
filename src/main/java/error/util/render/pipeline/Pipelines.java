package error.util.render.pipeline;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import error.util.render.renders.VertexFormats;

import java.util.Optional;

public final class Pipelines {
    public static final RenderPipeline RECT = builder("rect", "rect", false).build();
    public static final RenderPipeline CIRCLE_ARC = builder("circle_arc", "circle_arc", false).build();
    public static final RenderPipeline TEXT = builder("text", "text", true).build();
    public static final RenderPipeline BLUR_RECT = builder("blur_rect", "blur_rect", true).build();
    public static final RenderPipeline TEXTURE = builder("texture", "texture", true).build();

    public static final BindGroupLayout GUI_BLUR_LAYOUT = BindGroupLayout.builder()
            .withSampler("CurrentInput")
            .withUniform("GuiKawaseUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    public static final RenderPipeline GUI_BLUR_DOWN = blurBuilder("gui_blur_down", "gui_kawase_down").build();
    public static final RenderPipeline GUI_BLUR_UP = blurBuilder("gui_blur_up", "gui_kawase_up").build();

    private static RenderPipeline.Builder blurBuilder(String name, String fragmentShader) {
        return RenderPipeline.builder()
                .withLocation(Identifier.parse("error:gui/" + name))
                .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
                .withFragmentShader(Identifier.parse("error:post/blurs/" + fragmentShader))
                .withBindGroupLayout(GUI_BLUR_LAYOUT)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withDepthStencilState(Optional.<DepthStencilState>empty())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false);
    }

    private static RenderPipeline.Builder builder(String name, String shader, boolean sampled) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Identifier.parse("error:pipeline/gui/" + name))
                .withVertexShader(Identifier.parse("error:core/" + shader))
                .withFragmentShader(Identifier.parse("error:core/" + shader))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withVertexBinding(0, VertexFormats.UI)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withCull(false);

        if (sampled) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }
        return builder;
    }

    private Pipelines() {}
}