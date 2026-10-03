package error.util.render.pipeline;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 */
public class PiplinePost {
    private static final BindGroupLayout PUDDLES_LAYOUT = BindGroupLayout.builder().withSampler("SceneSampler").withSampler("DepthSampler").withUniform("WorldPuddlesUniforms", UniformType.UNIFORM_BUFFER).build();
    public static final RenderPipeline WORLD_PUDDLES = worldPostPipeline("puddles", "error:post/world_puddles", PUDDLES_LAYOUT);
    private static final BindGroupLayout HAND_MASK_LAYOUT = BindGroupLayout.builder().withSampler("BeforeTexture").withSampler("AfterTexture").withSampler("BeforeDepth").withSampler("AfterDepth").withUniform("HandMaskUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout SHADER_HANDS_LAYOUT = BindGroupLayout.builder().withSampler("BlurredSampler").withSampler("MaskSampler").withUniform("HandCompositeUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout HAND_FILL_LAYOUT = BindGroupLayout.builder().withSampler("MaskSampler").withUniform("HandFillUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout HAND_GLASS_LAYOUT = BindGroupLayout.builder().withSampler("SceneSampler").withSampler("MaskSampler").withUniform("HandGlassUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout HAND_OUTLINE_LAYOUT = BindGroupLayout.builder().withSampler("MaskSampler").withUniform("HandOutlineUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout HAND_HALO_LAYOUT = BindGroupLayout.builder().withSampler("BlurredSampler").withSampler("MaskSampler").withUniform("HandHaloUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout HIT_EFFECT_LAYOUT = BindGroupLayout.builder().withSampler("SceneSampler").withSampler("DepthSampler").withUniform("HitEffectUniforms", UniformType.UNIFORM_BUFFER).build();
    public static final RenderPipeline WORLD_HIT_EFFECT = worldPostPipeline("hit_effect", "error:post/hit_effect", HIT_EFFECT_LAYOUT);
    private static final BindGroupLayout HAND_TRAIL_LAYOUT = BindGroupLayout.builder().withSampler("PrevSampler").withSampler("InjectSampler").withUniform("HandTrailUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout HAND_BLUR_LAYOUT = BindGroupLayout.builder().withSampler("CurrentInput").withUniform("HandBlurUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout BLOCK_HIGHLIGHT_LAYOUT = BindGroupLayout.builder().withUniform("BlockHighLightTransform", UniformType.UNIFORM_BUFFER).withUniform("BlockHighLightStyle", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout WORLD_SKY_CLOUDS_LAYOUT = BindGroupLayout.builder().withSampler("DepthSampler").withUniform("WorldSkyUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout WORLD_SKY_LAYOUT = BindGroupLayout.builder().withSampler("DepthSampler").withSampler("CloudSampler").withUniform("WorldSkyUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout HAND_MASK_SMOOTH_LAYOUT = BindGroupLayout.builder().withSampler("RawMask").withUniform("HandMaskUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout SATURATION_LAYOUT = BindGroupLayout.builder().withSampler("SceneSampler").withUniform("SaturationUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final BindGroupLayout JUMP_CIRCLES_LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler")
            .withSampler("DepthSampler")
            .withUniform("WorldJumpCircleUniforms", UniformType.UNIFORM_BUFFER)
            .build();
    public static final RenderPipeline WORLD_JUMP_CIRCLES = worldPostPipeline("jump_circles", "error:post/jump_circle", JUMP_CIRCLES_LAYOUT);
    public static final GpuFormat MASK_RAW_FORMAT = GpuFormat.RGBA16_FLOAT;

    public static final RenderPipeline HAND_MASK = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_mask"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_mask"))
            .withBindGroupLayout(HAND_MASK_LAYOUT)
            .withColorTargetState(new ColorTargetState(Optional.empty(), MASK_RAW_FORMAT, ColorTargetState.WRITE_ALL))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_MASK_SMOOTH = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_mask_smooth"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_mask_smooth"))
            .withBindGroupLayout(HAND_MASK_SMOOTH_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();
    public static final RenderPipeline HAND_NOISE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_noise"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_noise"))
            .withBindGroupLayout(HAND_FILL_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_HOLOGRAM = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_hologram"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_hologram"))
            .withBindGroupLayout(HAND_FILL_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline SHADER_HANDS = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/shader_hands"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/shader_hands"))
            .withBindGroupLayout(SHADER_HANDS_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_FILL = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_fill"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_fill"))
            .withBindGroupLayout(HAND_FILL_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_PLASMA = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_plasma"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_plasma"))
            .withBindGroupLayout(HAND_FILL_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_GLASS = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_glass"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_glass"))
            .withBindGroupLayout(HAND_GLASS_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_OUTLINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_outline"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_outline"))
            .withBindGroupLayout(HAND_OUTLINE_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_HALO = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_halo"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_halo"))
            .withBindGroupLayout(HAND_HALO_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_BLUR_DOWN = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_blur_down"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_blur_down"))
            .withBindGroupLayout(HAND_BLUR_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_BLUR_UP = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_blur_up"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_blur_up"))
            .withBindGroupLayout(HAND_BLUR_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline HAND_TRAIL = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/post/hand_trail"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/hands/hand_trail"))
            .withBindGroupLayout(HAND_TRAIL_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.<DepthStencilState>empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();


    public static final RenderPipeline BLOCK_HIGHLIGHT_CAUSTICS = blockOutlinePipeline("caustics", "caustics", CompareOp.GREATER_THAN_OR_EQUAL);
    public static final RenderPipeline BLOCK_HIGHLIGHT_CAUSTICS_THROUGH = blockOutlinePipeline("caustics_through", "caustics", CompareOp.ALWAYS_PASS);
    public static final RenderPipeline BLOCK_HIGHLIGHT_GLOSSY = blockOutlinePipeline("glossy", "glossy", CompareOp.GREATER_THAN_OR_EQUAL);
    public static final RenderPipeline BLOCK_HIGHLIGHT_GLOSSY_THROUGH = blockOutlinePipeline("glossy_through", "glossy", CompareOp.ALWAYS_PASS);
    public static final RenderPipeline BLOCK_HIGHLIGHT_DEEP_SPACE = blockOutlinePipeline("deep_space", "deep_space", CompareOp.GREATER_THAN_OR_EQUAL);
    public static final RenderPipeline BLOCK_HIGHLIGHT_DEEP_SPACE_THROUGH = blockOutlinePipeline("deep_space_through", "deep_space", CompareOp.ALWAYS_PASS);
    public static final GpuFormat SKY_CLOUDS_FORMAT = GpuFormat.RGBA16_FLOAT;

    public static final RenderPipeline WORLD_SKY_STARRY_SKY_SPACE = skyCloudsPipeline("sky_clouds_starry_sky", "error:world/sky/clouds_starry_sky");
    public static final RenderPipeline WORLD_SKY_CLOUDS_NEBULA = skyCloudsPipeline("sky_clouds_nebula", "error:world/sky/clouds_nebula");
    public static final RenderPipeline WORLD_SKY_CLOUDS_PLASMA = skyCloudsPipeline("sky_clouds_plasma", "error:world/sky/clouds_plasma");
    public static final RenderPipeline WORLD_SKY_CLOUDS_CAUSTIC = skyCloudsPipeline("sky_clouds_caustic", "error:world/sky/clouds_caustic");
    public static final RenderPipeline WORLD_SKY_CAUSTIC = worldPostPipeline("sky_caustic", "error:world/sky/caustic", WORLD_SKY_LAYOUT);
    public static final RenderPipeline WORLD_SKY_STARRY_SKY = worldPostPipeline("sky_starry_sky", "error:world/sky/starry_sky", WORLD_SKY_LAYOUT);
    public static final RenderPipeline WORLD_SKY_NEBULA = worldPostPipeline("sky_nebula", "error:world/sky/nebula", WORLD_SKY_LAYOUT);
    public static final RenderPipeline WORLD_SKY_PLASMA = worldPostPipeline("sky_plasma", "error:world/sky/plasma", WORLD_SKY_LAYOUT);
    public static final RenderPipeline WORLD_SATURATION = worldPostPipeline("saturation", "error:post/world_saturation", SATURATION_LAYOUT);
    private static final BindGroupLayout VOLUMETRIC_FOG_LAYOUT = BindGroupLayout.builder()
            .withSampler("DepthSampler")
            .withUniform("VolumetricFogUniforms", UniformType.UNIFORM_BUFFER)
            .build();
    private static final BindGroupLayout LIGHTNING_LAYOUT = BindGroupLayout.builder()
            .withSampler("DepthSampler")
            .withUniform("LightningUniforms", UniformType.UNIFORM_BUFFER)
            .build();
    public static final RenderPipeline WORLD_LIGHTNING = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/lightning"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/world_lightning"))
            .withBindGroupLayout(LIGHTNING_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();
    public static final RenderPipeline WORLD_VOLUMETRIC_FOG = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/volumetric_fog"))
            .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
            .withFragmentShader(Identifier.parse("error:post/world_volumetric_fog"))
            .withBindGroupLayout(VOLUMETRIC_FOG_LAYOUT)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final GpuFormat EFFECT_FORMAT = GpuFormat.RGBA8_UNORM;

    private static RenderPipeline blockOutlinePipeline(String name, String fragment, CompareOp depthCompare) {
        return RenderPipeline.builder()
                .withLocation(Identifier.parse("error:pipeline/world/blocks/" + name))
                .withVertexShader(Identifier.parse("error:world/blocks"))
                .withFragmentShader(Identifier.parse("error:world/blocks/" + fragment))
                .withBindGroupLayout(BLOCK_HIGHLIGHT_LAYOUT)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(depthCompare, false))
                .withVertexBinding(0, DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
    }

    private static RenderPipeline worldPostPipeline(String name,
                                                    String fragment,
                                                    BindGroupLayout layout) {
        return RenderPipeline.builder()
                .withLocation(Identifier.parse("error:pipeline/world/" + name))
                .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
                .withFragmentShader(Identifier.parse(fragment))
                .withBindGroupLayout(layout)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withDepthStencilState(Optional.<DepthStencilState>empty())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
    }

    private static RenderPipeline skyCloudsPipeline(String name, String fragment) {
        return RenderPipeline.builder()
                .withLocation(Identifier.parse("error:pipeline/world/" + name))
                .withVertexShader(Identifier.parse("error:post/blurs/kawase_common"))
                .withFragmentShader(Identifier.parse(fragment))
                .withBindGroupLayout(WORLD_SKY_CLOUDS_LAYOUT)
                .withColorTargetState(new ColorTargetState(Optional.empty(), SKY_CLOUDS_FORMAT, ColorTargetState.WRITE_ALL))
                .withDepthStencilState(Optional.<DepthStencilState>empty())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
    }




    private PiplinePost() {
    }
}
