package error.ui.hud;

import error.event.list.Render2DEvent;
import error.util.math.Animation;
import java.util.List;

/**
 */
public abstract class HudElement {
    protected final String id;
    protected final String name;
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected boolean enabled = true;

    protected float targetX;
    protected float targetY;

    protected boolean dragging = false;
    protected float dragOffsetX = 0.0F;
    protected float dragOffsetY = 0.0F;

    protected final Animation fadeAnim = new Animation(0.0F, 0.20F);

    public record Box(float x, float y, float width, float height, Object handle) {}

    public HudElement(String id, String name, float defaultX, float defaultY, float width, float height) {
        this(id, name, defaultX, defaultY, width, height, false);
    }

    public HudElement(String id, String name, float defaultX, float defaultY, float width, float height, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.x = defaultX;
        this.y = defaultY;
        this.targetX = defaultX;
        this.targetY = defaultY;
        this.width = width;
        this.height = height;
        this.enabled = defaultEnabled;
    }

    public abstract void draw(Render2DEvent event);

    public boolean shouldRender() {
        return enabled;
    }

    public float drawContextMenu(float menuX, float menuY, double mouseX, double mouseY, float alpha) {
        return 0.0F;
    }

    public boolean handleContextMenuClick(float menuX, float menuY, double mouseX, double mouseY, int button) {
        return false;
    }

    public List<Box> getCollisionBoxes() {
        if (width <= 0 || height <= 0) return List.of();
        return List.of(new Box(x, y, width, height, this));
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width &&
                mouseY >= this.y && mouseY <= this.y + this.height;
    }

    public void startDragging(double mouseX, double mouseY) {
        this.dragging = true;
        this.dragOffsetX = (float) mouseX - this.targetX;
        this.dragOffsetY = (float) mouseY - this.targetY;
    }

    public void stopDragging() {
        this.dragging = false;
    }

    public void updatePhysics(float dt) {
        float speed = dragging ? 28.0F : 20.0F;
        float factor = 1.0F - (float) Math.exp(-speed * dt);
        this.x += (this.targetX - this.x) * factor;
        this.y += (this.targetY - this.y) * factor;

        if (Math.abs(this.targetX - this.x) < 0.05F) this.x = this.targetX;
        if (Math.abs(this.targetY - this.y) < 0.05F) this.y = this.targetY;
    }

    public void setPosInstant(float x, float y) {
        this.x = x;
        this.targetX = x;
        this.y = y;
        this.targetY = y;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public float getX() { return x; }
    public void setX(float x) { 
        this.targetX = x; 
        if (!dragging) this.x = x;
    }
    public float getY() { return y; }
    public void setY(float y) { 
        this.targetY = y; 
        if (!dragging) this.y = y;
    }
    public float getTargetX() { return targetX; }
    public void setTargetX(float targetX) { this.targetX = targetX; }
    public float getTargetY() { return targetY; }
    public void setTargetY(float targetY) { this.targetY = targetY; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isDragging() { return dragging; }
    public Animation getFadeAnim() { return fadeAnim; }
}