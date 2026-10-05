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
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Optional;
import java.util.OptionalDouble;

public class TargetEsp extends Module {
    public static TargetEsp INSTANCE;

    public final ModeSetting mode = mode("Режим", "Ромб", "Ромб", "Кружок", "Crystal", "Призраки", "Призраки 2");
    public final CheckBox colorOnHit = checkbox("Краснеть при ударе", true);

    public final SliderSetting size = slider("Размер", 1.0F, 0.5F, 2.0F, 0.05F);
    public final SliderSetting rotSpeed = slider("Скорость вращения", 1.2F, 0.2F, 4.0F, 0.05F);
    public final SliderSetting opacity = slider("Прозрачность", 1.0F, 0.1F, 1.0F, 0.05F);
    public final CheckBox onHover = checkbox("При наводке", true);

    public final ModeSetting colorMode = mode("Цвет", "Тема", "Тема", "Кастом");
    public final ColorSetting customColor = color("Цвет кастом", ColorUtil.rgba(0, 220, 255, 255)).visible(() -> colorMode.is("Кастом"));

    // Textures ported from Energy
    private static final Identifier TARGET_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/target.png");
    private static final Identifier GLOW_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/glow.png");
    private static final Identifier BLOOM_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/bloom.png");

    // Pipelines
    private static final RenderPipeline TEX_ADDITIVE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_esp_energy_additive"))
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

    private static final RenderPipeline COLOR_TRIANGLES = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_esp_energy_triangles"))
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
            .withLocation(Identifier.parse("error:pipeline/world/target_esp_energy_lines"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.LINES)
            .withCull(false)
            .build();

    // Target animation & tracking
    private final Animation appearAnim = new Animation(0.0F, 0.18F);
    private LivingEntity target;
    private float spinAngle = 0.0F;
    private long lastTime = System.currentTimeMillis();

    public TargetEsp() {
        super("Target ESP", "Подсветка цели атаки из Energy", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        appearAnim.setValue(0.0F);
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

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastTime) / 1000.0F, 0.1F);
        lastTime = now;
        spinAngle = (spinAngle + dt * 140.0F * rotSpeed.get()) % 360.0F;

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
            hurtFactor = (float) Math.sin(Math.max(0.0F, this.target.hurtTime - tickDelta) * (Math.PI / 10.0D));
        }
        int redCol = ColorUtil.rgba(244, 101, 101, 255);
        int finalColor = hurtFactor > 0.01F ? ColorUtil.interpolateColor(baseCol, redCol, hurtFactor) : baseCol;

        String m = mode.getValue();
        switch (m) {
            case "Ромб" -> renderRhombus(viewMatrix, targetPos, this.target, alpha, finalColor, hurtFactor);
            case "Кружок" -> renderCircle(viewMatrix, targetPos, this.target, alpha, finalColor);
            case "Crystal" -> renderCrystal(viewMatrix, targetPos, this.target, alpha, finalColor, hurtFactor);
            case "Призраки" -> renderGhosts(viewMatrix, targetPos, this.target, alpha, finalColor, hurtFactor);
            case "Призраки 2" -> renderGhosts2(viewMatrix, targetPos, this.target, alpha, finalColor, hurtFactor, tickDelta);
        }
    }

    // 1. Mode: "Ромб" (Spinning Diamond Billboard from Energy)
    private void renderRhombus(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color, float hurtFactor) {
        AbstractTexture tex = mc.getTextureManager().getTexture(TARGET_TEX);
        if (tex == null) tex = mc.getTextureManager().getTexture(BLOOM_TEX);
        if (tex == null) return;
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        double cy = targetPos.y + e.getBbHeight() * 0.5;
        Vector4f cv = viewMatrix.transform(new Vector4f((float) targetPos.x, (float) cy, (float) targetPos.z, 1.0F));
        if (cv.z >= -0.05F) return;

        float rad = (float) Math.toRadians(spinAngle);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        float s = (0.95F + 0.15F * hurtFactor) * size.get() * 0.75F;
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

        int finalCol = ColorUtil.withAlpha(color, (int) (240 * alpha));

        try (ByteBufferBuilder mem = new ByteBufferBuilder(DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4)) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            b.addVertex(cv.x + dx0, cv.y + dy0, cv.z).setUv(0.0F, 1.0F).setColor(finalCol);
            b.addVertex(cv.x + dx1, cv.y + dy1, cv.z).setUv(1.0F, 1.0F).setColor(finalCol);
            b.addVertex(cv.x + dx2, cv.y + dy2, cv.z).setUv(1.0F, 0.0F).setColor(finalCol);
            b.addVertex(cv.x + dx3, cv.y + dy3, cv.z).setUv(0.0F, 0.0F).setColor(finalCol);

            try (MeshData mesh = b.buildOrThrow()) {
                GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Rhombus VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "Rhombus Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(TEX_ADDITIVE);
                    pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                    pass.bindTexture("Sampler0", tex.getTextureView(), sampler);
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

    // 2. Mode: "Кружок" (Oscillating Cylinder Ring with Glowing Edges)
    private void renderCircle(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color) {
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        float h = e.getBbHeight();
        double period = 1700.0 / Math.max(0.2F, rotSpeed.get() * 0.8F);
        double t = System.currentTimeMillis() % (long) period;
        double progress = t / (period * 0.5);
        if (progress > 1.0) progress = 2.0 - progress;
        progress = 0.5 - 0.5 * Math.cos(progress * Math.PI); // ease in-out sine

        double curY = targetPos.y + (h + 0.1) * progress;
        float r = e.getBbWidth() * 1.15F * size.get();

        int segs = 48;
        int colCore = ColorUtil.withAlpha(color, (int) (225 * alpha));
        int colFade = ColorUtil.withAlpha(color, 0);
        float ribbonH = 0.10F;

        try (ByteBufferBuilder mem = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i < segs; i++) {
                double a1 = Math.toRadians((i * 360.0) / segs);
                double a2 = Math.toRadians(((i + 1) * 360.0) / segs);

                double x1 = targetPos.x + Math.cos(a1) * r;
                double z1 = targetPos.z + Math.sin(a1) * r;
                double x2 = targetPos.x + Math.cos(a2) * r;
                double z2 = targetPos.z + Math.sin(a2) * r;

                Vector4f mid1 = viewMatrix.transform(new Vector4f((float) x1, (float) curY, (float) z1, 1.0F));
                Vector4f mid2 = viewMatrix.transform(new Vector4f((float) x2, (float) curY, (float) z2, 1.0F));
                Vector4f up1 = viewMatrix.transform(new Vector4f((float) x1, (float) (curY + ribbonH), (float) z1, 1.0F));
                Vector4f up2 = viewMatrix.transform(new Vector4f((float) x2, (float) (curY + ribbonH), (float) z2, 1.0F));
                Vector4f down1 = viewMatrix.transform(new Vector4f((float) x1, (float) (curY - ribbonH), (float) z1, 1.0F));
                Vector4f down2 = viewMatrix.transform(new Vector4f((float) x2, (float) (curY - ribbonH), (float) z2, 1.0F));

                if (mid1.z >= -0.05F || mid2.z >= -0.05F) continue;

                // Upper ribbon
                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(up1.x, up1.y, up1.z).setColor(colFade);
                b.addVertex(up2.x, up2.y, up2.z).setColor(colFade);

                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(up2.x, up2.y, up2.z).setColor(colFade);
                b.addVertex(mid2.x, mid2.y, mid2.z).setColor(colCore);

                // Lower ribbon
                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(down2.x, down2.y, down2.z).setColor(colFade);
                b.addVertex(down1.x, down1.y, down1.z).setColor(colFade);

                b.addVertex(mid1.x, mid1.y, mid1.z).setColor(colCore);
                b.addVertex(mid2.x, mid2.y, mid2.z).setColor(colCore);
                b.addVertex(down2.x, down2.y, down2.z).setColor(colFade);
            }

            try (MeshData mesh = b.buildOrThrow()) {
                GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Circle Ribbon VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "Circle Ribbon Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
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

    // 3. Mode: "Crystal" (Orbiting 3D Shards & Particles)
    private void renderCrystal(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color, float hurtFactor) {
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        AbstractTexture glowTex = mc.getTextureManager().getTexture(GLOW_TEX);
        if (glowTex == null) glowTex = mc.getTextureManager().getTexture(BLOOM_TEX);

        int crystals = 4;
        float r = e.getBbWidth() * 1.35F * size.get();
        double baseTime = System.currentTimeMillis() * 0.002 * rotSpeed.get();

        try (ByteBufferBuilder mem = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            int triCount = 0;

            for (int i = 0; i < crystals; i++) {
                double angle = baseTime + i * (Math.PI * 2.0 / crystals);
                double heightOff = e.getBbHeight() * 0.5 + Math.sin(angle * 2.0) * 0.28;

                double cx = targetPos.x + Math.cos(angle) * r;
                double cy = targetPos.y + heightOff;
                double cz = targetPos.z + Math.sin(angle) * r;

                float cs = 0.11F * size.get();
                float ch = 0.24F * size.get();

                float selfRot = (float) (angle * 3.0);
                float cCos = (float) Math.cos(selfRot) * cs;
                float cSin = (float) Math.sin(selfRot) * cs;

                Vector4f top = viewMatrix.transform(new Vector4f((float) cx, (float) (cy + ch), (float) cz, 1.0F));
                Vector4f bot = viewMatrix.transform(new Vector4f((float) cx, (float) (cy - ch), (float) cz, 1.0F));

                Vector4f p1 = viewMatrix.transform(new Vector4f((float) (cx + cCos), (float) cy, (float) (cz + cSin), 1.0F));
                Vector4f p2 = viewMatrix.transform(new Vector4f((float) (cx - cSin), (float) cy, (float) (cz + cCos), 1.0F));
                Vector4f p3 = viewMatrix.transform(new Vector4f((float) (cx - cCos), (float) cy, (float) (cz - cSin), 1.0F));
                Vector4f p4 = viewMatrix.transform(new Vector4f((float) (cx + cSin), (float) cy, (float) (cz - cCos), 1.0F));

                if (top.z >= -0.05F || bot.z >= -0.05F) continue;

                int col1 = ColorUtil.withAlpha(color, (int) (240 * alpha));
                int col2 = ColorUtil.withAlpha(color, (int) (180 * alpha));
                int col3 = ColorUtil.withAlpha(color, (int) (210 * alpha));
                int col4 = ColorUtil.withAlpha(color, (int) (160 * alpha));

                // Upper pyramid (4 tris)
                b.addVertex(top.x, top.y, top.z).setColor(col1);
                b.addVertex(p1.x, p1.y, p1.z).setColor(col1);
                b.addVertex(p2.x, p2.y, p2.z).setColor(col1);

                b.addVertex(top.x, top.y, top.z).setColor(col2);
                b.addVertex(p2.x, p2.y, p2.z).setColor(col2);
                b.addVertex(p3.x, p3.y, p3.z).setColor(col2);

                b.addVertex(top.x, top.y, top.z).setColor(col3);
                b.addVertex(p3.x, p3.y, p3.z).setColor(col3);
                b.addVertex(p4.x, p4.y, p4.z).setColor(col3);

                b.addVertex(top.x, top.y, top.z).setColor(col4);
                b.addVertex(p4.x, p4.y, p4.z).setColor(col4);
                b.addVertex(p1.x, p1.y, p1.z).setColor(col4);

                // Lower pyramid (4 tris)
                b.addVertex(bot.x, bot.y, bot.z).setColor(col1);
                b.addVertex(p2.x, p2.y, p2.z).setColor(col1);
                b.addVertex(p1.x, p1.y, p1.z).setColor(col1);

                b.addVertex(bot.x, bot.y, bot.z).setColor(col2);
                b.addVertex(p3.x, p3.y, p3.z).setColor(col2);
                b.addVertex(p2.x, p2.y, p2.z).setColor(col2);

                b.addVertex(bot.x, bot.y, bot.z).setColor(col3);
                b.addVertex(p4.x, p4.y, p4.z).setColor(col3);
                b.addVertex(p3.x, p3.y, p3.z).setColor(col3);

                b.addVertex(bot.x, bot.y, bot.z).setColor(col4);
                b.addVertex(p1.x, p1.y, p1.z).setColor(col4);
                b.addVertex(p4.x, p4.y, p4.z).setColor(col4);

                triCount += 24;
            }

            if (triCount > 0) {
                try (MeshData mesh = b.buildOrThrow()) {
                    GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Crystal Triangles VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                            () -> "Crystal Triangles Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
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

    // 4. Mode: "Призраки" (Ghost trails spiraling in 3D)
    private void renderGhosts(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color, float hurtFactor) {
        AbstractTexture tex = mc.getTextureManager().getTexture(GLOW_TEX);
        if (tex == null) tex = mc.getTextureManager().getTexture(BLOOM_TEX);
        if (tex == null) return;
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        double cy = targetPos.y + e.getBbHeight() * 0.5;
        double radius = e.getBbWidth() * 1.2F * size.get();
        long now = System.currentTimeMillis();

        try (ByteBufferBuilder mem = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int quadCount = 0;

            int strands = 3;
            int nodes = 12;

            for (int s = 0; s < strands; s++) {
                double strandOffset = (s * Math.PI * 2.0) / strands;
                for (int i = 0; i < nodes; i++) {
                    float tail = 1.0F - (float) i / nodes;
                    double angle = (now * 0.003 * rotSpeed.get()) + strandOffset - (i * 0.10);
                    double yWave = Math.sin(angle * 1.5) * 0.35 + (s * 0.15 - 0.15);

                    double px = targetPos.x + Math.sin(angle) * radius;
                    double py = cy + yWave;
                    double pz = targetPos.z + Math.cos(angle) * radius;

                    Vector4f cv = viewMatrix.transform(new Vector4f((float) px, (float) py, (float) pz, 1.0F));
                    if (cv.z >= -0.05F) continue;

                    float hs = (0.07F + 0.13F * tail) * size.get();
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
                    GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Ghosts VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                    GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                            () -> "Ghosts Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                        pass.setPipeline(TEX_ADDITIVE);
                        pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        pass.bindTexture("Sampler0", tex.getTextureView(), sampler);
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

    // 5. Mode: "Призраки 2" (Energy 1:1 4-strand Helical Orbital Glow Mesh)
    private void renderGhosts2(Matrix4f viewMatrix, Vec3 targetPos, LivingEntity e, float alpha, int color, float hurtFactor, float tickDelta) {
        AbstractTexture tex = mc.getTextureManager().getTexture(GLOW_TEX);
        if (tex == null) tex = mc.getTextureManager().getTexture(BLOOM_TEX);
        if (tex == null) return;
        var renderTarget = mc.gameRenderer.mainRenderTarget();
        if (renderTarget == null) return;

        double width = e.getBbWidth() * size.get();
        float baseH = e.getBbHeight() / 2.0F + 0.2F;
        double timeSec = (System.currentTimeMillis() / 1000.0) * rotSpeed.get();
        double rotSpeedFactor = (timeSec * 3.0) % (Math.PI * 2.0);
        double angleStep = Math.toRadians(60.0) / 14.0;

        try (ByteBufferBuilder mem = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(mem, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int quadCount = 0;

            for (int strand = 0; strand < 4; strand++) {
                double strandTime = timeSec + strand * 15.0;
                double strandAngleOffset = strand * (Math.PI / 2.0);

                for (int node = 0; node <= 14; node++) {
                    double nodeAngle = node * angleStep;
                    double totalAngle = nodeAngle + rotSpeedFactor + strandAngleOffset;

                    float px = (float) (targetPos.x + width * Math.cos(totalAngle));
                    float py = (float) (targetPos.y + baseH + Math.sin(strandTime + nodeAngle + strand) * 0.7);
                    float pz = (float) (targetPos.z + width * Math.sin(totalAngle));

                    Vector4f cv = viewMatrix.transform(new Vector4f(px, py, pz, 1.0F));
                    if (cv.z >= -0.05F) continue;

                    float scale = 0.4F * (0.4F + (float) node / 14.0F) * size.get() * 0.65F;
                    float hs = scale * 0.5F;

                    int nodeAlpha = (int) (240.0F * (1.0F - (float) node / 18.0F) * alpha);
                    int nodeColor = ColorUtil.withAlpha(color, nodeAlpha);

                    b.addVertex(cv.x - hs, cv.y - hs, cv.z).setUv(0.0F, 1.0F).setColor(nodeColor);
                    b.addVertex(cv.x + hs, cv.y - hs, cv.z).setUv(1.0F, 1.0F).setColor(nodeColor);
                    b.addVertex(cv.x + hs, cv.y + hs, cv.z).setUv(1.0F, 0.0F).setColor(nodeColor);
                    b.addVertex(cv.x - hs, cv.y + hs, cv.z).setUv(0.0F, 0.0F).setColor(nodeColor);
                    quadCount++;
                }
            }

            if (quadCount > 0) {
                try (MeshData mesh = b.buildOrThrow()) {
                    GpuBuffer vb = RenderSystem.getDevice().createBuffer(() -> "Ghosts2 VB", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                    GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                    try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                            () -> "Ghosts2 Pass", renderTarget.getColorTextureView(), Optional.empty(), renderTarget.getDepthTextureView(), OptionalDouble.empty())) {
                        pass.setPipeline(TEX_ADDITIVE);
                        pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                        pass.bindTexture("Sampler0", tex.getTextureView(), sampler);
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
}
