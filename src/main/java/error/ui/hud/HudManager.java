package error.ui.hud;

import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import error.IMinecraft;
import error.event.EventTarget;
import error.event.list.MouseInputEvent;
import error.event.list.Render2DEvent;
import error.ui.hud.impl.*;

import error.util.client.clients.ColorUtil;
import error.util.display.batch.DisplayBatcher;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;

import java.util.ArrayList;
import java.util.List;

/**
 */
public final class HudManager implements IMinecraft {
    private static final HudManager INSTANCE = new HudManager();
    private final List<HudElement> elements = new ArrayList<>();

    private static final float GRID_STEP = 10.0F;
    private static final float SNAP_DISTANCE = 5.0F;
    private static final float COLLISION_MARGIN = 2.0F;

    private HudElement draggedElement = null;
    private boolean collisionsEnabled = false;
    private boolean snappingEnabled = true;
    private boolean showGuidelines = true;

    private float activeSnapX = -1.0F;
    private float activeSnapY = -1.0F;

    private HudElement contextMenuElement = null;
    private boolean contextMenuOpen = false;
    private float contextMenuX = 0.0F;
    private float contextMenuY = 0.0F;
    private final Animation menuFadeAnim = new Animation(0.0F, 0.22F);

    private HudManager() {
        register(new DynamicIslandHud());
        register(new GpsHud());
        register(new ArmorHud());
        register(new TargetHud());
        register(new KeyBindsHud());
        register(new PotionsHud());
        register(new ServerHelperHud());
        register(new CustomHotbarHud());
        register(new CustomScoreboardHud());
        register(new CooldownHud());
        register(new StaffHud());
        register(new ArrayListHud());
    }

    public static HudManager getInstance() {
        return INSTANCE;
    }

    public void register(HudElement element) {
        this.elements.add(element);
    }

    public List<HudElement> getElements() {
        return elements;
    }

    public void setCollisionsEnabled(boolean enabled) { this.collisionsEnabled = enabled; }
    public void setSnappingEnabled(boolean enabled) { this.snappingEnabled = enabled; }
    public void setShowGuidelines(boolean show) { this.showGuidelines = show; }

    public boolean isCollisionsEnabled() { return collisionsEnabled; }
    public boolean isSnappingEnabled() { return snappingEnabled; }

    public boolean isDraggableScreenOpen() {
        return mc.gui != null && mc.gui.screen() instanceof ChatScreen;
    }

    public static double getMouseX() {
        return mc.mouseHandler.xpos() / (double) mc.getWindow().getGuiScale();
    }

    public static double getMouseY() {
        return mc.mouseHandler.ypos() / (double) mc.getWindow().getGuiScale();
    }

    private long lastFrameTime = 0L;
    private final Animation snapAlphaX = new Animation(0.0F, 0.15F);
    private final Animation snapAlphaY = new Animation(0.0F, 0.15F);

    @EventTarget(priority = 500)
    public void onRender2D(Render2DEvent event) {
        if (mc == null || mc.getWindow() == null || error.module.impl.misc.UnHook.unhooked) return;

        long currentTime = System.nanoTime();
        if (lastFrameTime <= 0L) lastFrameTime = currentTime;
        float dt = (currentTime - lastFrameTime) / 1_000_000_000.0F;
        lastFrameTime = currentTime;
        dt = Math.min(Math.max(dt, 0.001F), 0.05F);

        for (HudElement element : elements) {
            element.updatePhysics(dt);
        }

        boolean isEditMode = isDraggableScreenOpen();
        double mouseX = getMouseX();
        double mouseY = getMouseY();

        if (isEditMode) {
            activeSnapX = -1.0F;
            activeSnapY = -1.0F;

            if (draggedElement != null && draggedElement.isDragging()) {
                updateDraggingPosition(draggedElement, mouseX, mouseY);
            }

            if (showGuidelines) {
                float screenW = mc.getWindow().getGuiScaledWidth();
                float screenH = mc.getWindow().getGuiScaledHeight();

                snapAlphaX.setTarget(activeSnapX >= 0 ? 1.0F : 0.0F);
                snapAlphaX.update();
                snapAlphaY.setTarget(activeSnapY >= 0 ? 1.0F : 0.0F);
                snapAlphaY.update();

                if (snapAlphaX.getValue() > 0.01F && activeSnapX >= 0) {
                    int lineCol = ColorUtil.rgba(255, 255, 255, (int) (140 * snapAlphaX.getValue()));
                    Render2D.drawRoundedRect(activeSnapX - 0.5F, 0.0F, 1.0F, screenH, 0.5F, lineCol);
                }
                if (snapAlphaY.getValue() > 0.01F && activeSnapY >= 0) {
                    int lineCol = ColorUtil.rgba(255, 255, 255, (int) (140 * snapAlphaY.getValue()));
                    Render2D.drawRoundedRect(0.0F, activeSnapY - 0.5F, screenW, 1.0F, 0.5F, lineCol);
                }
            }
        } else {
            snapAlphaX.setTarget(0.0F);
            snapAlphaX.setValue(0.0F);
            snapAlphaY.setTarget(0.0F);
            snapAlphaY.setValue(0.0F);
        }

        for (HudElement element : elements) {
            boolean visible = element.isEnabled() && (element.shouldRender() || isEditMode);
            element.getFadeAnim().setTarget(visible ? 1.0F : 0.0F);
            element.getFadeAnim().update();

            float animVal = element.getFadeAnim().getValue();
            if (animVal <= 0.001F) continue;

            Render2DUtil.flush();
            element.draw(event);
            DisplayBatcher.flush();

            if (isEditMode && element == draggedElement && !(element instanceof ArmorHud)) {
                Render2D.drawRoundedOutline(element.getX() - 1.0F, element.getY() - 1.0F,
                        element.getWidth() + 2.0F, element.getHeight() + 2.0F, 3.5F, 1.0F,
                        ColorUtil.rgba(255, 255, 255, 160));
            }
        }

        menuFadeAnim.setTarget((isEditMode && contextMenuOpen && contextMenuElement != null && contextMenuElement.isEnabled()) ? 1.0F : 0.0F);
        menuFadeAnim.update();

        float animVal = menuFadeAnim.getValue();
        if (animVal > 0.01F && contextMenuElement != null && contextMenuElement.isEnabled()) {
            float scale = 0.90F + 0.10F * animVal;
            if (isEditMode && GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                double localMx = (mouseX - contextMenuX) / scale;
                double localMy = (mouseY - contextMenuY) / scale;
                contextMenuElement.handleContextMenuClick(0.0F, 0.0F, localMx, localMy, 0);
            }
            var extractor = event.getGuiGraphicsExtractor();
            if (extractor != null) {
                Render2DUtil.flush();
                extractor.pose().pushMatrix();
                extractor.pose().translate(contextMenuX, contextMenuY);
                extractor.pose().scale(scale, scale);
                contextMenuElement.drawContextMenu(0.0F, 0.0F, (mouseX - contextMenuX) / scale, (mouseY - contextMenuY) / scale, animVal);
                extractor.pose().popMatrix();
            }
        } else if (animVal <= 0.01F && !contextMenuOpen) {
            contextMenuElement = null;
        }


    }

    public List<HudElement.Box> getAllCollisionBoxesExcept(Object excludeHandle) {
        List<HudElement.Box> all = new ArrayList<>();
        for (HudElement el : elements) {
            if (!el.isEnabled()) continue;
            for (HudElement.Box box : el.getCollisionBoxes()) {
                if (box.handle() != excludeHandle && el != excludeHandle) {
                    all.add(box);
                }
            }
        }
        return all;
    }

    private void updateDraggingPosition(HudElement target, double mouseX, double mouseY) {
        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();

        float desiredX = (float) mouseX - target.dragOffsetX;
        float desiredY = (float) mouseY - target.dragOffsetY;

        if (snappingEnabled) {
            desiredX = applySnappingX(target, desiredX, target.getWidth(), screenW);
            desiredY = applySnappingY(target, desiredY, target.getHeight(), screenH);
        }

        desiredX = Math.max(0.0F, Math.min(screenW - target.getWidth(), desiredX));
        desiredY = Math.max(0.0F, Math.min(screenH - target.getHeight(), desiredY));

        if (collisionsEnabled) {
            float originalX = target.getTargetX();
            float originalY = target.getTargetY();

            target.setTargetX(desiredX);
            if (checkOverlapBoxes(target.getTargetX(), target.getTargetY(), target.getWidth(), target.getHeight(), target)) {
                target.setTargetX(originalX);
            }

            target.setTargetY(desiredY);
            if (checkOverlapBoxes(target.getTargetX(), target.getTargetY(), target.getWidth(), target.getHeight(), target)) {
                target.setTargetY(originalY);
            }
        } else {
            target.setTargetX(desiredX);
            target.setTargetY(desiredY);
        }
    }

    public boolean checkOverlapBoxes(float x, float y, float w, float h, Object targetHandle) {
        for (HudElement.Box b : getAllCollisionBoxesExcept(targetHandle)) {
            if (x - COLLISION_MARGIN < b.x() + b.width() &&
                    x + w + COLLISION_MARGIN > b.x() &&
                    y - COLLISION_MARGIN < b.y() + b.height() &&
                    y + h + COLLISION_MARGIN > b.y()) {
                return true;
            }
        }
        return false;
    }

    public float applySnappingX(Object handle, float curX, float width, float screenW) {
        float centerX = curX + width / 2.0F;
        float rightX = curX + width;
        float bestX = curX;
        float minDiff = SNAP_DISTANCE;
        float snappedLine = -1.0F;

        float gridX = Math.round(curX / GRID_STEP) * GRID_STEP;
        if (Math.abs(curX - gridX) < minDiff) {
            bestX = gridX;
            minDiff = Math.abs(curX - gridX);
            snappedLine = gridX;
        }

        float[] screenTargets = {0.0F, screenW / 2.0F, screenW};
        for (float st : screenTargets) {
            if (Math.abs(curX - st) < minDiff) { bestX = st; minDiff = Math.abs(curX - st); snappedLine = st; }
            if (Math.abs(centerX - st) < minDiff) { bestX = st - width / 2.0F; minDiff = Math.abs(centerX - st); snappedLine = st; }
            if (Math.abs(rightX - st) < minDiff) { bestX = st - width; minDiff = Math.abs(rightX - st); snappedLine = st; }
        }

        for (HudElement.Box b : getAllCollisionBoxesExcept(handle)) {
            float elLeft = b.x();
            float elRight = b.x() + b.width();
            float elCenter = b.x() + b.width() / 2.0F;

            if (Math.abs(curX - elRight) < minDiff) { bestX = elRight; minDiff = Math.abs(curX - elRight); snappedLine = elRight; }
            if (Math.abs(rightX - elLeft) < minDiff) { bestX = elLeft - width; minDiff = Math.abs(rightX - elLeft); snappedLine = elLeft; }
            if (Math.abs(curX - elLeft) < minDiff) { bestX = elLeft; minDiff = Math.abs(curX - elLeft); snappedLine = elLeft; }
            if (Math.abs(centerX - elCenter) < minDiff) { bestX = elCenter - width / 2.0F; minDiff = Math.abs(centerX - elCenter); snappedLine = elCenter; }
        }

        this.activeSnapX = snappedLine;
        return bestX;
    }

    public float applySnappingY(Object handle, float curY, float height, float screenH) {
        float centerY = curY + height / 2.0F;
        float bottomY = curY + height;
        float bestY = curY;
        float minDiff = SNAP_DISTANCE;
        float snappedLine = -1.0F;

        float gridY = Math.round(curY / GRID_STEP) * GRID_STEP;
        if (Math.abs(curY - gridY) < minDiff) {
            bestY = gridY;
            minDiff = Math.abs(curY - gridY);
            snappedLine = gridY;
        }

        float[] screenTargets = {0.0F, screenH / 2.0F, screenH};
        for (float st : screenTargets) {
            if (Math.abs(curY - st) < minDiff) { bestY = st; minDiff = Math.abs(curY - st); snappedLine = st; }
            if (Math.abs(centerY - st) < minDiff) { bestY = st - height / 2.0F; minDiff = Math.abs(centerY - st); snappedLine = st; }
            if (Math.abs(bottomY - st) < minDiff) { bestY = st - height; minDiff = Math.abs(bottomY - st); snappedLine = st; }
        }

        for (HudElement.Box b : getAllCollisionBoxesExcept(handle)) {
            float elTop = b.y();
            float elBottom = b.y() + b.height();
            float elCenter = b.y() + b.height() / 2.0F;

            if (Math.abs(curY - elBottom) < minDiff) { bestY = elBottom; minDiff = Math.abs(curY - elBottom); snappedLine = elBottom; }
            if (Math.abs(bottomY - elTop) < minDiff) { bestY = elTop - height; minDiff = Math.abs(bottomY - elTop); snappedLine = elTop; }
            if (Math.abs(curY - elTop) < minDiff) { bestY = elTop; minDiff = Math.abs(curY - elTop); snappedLine = elTop; }
            if (Math.abs(centerY - elCenter) < minDiff) { bestY = elCenter - height / 2.0F; minDiff = Math.abs(centerY - elCenter); snappedLine = elCenter; }
        }

        this.activeSnapY = snappedLine;
        return bestY;
    }

    @EventTarget(priority = 500)
    public void onMouseInput(MouseInputEvent event) {
        if (!isDraggableScreenOpen()) {
            if (draggedElement != null) {
                draggedElement.stopDragging();
                draggedElement = null;
            }
            contextMenuOpen = false;
            return;
        }

        double mouseX = getMouseX();
        double mouseY = getMouseY();

        if (contextMenuOpen && contextMenuElement != null && event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float scale = 0.90F + 0.10F * menuFadeAnim.getValue();
            double localMx = (mouseX - contextMenuX) / scale;
            double localMy = (mouseY - contextMenuY) / scale;

            if (contextMenuElement.handleContextMenuClick(0.0F, 0.0F, localMx, localMy, event.getButton())) {
                event.cancel();
                return;
            }
            contextMenuOpen = false;
        }

        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudElement el = elements.get(i);
                if (!el.isEnabled()) continue;
                if (el.isHovered(mouseX, mouseY)) {
                    contextMenuElement = el;
                    contextMenuOpen = true;
                    contextMenuX = (float) mouseX;
                    contextMenuY = (float) mouseY;
                    event.cancel();
                    return;
                }
            }
            contextMenuOpen = false;
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudElement el = elements.get(i);
                if (!el.isEnabled()) continue;
                if (el instanceof DynamicIslandHud island && island.mouseClicked(mouseX, mouseY, event.getButton())) {
                    event.cancel();
                    return;
                }
                if (el.isHovered(mouseX, mouseY)) {
                    draggedElement = el;
                    el.startDragging(mouseX, mouseY);
                    event.cancel();
                    return;
                }
            }
        }

        if (event.getAction() == GLFW.GLFW_RELEASE && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (draggedElement != null) {
                draggedElement.stopDragging();
                draggedElement = null;
            }
            activeSnapX = -1.0F;
            activeSnapY = -1.0F;
        }
    }
}