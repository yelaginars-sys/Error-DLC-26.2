package error.util.display.blur;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;

public final class BlurManager {
    private static final BlurTargets TARGETS = new BlurTargets();
    private static final Set<BlurKey> REQUESTED = new LinkedHashSet<>();

    private BlurManager() {
    }

    public static void request(BlurKey key) {
        REQUESTED.add(key);
    }

    public static GpuTextureView outputView(BlurKey key) {
        TARGETS.ensure(mainTarget());
        return TARGETS.output(key).getColorTextureView();
    }

    public static void process() {
        if (REQUESTED.isEmpty()) {
            return;
        }
        RenderTarget main = mainTarget();
        TARGETS.ensure(main);
        int depth = REQUESTED.stream().mapToInt(key -> key.type().downDepth(key.strength())).max().orElse(0);
        GpuTextureView source = main.getColorTextureView();
        for (int level = 1; level <= depth; level++) {
            TextureTarget target = TARGETS.down(level);
            pass(BlurPipelines.DOWNSAMPLE, source, target);
            source = target.getColorTextureView();
        }
        REQUESTED.forEach(BlurManager::process);
        REQUESTED.clear();
    }

    public static void close() {
        REQUESTED.clear();
        TARGETS.close();
    }

    private static void process(BlurKey key) {
        switch (key.type()) {
            case KAWASE -> kawase(key);
            case GAUSSIAN -> separable(key, BlurPipelines.GAUSSIAN_HORIZONTAL, BlurPipelines.GAUSSIAN_VERTICAL);
            case BOX -> separable(key, BlurPipelines.BOX_HORIZONTAL, BlurPipelines.BOX_VERTICAL);
        }
    }

    private static void kawase(BlurKey key) {
        GpuTextureView source = TARGETS.down(key.strength()).getColorTextureView();
        for (int level = key.strength() - 1; level >= 1; level--) {
            TextureTarget target = level == 1 ? TARGETS.output(key) : TARGETS.temp(level);
            pass(BlurPipelines.UPSAMPLE, source, target);
            source = target.getColorTextureView();
        }
    }

    private static void separable(BlurKey key, RenderPipeline horizontal, RenderPipeline vertical) {
        int level = key.type().outputLevel(key.strength());
        int repeats = key.strength() % 2 == 0 ? 2 : 1;
        TextureTarget temp = TARGETS.temp(level);
        TextureTarget output = TARGETS.output(key);
        GpuTextureView source = TARGETS.down(level).getColorTextureView();
        for (int i = 0; i < repeats; i++) {
            pass(horizontal, source, temp);
            pass(vertical, temp.getColorTextureView(), output);
            source = output.getColorTextureView();
        }
    }

    private static void pass(RenderPipeline pipeline, GpuTextureView source, TextureTarget target) {
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Blur " + pipeline.getLocation().getPath(), target.getColorTextureView(), Optional.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("InSampler", source, sampler);
            renderPass.draw(3, 1, 0, 0);
        }
    }

    private static RenderTarget mainTarget() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }
}
