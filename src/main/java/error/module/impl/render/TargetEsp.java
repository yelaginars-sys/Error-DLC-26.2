package error.module.impl.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import error.event.EventTarget;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.combat.AuraModule;
import error.module.impl.combat.TriggerBot;
import error.setting.impl.CheckBox;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.TargetLightningV2;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ThreadLocalRandom;

public class TargetEsp extends Module {
    public static TargetEsp INSTANCE;

    public final ModeSetting mode = mode("Режим", "Кольцо 2", "Кольцо 2", "Души", "МолнииV2", "Зако", "Орбиты", "Ромб", "Картинка 1", "Картинка 2");

    public final SliderSetting size = slider("Размер", 1.15F, 0.6F, 2.5F, 0.05F);
    public final SliderSetting ringRadius = slider("Радиус кольца", 0.5F, 0.3F, 1.5F, 0.05F).visible(() -> mode.is("Кольцо 2"));
    public final SliderSetting ringSpeed = slider("Скорость кольца", 1.0F, 0.3F, 3.0F, 0.1F).visible(() -> mode.is("Кольцо 2"));
    public final SliderSetting rotSpeed = slider("Скорость вращения", 1.2F, 0.2F, 4.0F, 0.05F);
    public final SliderSetting radius = slider("Радиус", 0.7F, 0.3F, 2.0F, 0.05F);
    public final SliderSetting opacity = slider("Прозрачность", 1.0F, 0.1F, 1.0F, 0.05F);

    public final CheckBox colorOnHit = checkbox("Окрашивание при ударе", true);
    public final CheckBox onHover = checkbox("При наводке", true);

    public final ModeSetting colorMode = mode("Цвет", "Тема", "Тема", "Кастом");
    public final ColorSetting customColor = color("Цвет кастом", ColorUtil.rgba(0, 220, 255, 255)).visible(() -> colorMode.is("Кастом"));

    // Textures ported from Lumen
    private static final Identifier BLOOM_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/bloom.png");
    private static final Identifier ZAKO_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/zako.png");
    private static final Identifier TEX_2 = Identifier.fromNamespaceAndPath("error", "textures/targetesp/targetesp_2.png");
    private static final Identifier TEX_3 = Identifier.fromNamespaceAndPath("error", "textures/targetesp/targetesp_3.png");
    private static final Identifier DIAMOND_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/diamond.png");

    // Pipelines
    private static final RenderPipeline TEX_ADDITIVE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_esp_lumen_additive"))
            .withVertexShader(Identifier.parse("error:core/aura_bloom"))
            .withFragmentShader(Identifier.parse("error:core/aura_bloom"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private static final RenderPipeline TEX_TRANSLUCENT = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_esp_lumen_translucent"))
            .withVertexShader(Identifier.parse("error:core/aura_bloom"))
            .withFragmentShader(Identifier.parse("error:core/aura_bloom"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private static final RenderPipeline COLOR_TRIANGLES = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_esp_lumen_triangles"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private static final RenderPipeline COLOR_LINES = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_esp_lumen_lines"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.LINES)
            .withCull(false)
            .build();

    // Lightning V2 sub-renderer
    private final TargetLightningV2 lightningV2 = new TargetLightningV2();

    // Target animation & tracking
    private final Animation appearAnim = new Animation(0.0F, 0.18F);
    private LivingEntity target;

    // Ring 2 Particles
    private static class RingParticle {
        double x, y, z;
        double vx, vy, vz;
        float age, maxAge;
        float size;
    }
    private final List<RingParticle> ringParticles = new ArrayList<>();

    // Sine animation state
    private float imageSine = 0.0F;
    private float imageDir = 280.0F;

    public TargetEsp() {
        super("Target ESP", "Подсветка цели атаки полностью из Lumen", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        appearAnim.setValue(0.0F);
        ringParticles.clear();
        lightningV2.clear();
        target = null;
    }

    public LivingEntity getTarget() {
        if (AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled() && AuraModule.INSTANCE.getTarget() != null) {
            return AuraModule.INSTANCE.getTarget();
        }
        if (TriggerBot.INSTANCE != null && TriggerBot.INSTANCE.isEnabled() && TriggerBot.INSTANCE.getTarget() != null) {
            return TriggerBot.INSTANCE.getTarget();
        }
        if (onHover.getValue() && mc != null && mc.crosshairPickEntity instanceof LivingEntity living && living != mc.player && living.isAlive()) {
            return living;
        }
        return null;
    }

    public int getEspColor() {
        if ("Кастом".equalsIgnoreCase(colorMode.getValue())) {
            return customColor.getValue();
        }
        return Theme.getAccentColor();
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc == null || mc.player == null || mc.level == null || mc.gameRenderer == null) return;

        LivingEntity active = getTarget();
        boolean hasTarget = active != null && active.isAlive();
        appearAnim.setTarget(hasTarget ? 1.0F : 0.0F);
        appearAnim.update();

        float alpha = appearAnim.getValue() * opacity.get();
        if (alpha <= 0.005F) return;

        if (hasTarget) {
            this.target = active;
        }
        if (this.target == null) return;

        float tickDelta = event.getDeltaTracker() != null ? event.getDeltaTracker().getGameTimeDeltaPartialTick(true) : 1.0F;
        Vec3 targetPos = new Vec3(
                Mth.lerp(tickDelta, this.target.xOld, this.target.getX()),
                Mth.lerp(tickDelta, this.target.yOld, this.target.getY()),
                Mth.lerp(tickDelta, this.target.zOld, this.target.getZ())
        );

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        Matrix4f viewMatrix = camera.getViewRotationMatrix(new Matrix4f())
                .translate((float) -cam.x, (float) -cam.y, (float) -cam.z);

        int baseCol = getEspColor();
        float hurtFactor = 0.0F;
        if (colorOnHit.getValue() && this.target.hurtTime > 0) {
            hurtFactor = (float) Math.sin(this.target.hurtTime * (Math.PI / 10.0D));
        }
        int redCol = ColorUtil.rgba(255, 50, 50, 255);
        int finalColor = hurtFactor > 0.01F ? ColorUtil.interpolateColor(baseCol, redCol, hurtFactor) : baseCol;

        String m = mode.getValue();

        switch (m) {
            case "Кольцо 2" -> renderRing2(viewMatrix, targetPos, this.target, alpha, finalColor, tickDelta);
            case "Души" -> renderSouls(viewMatrix, targetPos, this.target, alpha, finalColor);
            case "МолнииV2" -> lightningV2.render(this.target, tickDelta, finalColor, colorOnHit.getValue(), size.get());
            case "Зако" -> renderBillboard(viewMatrix, targetPos, this.target, ZAKO_TEX, alpha, -1, true);
            case "Картинка 1" -> renderBillboard(viewMatrix, targetPos, this.target, TEX_2, alpha, finalColor, false);
            case "Картинка 2" -> renderBillboard(viewMatrix, targetPos, this.target, TEX_3, alpha, finalColor, false);
            case "Ромб" -> renderRhombus(viewMatrix, targetPos, this.target, alpha, finalColor);
            case "Орбиты" -> renderOrbits(viewMatrix, targetPos, this.target, alpha, finalColor);
        }
    }

    // 1. Ring 2 Mode
    private void renderRing2(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color, float delta) {
        float h = e.getBbHeight();
        double period = 2000.0 / Math.max(0.1F, ringSpeed.get());
        double t = System.currentTimeMillis() % (long) period;
        boolean goingDown = t > period * 0.5;
        double progress = t / (period * 0.5);
        if (goingDown) progress -= 1.0; else progress = 1.0 - progress;

        // Smooth sine wave easing
        progress = progress < 0.5 ? 2.0 * progress * progress : 1.0 - Math.pow(-2.0 * progress + 2.0, 2.0) / 2.0;

        double curRingY = targetPos.y + h * progress;
        float r = ringRadius.get();

        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        // Spawn ring particles
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (ringParticles.size() < 40 && rnd.nextInt(2) == 0) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            RingParticle p = new RingParticle();
            p.x = targetPos.x + Math.cos(angle) * r;
            p.y = curRingY + (rnd.nextDouble() - 0.5) * 0.1;
            p.z = targetPos.z + Math.sin(angle) * r;
            p.vx = (rnd.nextDouble() - 0.5) * 0.02;
            p.vy = (rnd.nextDouble() - 0.5) * 0.04;
            p.vz = (rnd.nextDouble() - 0.5) * 0.02;
            p.size = 0.06F + rnd.nextFloat() * 0.08F;
            p.age = 0.0F;
            p.maxAge = 20.0F + rnd.nextFloat() * 20.0F;
            ringParticles.add(p);
        }

        // Draw Dual Gradient Ring (Triangles)
        try (ByteBufferBuilder mem = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            int segs = 60;
            int colCore = ColorUtil.withAlpha(color, (int) (220 * alpha));
            int colFade = ColorUtil.withAlpha(color, 0);

            float ribbonH = 0.12F;

            for (int i = 0; i < segs; i++) {
                double a1 = Math.toRadians((i * 360.0) / segs);
                double a2 = Math.toRadians(((i + 1) * 360.0) / segs);

                double x1 = targetPos.x + Math.cos(a1) * r;
                double z1 = targetPos.z + Math.sin(a1) * r;
                double x2 = targetPos.x + Math.cos(a2) * r;
                double z2 = targetPos.z + Math.sin(a2) * r;

                // Center line vertices
                Vector4f mid1 = viewMatrix.transform(new Vector4f((float) x1, (float) curRingY, (float) z1, 1.0F));
                Vector4f mid2 = viewMatrix.transform(new Vector4f((float) x2, (float) curRingY, (float) z2, 1.0F));

                // Upper vertices
                Vector4f up1 = viewMatrix.transform(new Vector4f((float) x1, (float) (curRingY + ribbonH), (float) z1, 1.0F));
                Vector4f up2 = viewMatrix.transform(new Vector4f((float) x2, (float) (curRingY + ribbonH), (float) z2, 1.0F));

                // Lower vertices
                Vector4f down1 = viewMatrix.transform(new Vector4f((float) x1, (float) (curRingY - ribbonH), (float) z1, 1.0F));
                Vector4f down2 = viewMatrix.transform(new Vector4f((float) x2, (float) (curRingY - ribbonH), (float) z2, 1.0F));

                if (mid1.z >= -0.05F || mid2.z >= -0.05F) continue;

                // Upper quad (2 tris)
                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(up1.x, up1.y, up1.z).setColor(colFade);
                b.addVertex(up2.x, up2.y, up2.z).setColor(colFade);

                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(up2.x, up2.y, up2.z).setColor(colFade);
                b.addVertex(mid2.x, mid2.y, mid2.z).setColor(colCore);

                // Lower quad (2 tris)
                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(down2.x, down2.y, down2.z).setColor(colFade);
                b.addVertex(down1.x, down1.y, down1.z).setColor(colFade);

                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(mid2.x, mid2.y, mid2.z).setColor(colCore);
                b.addVertex(down2.x, down2.y, down2.z).setColor(colFade);
            }

            try (MeshData mesh = b.buildOrThrow()) {
                GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Ring2 VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "Ring2 Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(COLOR_TRIANGLES);
                    pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                    pass.setVertexBuffer(0, vb.slice());
                    pass.draw(mesh.drawState().vertexCount(), 1, 0, 0);
                } finally {
                    vb.close();
                }
            }
        }

        // Draw Ring Floating Particles (Bloom Quads)
        AbstractTexture bloomTex = mc.getTextureManager().getTexture(BLOOM_TEX);
        if (bloomTex != null && !ringParticles.isEmpty()) {
            Iterator<RingParticle> it = ringParticles.iterator();
            try (ByteBufferBuilder pMem = new ByteBufferBuilder(1024 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
                BufferBuilder pb = new BufferBuilder(pMem, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                int pCount = 0;

                while (it.hasNext()) {
                    RingParticle p = it.next();
                    p.age += 1.0F;
                    if (p.age >= p.maxAge) {
                        it.remove();
                        continue;
                    }
                    p.x += p.vx;
                    p.y += p.vy;
                    p.z += p.vz;
                    float life = 1.0F - p.age / p.maxAge;
                    int pCol = ColorUtil.withAlpha(color, (int) (180 * life * alpha));

                    Vector4f cv = viewMatrix.transform(new Vector4f((float) p.x, (float) p.y, (float) p.z, 1.0F));
                    if (cv.z >= -0.05F) continue;

                    float hs = p.size * (0.5F + 0.5F * life);
                    pb.addVertex(cv.x - hs, cv.y - hs, cv.z).setUv(0.0F, 1.0F).setColor(pCol);
                    pb.addVertex(cv.x + hs, cv.y - hs, cv.z).setUv(1.0F, 1.0F).setColor(pCol);
                    pb.addVertex(cv.x + hs, cv.y + hs, cv.z).setUv(1.0F, 0.0F).setColor(pCol);
                    pb.addVertex(cv.x - hs, cv.y + hs, cv.z).setUv(0.0F, 0.0F).setColor(pCol);
                    pCount++;
                }

                if (pCount > 0) {
                    try (MeshData mesh = pb.buildOrThrow()) {
                        GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Ring2 Particles", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                                () -> "Ring2 Particles Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                            pass.setPipeline(TEX_ADDITIVE);
                            pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                            pass.bindTexture("Sampler0", bloomTex.getTextureView(), sampler);
                            pass.setVertexBuffer(0, vb.slice());
                            GpuBuffer ib = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(pCount * 6);
                            pass.setIndexBuffer(ib, RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).type());
                            pass.drawIndexed(pCount * 6, 1, 0, 0, 0);
                        } finally {
                            vb.close();
                        }
                    }
                }
            }
        }
    }

    // 2. Souls Mode
    private void renderSouls(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color) {
        AbstractTexture bloomTex = mc.getTextureManager().getTexture(BLOOM_TEX);
        if (bloomTex == null) return;
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        double cy = targetPos.y + e.getBbHeight() * 0.5;
        double radius = this.radius.get();
        long now = System.currentTimeMillis();

        try (ByteBufferBuilder mem = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int quadCount = 0;

            int strands = 3;
            int nodesPerStrand = 16;

            for (int s = 0; s < strands; s++) {
                double strandOffset = (s * Math.PI * 2.0) / strands;
                for (int i = 0; i < nodesPerStrand; i++) {
                    float tail = 1.0F - (float) i / nodesPerStrand;
                    double angle = (now * 0.003 * rotSpeed.get()) + strandOffset - (i * 0.08);
                    double yWave = Math.sin(angle * 1.5) * 0.45;

                    double px = targetPos.x + Math.sin(angle) * radius;
                    double py = cy + yWave;
                    double pz = targetPos.z + Math.cos(angle) * radius;

                    Vector4f cv = viewMatrix.transform(new Vector4f((float) px, (float) py, (float) pz, 1.0F));
                    if (cv.z >= -0.05F) continue;

                    float hs = (0.08F + 0.14F * tail) * size.get();
                    int c = ColorUtil.withAlpha(color, (int) (220 * tail * alpha));

                    b.addVertex(cv.x - hs, cv.y - hs, cv.z).setUv(0.0F, 1.0F).setColor(c);
                    b.addVertex(cv.x + hs, cv.y - hs, cv.z).setUv(1.0F, 1.0F).setColor(c);
                    b.addVertex(cv.x + hs, cv.y + hs, cv.z).setUv(1.0F, 0.0F).setColor(c);
                    b.addVertex(cv.x - hs, cv.y + hs, cv.z).setUv(0.0F, 0.0F).setColor(c);
                    quadCount++;
                }
            }

            if (quadCount > 0) {
                try (MeshData mesh = b.buildOrThrow()) {
                    GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Souls VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                    GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                            () -> "Souls Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                        pass.setPipeline(TEX_ADDITIVE);
                        pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        pass.bindTexture("Sampler0", bloomTex.getTextureView(), sampler);
                        pass.setVertexBuffer(0, vb.slice());
                        GpuBuffer ib = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(quadCount * 6);
                        pass.setIndexBuffer(ib, RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).type());
                        pass.drawIndexed(quadCount * 6, 1, 0, 0, 0);
                    } finally {
                        vb.close();
                    }
                }
            }
        }
    }

    // 3. Billboards (Zako, Картинка 1, Картинка 2)
    private void renderBillboard(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, Identifier tex, float alpha, int color, boolean isZako) {
        AbstractTexture texture = mc.getTextureManager().getTexture(tex);
        if (texture == null) return;
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        double cy = targetPos.y + e.getBbHeight() * 0.55;
        Vector4f cv = viewMatrix.transform(new Vector4f((float) targetPos.x, (float) cy, (float) targetPos.z, 1.0F));
        if (cv.z >= -0.05F) return;

        long now = System.currentTimeMillis();
        float rot = (now * 0.1F * rotSpeed.get()) % 360.0F;
        float rad = (float) Math.toRadians(rot);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float s = size.get() * (isZako ? 0.65F : 0.85F);
        float hw = s * 0.5F;
        float hh = s * 0.5F;

        float dx0 = (-hw) * cos - (-hh) * sin;
        float dy0 = (-hw) * sin + (-hh) * cos;
        float dx1 = (hw) * cos - (-hh) * sin;
        float dy1 = (hw) * sin + (-hh) * cos;
        float dx2 = (hw) * cos - (hh) * sin;
        float dy2 = (hw) * sin + (hh) * cos;
        float dx3 = (-hw) * cos - (hh) * sin;
        float dy3 = (-hw) * sin + (hh) * cos;

        int finalCol = isZako ? ColorUtil.rgba(255, 255, 255, (int) (255 * alpha)) : ColorUtil.withAlpha(color, (int) (240 * alpha));

        try (ByteBufferBuilder mem = new ByteBufferBuilder(DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4)) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            b.addVertex(cv.x + dx0, cv.y + dy0, cv.z).setUv(0.0F, 1.0F).setColor(finalCol);
            b.addVertex(cv.x + dx1, cv.y + dy1, cv.z).setUv(1.0F, 1.0F).setColor(finalCol);
            b.addVertex(cv.x + dx2, cv.y + dy2, cv.z).setUv(1.0F, 0.0F).setColor(finalCol);
            b.addVertex(cv.x + dx3, cv.y + dy3, cv.z).setUv(0.0F, 0.0F).setColor(finalCol);

            try (MeshData mesh = b.buildOrThrow()) {
                GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Billboard VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "Billboard Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(isZako ? TEX_TRANSLUCENT : TEX_ADDITIVE);
                    pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                    pass.bindTexture("Sampler0", texture.getTextureView(), sampler);
                    pass.setVertexBuffer(0, vb.slice());
                    GpuBuffer ib = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(6);
                    pass.setIndexBuffer(ib, RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).type());
                    pass.drawIndexed(6, 1, 0, 0, 0);
                } finally {
                    vb.close();
                }
            }
        }
    }

    // 4. Rhombus Mode
    private void renderRhombus(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color) {
        AbstractTexture tex = mc.getTextureManager().getTexture(DIAMOND_TEX);
        if (tex == null) tex = mc.getTextureManager().getTexture(BLOOM_TEX);
        if (tex == null) return;
        renderBillboard(viewMatrix, targetPos, e, DIAMOND_TEX, alpha, color, false);
    }

    // 5. Orbits Mode
    private void renderOrbits(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color) {
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        double cy = targetPos.y + e.getBbHeight() * 0.5;
        float r = radius.get() * 1.1F;
        long now = System.currentTimeMillis();
        float rot = (float) (now * 0.002 * rotSpeed.get());

        try (ByteBufferBuilder mem = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            int segs = 48;
            int cCore = ColorUtil.withAlpha(color, (int) (220 * alpha));
            int cFade = ColorUtil.withAlpha(color, 0);

            for (int orbit = 0; orbit < 2; orbit++) {
                float tilt = orbit == 0 ? 0.45F : -0.45F;
                float orbitRot = rot + orbit * (float) Math.PI;

                for (int i = 0; i < segs; i++) {
                    double a1 = (i * Math.PI * 2.0) / segs;
                    double a2 = ((i + 1) * Math.PI * 2.0) / segs;

                    double ox1 = Math.cos(a1) * r;
                    double oz1 = Math.sin(a1) * r;
                    double oy1 = Math.sin(a1 + orbitRot) * (r * tilt);

                    double ox2 = Math.cos(a2) * r;
                    double oz2 = Math.sin(a2) * r;
                    double oy2 = Math.sin(a2 + orbitRot) * (r * tilt);

                    Vector4f mid1 = viewMatrix.transform(new Vector4f((float) (targetPos.x + ox1), (float) (cy + oy1), (float) (targetPos.z + oz1), 1.0F));
                    Vector4f mid2 = viewMatrix.transform(new Vector4f((float) (targetPos.x + ox2), (float) (cy + oy2), (float) (targetPos.z + oz2), 1.0F));

                    if (mid1.z >= -0.05F || mid2.z >= -0.05F) continue;

                    b.addVertex(mid1.x, mid1.y - 0.04F, mid1.z).setColor(cFade);
                    b.addVertex(mid1.x, mid1.y, mid1.z).setColor(cCore);
                    b.addVertex(mid2.x, mid2.y, mid2.z).setColor(cCore);

                    b.addVertex(mid1.x, mid1.y - 0.04F, mid1.z).setColor(cFade);
                    b.addVertex(mid2.x, mid2.y, mid2.z).setColor(cCore);
                    b.addVertex(mid2.x, mid2.y - 0.04F, mid2.z).setColor(cFade);
                }
            }

            try (MeshData mesh = b.buildOrThrow()) {
                GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Orbits VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "Orbits Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(COLOR_TRIANGLES);
                    pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                    pass.setVertexBuffer(0, vb.slice());
                    pass.draw(mesh.drawState().vertexCount(), 1, 0, 0);
                } finally {
                    vb.close();
                }
            }
        }
    }
}
