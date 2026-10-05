package error.module.impl.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import error.util.render.Render3D;
import error.util.render.Render3DUtil;
import error.event.EventTarget;
import error.event.list.Render2DEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import error.util.render.Render2DUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static error.util.client.clients.Theme.BG_COLOR;
import static error.util.client.clients.Theme.DIVIDER_COLOR;

/**
 */
public final class Predictions extends Module {

    private static final float PILL_HEIGHT = 20;
    private static final float RADIUS = 6;
    private static final float PADDING = 6;
    private static final float GAP = 5;
    private static final float ITEM_SIZE = 12;
    private static final float DIVIDER_HEIGHT = 10;
    private static final float TEXT_SIZE = 10.5f;

    public final MultiModeSetting projectiles = multiMode(
            "Снаряды",
            List.of("Эндер-перл", "Стрелы", "Трезубец"),
            "Эндер-перл", "Стрелы", "Трезубец"
    );

    public final MultiModeSetting info = multiMode(
            "Инфо",
            List.of("Иконка", "Метры", "Время"),
            "Иконка", "Название", "Метры", "Время"
    );

    public final CheckBox inHand = checkbox("В руке", true);
    public final CheckBox inFlight = checkbox("Летящие", true);
    public final CheckBox drawLanding = checkbox("Точка падения", true);

    private final List<TrajectoryData> activeTrajectories = new ArrayList<>();
    private GpuBuffer vertexBuffer;

    public Predictions() {
        super("Predictions", "Отображает траекторию и место приземления снарядов", Category.RENDER);
    }

    private record ProjectileProperties(double gravity, double drag, double velocity) {}

    private record TrajectoryData(
            List<Vec3> points,
            Vec3 landingPos,
            ItemStack iconStack,
            String label,
            int ticksToLand,
            int color
    ) {}

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) return;

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        activeTrajectories.clear();

        if (this.inHand.getValue()) {
            simulatePlayerHand(mc.player, tickDelta);
        }

        if (this.inFlight.getValue()) {
            simulateWorldEntities(mc, tickDelta);
        }

        for (TrajectoryData traj : activeTrajectories) {
            if (traj.points != null && traj.points.size() > 1) {
                Render3D.drawTrajectory(traj.points, traj.color, true);
            }
            if (traj.landingPos != null && this.drawLanding.getValue()) {
                Render3D.drawLandingCircle(traj.landingPos, 0.6F, traj.color, true);
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.drawLanding.getValue() || activeTrajectories.isEmpty()) return;

        Minecraft mc = event.getClient();
        if (mc == null || mc.player == null) return;

        float guiScale = (float) mc.getWindow().getGuiScale();
        float unit = 1.0F / (guiScale > 0 ? guiScale : 1.0F);

        for (TrajectoryData traj : activeTrajectories) {
            if (traj.landingPos == null) continue;
            drawLandingTag(event, mc, traj, unit);
        }
    }

    private void drawLandingTag(Render2DEvent event, Minecraft mc, TrajectoryData traj, float unit) {
        Vec3 hitPos = traj.landingPos;
        Render3DUtil.ScreenPoint anchor =Render3DUtil.projectToScreen(mc, hitPos);
        if (anchor == null) return;

        MsdfFont font = Fonts.SF_MEDIUM;
        float textSize = TEXT_SIZE * unit;
        float itemSize = ITEM_SIZE * unit;
        float gap = GAP * unit;
        float dividerWidth = Math.max(1.0F, 1.0F * unit);
        float padding = PADDING * unit;

        boolean showIcon = this.info.isEnabled("Иконка") && !traj.iconStack.isEmpty();
        boolean showName = this.info.isEnabled("Название");
        boolean showDistance = this.info.isEnabled("Метры");
        boolean showTime = this.info.isEnabled("Время");

        String nameText = showName ? traj.label : null;
        double distance = Math.sqrt(mc.player.distanceToSqr(hitPos));
        String distText = showDistance ? String.format("%.1fm", distance) : null;
        float timeSeconds = traj.ticksToLand / 20.0F;
        String timeText = showTime ? String.format("%.1fs", timeSeconds) : null;

        float contentWidth = 0.0F;
        int elementCount = 0;

        if (showIcon) {
            contentWidth += itemSize;
            elementCount++;
        }
        if (nameText != null) {
            if (elementCount > 0) contentWidth += gap + dividerWidth + gap;
            contentWidth += font.getWidth(nameText, textSize);
            elementCount++;
        }
        if (distText != null) {
            if (elementCount > 0) contentWidth += gap + dividerWidth + gap;
            contentWidth += font.getWidth(distText, textSize);
            elementCount++;
        }
        if (timeText != null) {
            if (elementCount > 0) contentWidth += gap + dividerWidth + gap;
            contentWidth += font.getWidth(timeText, textSize);
            elementCount++;
        }

        if (elementCount == 0) return;

        float width = padding + contentWidth + padding;
        float pillHeight = PILL_HEIGHT * unit;

        float pillX = anchor.x() - width / 2.0F;
        float pillY = anchor.y() - pillHeight / 2.0F;
        float centerY = pillY + pillHeight / 2.0F;
        float textY = font.centeredTextY(centerY, textSize);

        Render2D.drawShadow(pillX, pillY, width, pillHeight, RADIUS * unit, 6.0F * unit, ColorUtil.rgba(0, 0, 0, 70));
        Render2D.drawLiquidGlass(pillX, pillY, width, pillHeight, RADIUS * unit, 1.0F, traj.color);
        Render2D.drawRoundedOutline(pillX, pillY, width, pillHeight, RADIUS * unit, 0.6F * unit, ColorUtil.rgba(255, 255, 255, 30));

        float cursor = pillX + padding;
        boolean hasPrev = false;

        if (showIcon) {
            Render2DUtil.flush();
            var extractor = event.getGuiGraphicsExtractor();
            if (extractor != null) {
                Matrix3x2fStack pose = extractor.pose();
                float itemScale = itemSize / 16.0F;
                float itemY = centerY - itemSize / 2.0F;

                pose.pushMatrix();
                pose.translate(cursor, itemY);
                pose.scale(itemScale, itemScale);
                extractor.item(traj.iconStack, 0, 0);
                pose.popMatrix();
            }
            cursor += itemSize;
            hasPrev = true;
        }

        if (nameText != null) {
            if (hasPrev) cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);
            Fonts.drawString(font, nameText, cursor, textY, textSize, ColorUtil.WHITE);
            cursor += font.getWidth(nameText, textSize);
            hasPrev = true;
        }

        if (distText != null) {
            if (hasPrev) cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);
            Fonts.drawString(font, distText, cursor, textY, textSize, ColorUtil.rgba(255, 255, 255, 255));
            cursor += font.getWidth(distText, textSize);
            hasPrev = true;
        }

        if (timeText != null) {
            if (hasPrev) cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit);
            Fonts.drawString(font, timeText, cursor, textY, textSize, ColorUtil.rgba(200, 200, 200, 255));
        }
    }

    private float drawDivider(float cursor, float centerY, float dividerWidth, float gap, float unit) {
        cursor += gap;
        float divH = DIVIDER_HEIGHT * unit;
        Render2D.drawRoundedRect(cursor, centerY - divH / 2.0F, dividerWidth, divH, 0.5F * unit, DIVIDER_COLOR);
        return cursor + dividerWidth + gap;
    }

    private void simulatePlayerHand(Player player, float tickDelta) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !isSupportedItem(held)) {
            held = player.getOffhandItem();
            if (held.isEmpty() || !isSupportedItem(held)) return;
        }

        ProjectileProperties props = getPropertiesForHeldItem(player, held);
        if (props == null) return;

        Vec3 startPos = player.getEyePosition(tickDelta).subtract(0.0D, 0.1D, 0.0D);
        float pitch = player.getXRot();
        float yaw = player.getYRot();

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        double vx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double vy = -Mth.sin(pitchRad);
        double vz = Mth.cos(yawRad) * Mth.cos(pitchRad);

        Vec3 motion = new Vec3(vx, vy, vz).normalize().scale(props.velocity);
        if (player.isPassenger() && player.getVehicle() != null) {
            motion = motion.add(player.getVehicle().getDeltaMovement());
        } else {
            motion = motion.add(player.getDeltaMovement());
        }

        simulatePhysics(startPos, motion, props.gravity, props.drag, player, getDisplayStack(held), getItemLabel(held), getItemColor(held));
    }

    private void simulateWorldEntities(Minecraft mc, float tickDelta) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.isRemoved() || !entity.isAlive()) continue;

            Identifier id = EntityType.getKey(entity.getType());
            if (id == null) continue;
            String path = id.getPath();

            Vec3 pos =Render3DUtil.interpolatedPosition(entity, tickDelta);
            Vec3 motion = entity.getDeltaMovement();
            if (motion.lengthSqr() < 0.0001D) continue;

            if (path.equals("ender_pearl") && this.projectiles.isEnabled("Эндер-перл")) {
                simulatePhysics(pos, motion, 0.03D, 0.99D, entity, new ItemStack(Items.ENDER_PEARL), "Перл", ColorUtil.rgba(180, 80, 255, 255));
            } else if ((path.equals("arrow") || path.equals("spectral_arrow")) && this.projectiles.isEnabled("Стрелы")) {
                simulatePhysics(pos, motion, 0.05D, 0.99D, entity, new ItemStack(Items.ARROW), "Стрела", ColorUtil.rgba(255, 200, 50, 255));
            } else if (path.equals("trident") && this.projectiles.isEnabled("Трезубец")) {
                simulatePhysics(pos, motion, 0.05D, 0.99D, entity, new ItemStack(Items.TRIDENT), "Трезубец", ColorUtil.rgba(60, 220, 220, 255));
            }
        }
    }

    private void simulatePhysics(Vec3 startPos, Vec3 initialMotion, double gravity, double drag, Entity source, ItemStack icon, String label, int color) {
        Minecraft mc = Minecraft.getInstance();
        double posX = startPos.x;
        double posY = startPos.y;
        double posZ = startPos.z;

        double velX = initialMotion.x;
        double velY = initialMotion.y;
        double velZ = initialMotion.z;

        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(posX, posY, posZ));
        Vec3 landingPos = null;
        int ticks = 0;

        for (int i = 0; i < 300; i++) {
            ticks++;
            double prevX = posX;
            double prevY = posY;
            double prevZ = posZ;

            posX += velX;
            posY += velY;
            posZ += velZ;

            velY -= gravity;
            velX *= drag;
            velY *= drag;
            velZ *= drag;

            Vec3 from = new Vec3(prevX, prevY, prevZ);
            Vec3 to = new Vec3(posX, posY, posZ);

            ClipContext clipContext = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source);
            BlockHitResult blockHit = mc.level.clip(clipContext);

            EntityHitResult entityHit = traceEntity(mc, source, from, to);

            if (entityHit != null) {
                landingPos = entityHit.getLocation();
                points.add(landingPos);
                break;
            }

            if (blockHit.getType() != HitResult.Type.MISS) {
                landingPos = blockHit.getLocation();
                points.add(landingPos);
                break;
            }

            points.add(to);

            if (posY < mc.level.getMinY() - 10) {
                landingPos = new Vec3(posX, mc.level.getMinY(), posZ);
                points.add(landingPos);
                break;
            }
        }

        if (landingPos == null && !points.isEmpty()) {
            landingPos = points.get(points.size() - 1);
        }

        activeTrajectories.add(new TrajectoryData(points, landingPos, icon, label, ticks, color));
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

    private boolean isSupportedItem(ItemStack stack) {
        if (stack.is(Items.ENDER_PEARL) && this.projectiles.isEnabled("Эндер-перл")) return true;
        if ((stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) && this.projectiles.isEnabled("Стрелы")) return true;
        return stack.is(Items.TRIDENT) && this.projectiles.isEnabled("Трезубец");
    }

    private ProjectileProperties getPropertiesForHeldItem(Player player, ItemStack stack) {
        if (stack.is(Items.ENDER_PEARL)) {
            return new ProjectileProperties(0.03D, 0.99D, 1.5D);
        }
        if (stack.is(Items.BOW)) {
            int useTicks = player.getTicksUsingItem();
            float pull = BowItem.getPowerForTime(useTicks);
            if (pull <= 0.1F) pull = 1.0F;
            return new ProjectileProperties(0.05D, 0.99D, pull * 3.0D);
        }
        if (stack.is(Items.CROSSBOW)) {
            if (CrossbowItem.isCharged(stack)) {
                return new ProjectileProperties(0.05D, 0.99D, 3.15D);
            }
            return null;
        }
        if (stack.is(Items.TRIDENT)) {
            int useTicks = player.getTicksUsingItem();
            float pull = (useTicks >= 10) ? 1.0F : (useTicks / 10.0F);
            if (pull <= 0.1F) pull = 1.0F;
            return new ProjectileProperties(0.05D, 0.99D, pull * 2.5D);
        }
        return null;
    }

    private ItemStack getDisplayStack(ItemStack held) {
        if (held.is(Items.BOW) || held.is(Items.CROSSBOW)) {
            return new ItemStack(Items.ARROW);
        }
        return held;
    }

    private String getItemLabel(ItemStack stack) {
        if (stack.is(Items.ENDER_PEARL)) return "Перл";
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) return "Стрела";
        if (stack.is(Items.TRIDENT)) return "Трезубец";
        return "Снаряд";
    }

    private int getItemColor(ItemStack stack) {
        if (stack.is(Items.ENDER_PEARL)) return ColorUtil.rgba(180, 80, 255, 255);
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) return ColorUtil.rgba(255, 200, 50, 255);
        if (stack.is(Items.TRIDENT)) return ColorUtil.rgba(60, 220, 220, 255);
        return Theme.getAccentColor();
    }

    private void ensureVertexCapacity(int byteSize) {
        if (this.vertexBuffer != null && this.vertexBuffer.size() >= byteSize) return;
        if (this.vertexBuffer != null) this.vertexBuffer.close();
        int capacity = Math.max(byteSize + byteSize / 2, 64 * 1024);
        this.vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Predictions Line Buffer",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                capacity
        );
    }
}