package error.module.impl.misc;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.player.MoveUtility;
import error.util.render.Render3DUtil;
import error.util.RotationHandler;
import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Create by daun kvass
 */
public final class ProjectileHelper extends Module {

    private static final RenderPipeline TRAJECTORY_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/projectile_lines"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private final HeaderSetting aimbotHeader = header("AimBot");
    public final CheckBox aimbot = checkbox("Aim Bot", true);
    public final MultiModeSetting aimWeapons = multiMode("Weapon", "Bow", "Trident", "CrossBow").visible(aimbot::getValue);
    public final SliderSetting fov = slider("Fov", 90.0f, 10.0f, 360.0f, 5.0f).visible(aimbot::getValue);
    public final SliderSetting range = slider("Distance search", 40.0f, 10.0f, 80.0f, 1.0f).visible(aimbot::getValue);
    public final CheckBox targetPredict = checkbox("Predicts", true).visible(aimbot::getValue);
    public final ModeSetting targetType = mode("Targets", "Players", "Players", "All", "Mobs").visible(aimbot::getValue);

    private final HeaderSetting renderHeader = header("Display");
    public final CheckBox renderLine = checkbox("Trajectory", true);
    public final SliderSetting lineWidth = slider("Thickness Line", 1.2f, 0.5f, 3.0f, 0.1f).visible(renderLine::getValue);
    public final CheckBox renderLanding = checkbox("Marker landing", true);
    public final SliderSetting markerSize = slider("Scale cube", 0.35f, 0.15f, 0.8f, 0.05f).visible(renderLanding::getValue);

    private final List<Vec3> currentPoints = new ArrayList<>();
    private Vec3 currentLandingPos = null;
    private Direction hitDirection = Direction.UP;
    private boolean hitsEntity = false;
    private boolean wasAiming = false;
    private GpuBuffer vertexBuffer;

    public ProjectileHelper() {
        super("ProjectileHelper", "Помощник в да пизда", Category.MISC);
    }

    @Override
    protected void onDisable() {
        currentPoints.clear();
        currentLandingPos = null;
        hitsEntity = false;
        if (wasAiming || RotationHandler.isActive()) {
            RotationHandler.disengage("Instant");
        }
        wasAiming = false;
    }

    public record ProjectileProps(double gravity, double drag, double velocity, float pitchOffset) {}

    private ProjectileProps getHeldItemProps(Player player, ItemStack stack) {
        if (stack.isEmpty()) return null;

        if (stack.getItem() instanceof BowItem) {
            int useTicks = player.getTicksUsingItem();
            float pull = BowItem.getPowerForTime(useTicks);
            if (pull <= 0.1F) pull = 1.0F;
            return new ProjectileProps(0.05D, 0.99D, pull * 3.0D, 0.0F);
        }
        if (stack.getItem() instanceof CrossbowItem) {
            if (CrossbowItem.isCharged(stack)) {
                return new ProjectileProps(0.05D, 0.99D, 3.15D, 0.0F);
            }
            return null;
        }
        if (stack.getItem() instanceof TridentItem) {
            int useTicks = player.getTicksUsingItem();
            float pull = (useTicks >= 10) ? 1.0F : (useTicks / 10.0F);
            if (pull <= 0.1F) pull = 1.0F;
            return new ProjectileProps(0.05D, 0.99D, pull * 2.5D, 0.0F);
        }
        if (stack.is(Items.ENDER_PEARL) || stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) {
            return new ProjectileProps(0.03D, 0.99D, 1.5D, 0.0F);
        }
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.EXPERIENCE_BOTTLE)) {
            return new ProjectileProps(0.05D, 0.99D, 0.5D, -20.0F);
        }
        if (stack.is(Items.WIND_CHARGE)) {
            return new ProjectileProps(0.00D, 1.0D, 1.5D, 0.0F);
        }

        return null;
    }

    private ItemStack getValidHeldItem(Player player) {
        ItemStack main = player.getMainHandItem();
        if (getHeldItemProps(player, main) != null) return main;
        ItemStack off = player.getOffhandItem();
        if (getHeldItemProps(player, off) != null) return off;
        return ItemStack.EMPTY;
    }

    private boolean isWeaponCharging(Player player, ItemStack stack) {
        if (stack.getItem() instanceof BowItem && aimWeapons.isEnabled("Bow")) {
            return player.isUsingItem();
        }
        if (stack.getItem() instanceof TridentItem && aimWeapons.isEnabled("Trident")) {
            return player.isUsingItem();
        }
        if (stack.getItem() instanceof CrossbowItem && aimWeapons.isEnabled("CrossBow")) {
            return CrossbowItem.isCharged(stack) && mc.options.keyUse.isDown();
        }
        return false;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null) return;

        LocalPlayer player = player();
        ItemStack held = getValidHeldItem(player);

        boolean isCharging = !held.isEmpty() && isWeaponCharging(player, held);

        if (isCharging && aimbot.getValue()) {
            ProjectileProps props = getHeldItemProps(player, held);
            LivingEntity target = findAimbotTarget();

            if (props != null && target != null) {
                Vec3 startEye = player.getEyePosition();
                Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.55, 0);

                if (targetPredict.getValue()) {
                    double distance = startEye.distanceTo(targetPos);
                    double travelTicks = distance / Math.max(props.velocity, 0.1);
                    Vec3 motion = target.getDeltaMovement();
                    targetPos = targetPos.add(motion.scale(travelTicks));
                }

                float[] angles = calculateTrajectoryAngles(startEye, targetPos, props);
                if (angles != null) {
                    float fovDiff = getFovDifference(angles[0], angles[1]);
                    if (fovDiff <= this.fov.getValue() / 2.0F) {
                        RotationHandler.setRotation(angles[0], angles[1]);
                        wasAiming = true;
                        return;
                    }
                }
            }
        }

        if (wasAiming) {
            wasAiming = false;
            RotationHandler.disengage("Smooth");
        }
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;

        if (wasAiming && RotationHandler.isActive()) {
            MoveUtility.fixMovement(event, RotationHandler.getFreeYaw());
        }
    }

    private LivingEntity findAimbotTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;

        LivingEntity best = null;
        float minFov = Float.MAX_VALUE;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living) || e == mc.player || !living.isAlive()) continue;

            if (targetType.is("Players") && !(living instanceof Player)) continue;
            if (targetType.is("Mobs") && !(living instanceof Monster)) continue;

            double dist = mc.player.distanceTo(living);
            if (dist > range.getValue()) continue;

            Vec3 diff = living.position().subtract(mc.player.position());
            float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
            float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));

            float fovDiff = getFovDifference(yaw, pitch);
            if (fovDiff <= this.fov.getValue() / 2.0F && fovDiff < minFov) {
                minFov = fovDiff;
                best = living;
            }
        }
        return best;
    }

    private float getFovDifference(float targetYaw, float targetPitch) {
        float curYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player().getYRot();
        float curPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player().getXRot();
        float deltaYaw = Math.abs(Mth.wrapDegrees(targetYaw - curYaw));
        float deltaPitch = Math.abs(targetPitch - curPitch);
        return (float) Math.hypot(deltaYaw, deltaPitch);
    }

    private float[] calculateTrajectoryAngles(Vec3 from, Vec3 to, ProjectileProps props) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double distHoriz = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;

        double v = props.velocity;
        double g = props.gravity;

        if (g <= 0.0001) {
            float pitch = (float) -Math.toDegrees(Math.atan2(dy, distHoriz));
            return new float[]{yaw, pitch};
        }

        double v2 = v * v;
        double v4 = v2 * v2;
        double root = v4 - g * (g * distHoriz * distHoriz + 2.0 * dy * v2);

        if (root < 0) return null;

        double pitchRad = Math.atan((v2 - Math.sqrt(root)) / (g * distHoriz));
        float pitch = (float) -Math.toDegrees(pitchRad) + props.pitchOffset;

        return new float[]{yaw, pitch};
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

        ItemStack held = getValidHeldItem(mc.player);
        if (held.isEmpty()) {
            currentPoints.clear();
            currentLandingPos = null;
            return;
        }

        ProjectileProps props = getHeldItemProps(mc.player, held);
        if (props == null) return;

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        simulatePhysics(mc.player, props, tickDelta);

        if (currentPoints.size() < 2) return;

        var target = mc.gameRenderer.mainRenderTarget();
        if (target == null || target.getColorTextureView() == null || target.getDepthTextureView() == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();
        Matrix4f viewPose = Render3DUtil.cameraViewPose(camera);

        int mainColor = hitsEntity ? ColorUtil.rgba(255, 45, 45, 255) : Theme.getAccentColor();

        BufferBuilder builder = new BufferBuilder(
                new ByteBufferBuilder(Math.max(currentPoints.size() * 120 * DefaultVertexFormat.POSITION_COLOR.getVertexSize(), 8192)),
                PrimitiveTopology.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR
        );

        int drawnVertices = 0;

        if (renderLine.getValue()) {
            float linePx = 0.0016F * this.lineWidth.getValue();

            for (int i = 0; i < currentPoints.size() - 1; i++) {
                Vec3 p1 = currentPoints.get(i);
                Vec3 p2 = currentPoints.get(i + 1);

                Vector4f v1 = Render3DUtil.toViewSpace(p1, cameraPos, viewPose);
                Vector4f v2 = Render3DUtil.toViewSpace(p2, cameraPos, viewPose);

                if (v1.z > -0.05F && v2.z > -0.05F) continue;

                float progress = (float) i / (float) currentPoints.size();
                int pointColor = ColorUtil.withAlpha(mainColor, Math.max(0.45F, progress));

                drawnVertices += addScreenSpaceSegment(builder, v1, v2, linePx, pointColor);
            }
        }

        if (renderLanding.getValue() && currentLandingPos != null) {
            drawnVertices += renderSurfaceDiamond(builder, currentLandingPos, hitDirection, markerSize.getValue(), mainColor, cameraPos, viewPose);
        }

        if (drawnVertices == 0) return;

        MeshData meshData = builder.buildOrThrow();
        var device = RenderSystem.getDevice();
        try {
            ByteBuffer vertexData = meshData.vertexBuffer();
            int remainingBytes = vertexData.remaining();
            ensureVertexCapacity(remainingBytes);
            device.createCommandEncoder().writeToBuffer(this.vertexBuffer.slice(0, remainingBytes), vertexData);

            try (RenderPass pass = device.createCommandEncoder().createRenderPass(
                    () -> "Godweer Projectile Helper Trajectory",
                    target.getColorTextureView(),
                    Optional.empty(),
                    target.getDepthTextureView(),
                    OptionalDouble.empty()
            )) {
                pass.setPipeline(TRAJECTORY_PIPELINE);
                pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                pass.setVertexBuffer(0, this.vertexBuffer.slice(0, remainingBytes));
                pass.draw(drawnVertices, 1, 0, 0);
            }
        } finally {
            meshData.close();
        }
    }

    private void simulatePhysics(Player player, ProjectileProps props, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        currentPoints.clear();
        hitsEntity = false;

        float pitch = (RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot()) + props.pitchOffset;
        float yaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        double vx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double vy = -Mth.sin(pitchRad);
        double vz = Mth.cos(yawRad) * Mth.cos(pitchRad);

        Vec3 motion = new Vec3(vx, vy, vz).normalize().scale(props.velocity);

        Vec3 viewDir = new Vec3(vx, vy, vz).normalize();
        Vec3 startPos = player.getEyePosition(tickDelta).add(viewDir.scale(0.35D));

        double posX = startPos.x;
        double posY = startPos.y;
        double posZ = startPos.z;

        double velX = motion.x;
        double velY = motion.y;
        double velZ = motion.z;

        currentPoints.add(new Vec3(posX, posY, posZ));
        currentLandingPos = null;
        hitDirection = Direction.UP;

        for (int i = 0; i < 300; i++) {
            double prevX = posX;
            double prevY = posY;
            double prevZ = posZ;

            posX += velX;
            posY += velY;
            posZ += velZ;

            velY -= props.gravity;
            velX *= props.drag;
            velY *= props.drag;
            velZ *= props.drag;

            Vec3 from = new Vec3(prevX, prevY, prevZ);
            Vec3 to = new Vec3(posX, posY, posZ);

            ClipContext clipContext = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            BlockHitResult blockHit = mc.level.clip(clipContext);
            EntityHitResult entityHit = traceEntity(mc, player, from, to);

            if (entityHit != null) {
                currentLandingPos = entityHit.getLocation();
                currentPoints.add(currentLandingPos);
                hitsEntity = true;
                hitDirection = Direction.UP;
                break;
            }

            if (blockHit.getType() != HitResult.Type.MISS) {
                currentLandingPos = blockHit.getLocation();
                hitDirection = blockHit.getDirection();
                currentPoints.add(currentLandingPos);
                break;
            }

            currentPoints.add(to);

            if (posY < mc.level.getMinY() - 5) {
                currentLandingPos = new Vec3(posX, mc.level.getMinY(), posZ);
                currentPoints.add(currentLandingPos);
                hitDirection = Direction.UP;
                break;
            }
        }

        if (currentLandingPos == null && !currentPoints.isEmpty()) {
            currentLandingPos = currentPoints.get(currentPoints.size() - 1);
        }
    }

    private EntityHitResult traceEntity(Minecraft mc, Entity shooter, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(0.8D);
        double minDistance = Double.MAX_VALUE;
        Entity target = null;
        Vec3 hitPos = null;

        for (Entity entity : mc.level.getEntities(shooter, box, e -> !e.isSpectator() && e.isPickable() && e != shooter)) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3D);
            Optional<Vec3> clip = entityBox.clip(start, end);
            if (clip.isPresent()) {
                double dist = start.distanceToSqr(clip.get());
                if (dist < minDistance) {
                    minDistance = dist;
                    target = entity;
                    hitPos = clip.get();
                }
            }
        }
        return target != null ? new EntityHitResult(target, hitPos) : null;
    }

    private static int addScreenSpaceSegment(BufferBuilder builder, Vector4f v1, Vector4f v2, float thickness, int color) {
        float dx = v2.x - v1.x;
        float dy = v2.y - v1.y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.00001F) return 0;

        float nx = (-dy / len);
        float ny = (dx / len);

        float w1 = Math.max(0.0001F, -v1.z) * thickness;
        float w2 = Math.max(0.0001F, -v2.z) * thickness;

        builder.addVertex(v1.x + nx * w1, v1.y + ny * w1, v1.z).setColor(color);
        builder.addVertex(v1.x - nx * w1, v1.y - ny * w1, v1.z).setColor(color);
        builder.addVertex(v2.x - nx * w2, v2.y - ny * w2, v2.z).setColor(color);

        builder.addVertex(v1.x + nx * w1, v1.y + ny * w1, v1.z).setColor(color);
        builder.addVertex(v2.x - nx * w2, v2.y - ny * w2, v2.z).setColor(color);
        builder.addVertex(v2.x + nx * w2, v2.y + ny * w2, v2.z).setColor(color);

        return 6;
    }

    /**
     * Отрисовка плоского ромбика с правильной ориентацией на любой плоскости (пол, потолок, стены)
     */
    private static int renderSurfaceDiamond(BufferBuilder builder, Vec3 pos, Direction dir, float size, int color, Vec3 cameraPos, Matrix4f viewPose) {
        Vec3 offsetPos = pos.add(
                dir.getStepX() * 0.012D,
                dir.getStepY() * 0.012D,
                dir.getStepZ() * 0.012D
        );

        float s = size * 0.5F;
        Vec3 p0, p1, p2, p3;

        switch (dir.getAxis()) {
            case Y -> {
                p0 = offsetPos.add(-s, 0, 0);
                p1 = offsetPos.add(0, 0, s);
                p2 = offsetPos.add(s, 0, 0);
                p3 = offsetPos.add(0, 0, -s);
            }
            case Z -> {
                p0 = offsetPos.add(-s, 0, 0);
                p1 = offsetPos.add(0, s, 0);
                p2 = offsetPos.add(s, 0, 0);
                p3 = offsetPos.add(0, -s, 0);
            }
            case X -> {
                p0 = offsetPos.add(0, 0, -s);
                p1 = offsetPos.add(0, s, 0);
                p2 = offsetPos.add(0, 0, s);
                p3 = offsetPos.add(0, -s, 0);
            }
            default -> {
                p0 = offsetPos.add(-s, 0, 0);
                p1 = offsetPos.add(0, 0, s);
                p2 = offsetPos.add(s, 0, 0);
                p3 = offsetPos.add(0, 0, -s);
            }
        }

        Vector4f v0 = Render3DUtil.toViewSpace(p0, cameraPos, viewPose);
        Vector4f v1 = Render3DUtil.toViewSpace(p1, cameraPos, viewPose);
        Vector4f v2 = Render3DUtil.toViewSpace(p2, cameraPos, viewPose);
        Vector4f v3 = Render3DUtil.toViewSpace(p3, cameraPos, viewPose);

        int vertices = 0;

        int fillColor = ColorUtil.withAlpha(color, 0.22F);
        builder.addVertex(v0.x, v0.y, v0.z).setColor(fillColor);
        builder.addVertex(v1.x, v1.y, v1.z).setColor(fillColor);
        builder.addVertex(v2.x, v2.y, v2.z).setColor(fillColor);

        builder.addVertex(v0.x, v0.y, v0.z).setColor(fillColor);
        builder.addVertex(v2.x, v2.y, v2.z).setColor(fillColor);
        builder.addVertex(v3.x, v3.y, v3.z).setColor(fillColor);
        vertices += 6;

        float outlinePx = 0.0014F;
        int outlineColor = ColorUtil.withAlpha(color, 0.95F);

        vertices += addScreenSpaceSegment(builder, v0, v1, outlinePx, outlineColor);
        vertices += addScreenSpaceSegment(builder, v1, v2, outlinePx, outlineColor);
        vertices += addScreenSpaceSegment(builder, v2, v3, outlinePx, outlineColor);
        vertices += addScreenSpaceSegment(builder, v3, v0, outlinePx, outlineColor);

        return vertices;
    }

    private void ensureVertexCapacity(int byteSize) {
        if (this.vertexBuffer != null && this.vertexBuffer.size() >= byteSize) return;
        if (this.vertexBuffer != null) this.vertexBuffer.close();
        int capacity = Math.max(byteSize + byteSize / 2, 64 * 1024);
        this.vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "ProjectileHelper Vertex Buffer",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                capacity
        );
    }
}