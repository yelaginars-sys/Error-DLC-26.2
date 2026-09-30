package error.ui.hud.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import error.Info;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.Tps;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Create by daun kvass
 */
public final class WatermarkHud extends HudElement implements IMinecraft {

    public enum Part {
        NAME("Name", null),
        FPS("FPS", IconUse.FPS),
        BPS("BPS", IconUse.CORD),
        TPS("Tps",IconUse.TPS),
        TIME("Time", IconUse.CLOCK),
        COORDS("Coords", IconUse.BPS),
        NICK("Nick", IconUse.PERSONS),
        PING("Ping", IconUse.PING),
        IP("IP", IconUse.GLOBE);

        final String title;
        final IconUse icon;

        Part(String title, IconUse icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    public static class Cluster {
        public final List<Part> parts = new ArrayList<>();
        public float x, y, width, height = 18.0F;
        public boolean dragging;
        public float dragOffsetX, dragOffsetY;

        public Cluster(float x, float y, Part... initialParts) {
            this.x = x;
            this.y = y;
            Collections.addAll(this.parts, initialParts);
        }

        public boolean isHovered(double mx, double my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + height;
        }
    }

    private static final float HEIGHT = 16.0F;
    private static final float PADDING_X = 6.0F;
    private static final float GAP = 5.0F;
    private static final float DETACH_THRESHOLD = 18.0F;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 185);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 140);

    private final List<Cluster> clusters = new ArrayList<>();
    private final Map<Part, Boolean> visibleMap = new EnumMap<>(Part.class);

    private final Map<Part, Animation> partScaleAnims = new EnumMap<>(Part.class);
    private final Map<Part, Animation> partPosAnims = new EnumMap<>(Part.class);

    private Part draggedPart = null;
    private Cluster sourceCluster = null;
    private float mouseX, mouseY;
    private float partStartX, partStartY;
    private boolean isDetached = false;
    private Cluster draggedCluster = null;
    private Cluster targetMergeCluster = null;

    private final Map<Part, float[]> chipBounds = new EnumMap<>(Part.class);
    private float lastMenuH = 60.0F;

    public WatermarkHud() {
        super("watermark", "Watermark", 10.0F, 10.0F, 240.0F, HEIGHT);
        for (Part p : Part.values()) {
            visibleMap.put(p, true);
            partScaleAnims.put(p, new Animation(0.0F, 0.20F));
            partPosAnims.put(p, new Animation(0.0F, 0.22F));
        }
        Cluster mainCluster = new Cluster(10.0F, 10.0F, Part.values());
        clusters.add(mainCluster);
    }

    public JsonObject writeConfig() {
        JsonObject obj = new JsonObject();

        JsonObject visObj = new JsonObject();
        for (Map.Entry<Part, Boolean> entry : visibleMap.entrySet()) {
            visObj.addProperty(entry.getKey().name(), entry.getValue());
        }
        obj.add("visibility", visObj);

        JsonArray clusterArr = new JsonArray();
        for (Cluster c : clusters) {
            JsonObject cObj = new JsonObject();
            cObj.addProperty("x", c.x);
            cObj.addProperty("y", c.y);

            JsonArray partsArr = new JsonArray();
            for (Part p : c.parts) {
                partsArr.add(p.name());
            }
            cObj.add("parts", partsArr);
            clusterArr.add(cObj);
        }
        obj.add("clusters", clusterArr);

        return obj;
    }

    public void readConfig(JsonObject obj) {
        if (obj == null) return;

        if (obj.has("visibility")) {
            JsonObject visObj = obj.getAsJsonObject("visibility");
            for (Part p : Part.values()) {
                if (visObj.has(p.name())) {
                    visibleMap.put(p, visObj.get(p.name()).getAsBoolean());
                }
            }
        }

        if (obj.has("clusters")) {
            JsonArray clusterArr = obj.getAsJsonArray("clusters");
            clusters.clear();
            for (JsonElement elem : clusterArr) {
                JsonObject cObj = elem.getAsJsonObject();
                float cx = cObj.get("x").getAsFloat();
                float cy = cObj.get("y").getAsFloat();
                Cluster c = new Cluster(cx, cy);

                if (cObj.has("parts")) {
                    for (JsonElement pElem : cObj.getAsJsonArray("parts")) {
                        try {
                            Part p = Part.valueOf(pElem.getAsString());
                            c.parts.add(p);
                        } catch (Exception ignored) {}
                    }
                }
                if (!c.parts.isEmpty()) {
                    clusters.add(c);
                }
            }

            if (clusters.isEmpty()) {
                clusters.add(new Cluster(10.0F, 10.0F, Part.values()));
            }
        }
        updateDimensions();
    }

    @Override
    public List<Box> getCollisionBoxes() {
        List<Box> list = new ArrayList<>();
        for (Cluster c : clusters) {
            if (!c.parts.isEmpty() && c.width > 0) {
                list.add(new Box(c.x, c.y, c.width, HEIGHT, c));
            }
        }
        return list;
    }

    @Override
    public void draw(Render2DEvent event) {
        if (!enabled) return;

        updateDimensions();

        for (Part p : Part.values()) {
            Animation scale = partScaleAnims.get(p);
            scale.setTarget((p == draggedPart && !isDetached) ? 1.0F : 0.0F);
            scale.update();

            Animation pos = partPosAnims.get(p);
            pos.update();
        }

        for (Cluster cluster : clusters) {
            if (cluster.parts.isEmpty()) continue;

            Render2D.drawShadow(cluster.x, cluster.y, cluster.width, HEIGHT, 5.0F, 8.0F, SHADOW_COLOR);
            Render2D.drawBlur(cluster.x, cluster.y, cluster.width, HEIGHT, 5.0F, BG_COLOR, 1.0F);

            if (cluster == targetMergeCluster) {
                Render2D.drawRoundedOutline(cluster.x - 1.0F, cluster.y - 1.0F, cluster.width + 2.0F, HEIGHT + 2.0F, 4.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 180));
            }

            float targetCursor = cluster.x + PADDING_X;
            for (Part part : cluster.parts) {
                if (!visibleMap.get(part)) continue;
                float pw = getPartWidth(part);

                Animation posAnim = partPosAnims.get(part);
                if (posAnim.getValue() == 0.0F) {
                    posAnim.setValue(targetCursor);
                }
                posAnim.setTarget(targetCursor);

                float renderX = posAnim.getValue();

                if (part == draggedPart && isDetached) {
                    Render2D.drawRoundedRect(targetCursor, cluster.y + 2.0F, pw, HEIGHT - 4.0F, 2.5F, ColorUtil.rgba(255, 255, 255, 14));
                } else {
                    renderPartInteractive(part, renderX, cluster.y);
                }
                targetCursor += pw + GAP;
            }
        }

        if (draggedPart != null && isDetached) {
            float pw = getPartWidth(draggedPart);
            float dx = mouseX - pw / 2.0F;
            float dy = mouseY - HEIGHT / 2.0F;

            Render2D.drawShadow(dx, dy, pw + 6.0F, HEIGHT, 5.0F, 8.0F, SHADOW_COLOR);
            Render2D.drawBlur(dx, dy, pw + 6.0F, HEIGHT, 5.0F, ColorUtil.rgba(20, 20, 28, 240), 1.0F);
            renderPart(draggedPart, dx + 3.0F, dy, 8.5F, 1.0F);
        }
    }

    private void renderPartInteractive(Part part, float px, float py) {
        float scaleProgress = partScaleAnims.get(part).getValue();
        float fontSize = 8.5F + 0.6F * scaleProgress;
        float yOffset = -0.5F * scaleProgress;

        if (scaleProgress > 0.01F) {
            float pw = getPartWidth(part);
            Render2D.drawRoundedRect(px - 1.5F, py + 1.5F, pw + 3.0F, HEIGHT - 3.0F, 2.5F, ColorUtil.rgba(255, 255, 255, (int) (18 * scaleProgress)));
        }

        renderPart(part, px, py + yOffset, fontSize, scaleProgress);
    }

    private void renderPart(Part part, float px, float py, float fontSize, float scaleProgress) {
        String text = getPartValue(part);
        int baseColor = part == Part.NAME ? ColorUtil.multiplyAlpha(Theme.getAccentColor(), 200) : ColorUtil.rgba(215, 220, 230, 255);
        int activeColor = Theme.getAccentColor();
        int textColor = ColorUtil.lerp(baseColor, activeColor, scaleProgress);
        float textY = py + 3.5F;

        if (part.icon != null) {
            int iconCol = ColorUtil.withAlpha(Theme.getAccentColor(), 240);
            Fonts.drawIcon(part.icon, px + 1.0F, py + 4.5F, 8.0F, iconCol);
            Fonts.drawString(Fonts.SF_MEDIUM, text, px + 11.0F, textY, fontSize, textColor);
        } else {
            Fonts.drawString(Fonts.SF_MEDIUM, text, px + 1.5F, textY, fontSize, textColor);
        }
    }

    private void updateDimensions() {
        clusters.removeIf(c -> c.parts.isEmpty());

        for (Cluster c : clusters) {
            float calcW = PADDING_X * 2.0F;
            int count = 0;
            for (Part p : c.parts) {
                if (visibleMap.get(p)) {
                    calcW += getPartWidth(p) + GAP;
                    count++;
                }
            }
            if (count > 0) calcW -= GAP;
            c.width = count == 0 ? 0 : Math.max(16.0F, calcW);
        }

        if (!clusters.isEmpty()) {
            this.x = clusters.get(0).x;
            this.y = clusters.get(0).y;
            this.width = clusters.get(0).width;
        } else {
            this.width = 0;
        }
    }

    public List<Cluster> getClusters() { return clusters; }

    private float getPartWidth(Part p) {
        float iconW = (p.icon != null) ? 11.0F : 3.0F;
        return iconW + Fonts.SF_MEDIUM.getWidth(getPartValue(p), 8.5F) + 2.0F;
    }

    private String getPartValue(Part p) {
        if (mc.player == null) return p.title;
        return switch (p) {
            case NAME -> Info.NAME;
            case FPS -> mc.getFps() + " fps";
            case BPS -> {
                double dx = mc.player.getX() - mc.player.xo;
                double dz = mc.player.getZ() - mc.player.zo;
                double speed = Math.hypot(dx, dz) * 20.0D;
                yield String.format(Locale.US, "%.1f bps", speed);
            }
            case TPS -> String.format(Locale.US, "%.1f tps",Mth.clamp(Tps.INSTANCE.effectiveTps(), 1.0F, 20.0F));
            case TIME -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            case COORDS -> String.format("%.0f, %.0f, %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            case NICK -> mc.player.getGameProfile().name();
            case PING -> getPing() + "ms";
            case IP -> getServerAddress();
        };
    }

    private int getPing() {
        if (mc.getConnection() == null || mc.player == null) return 0;
        var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info != null ? info.getLatency() : 0;
    }

    private String getServerAddress() {
        ServerData data = mc.getCurrentServer();
        if (data == null) return "Singleplayer";
        String ip = data.ip;
        return ip.length() > 16 ? ip.substring(0, 15) + ".." : ip;
    }

    public Cluster getClusterAt(double mx, double my) {
        for (Cluster c : clusters) {
            if (c.isHovered(mx, my)) return c;
        }
        return null;
    }

    public Part getPartAt(Cluster c, double mx, double my) {
        if (c == null) return null;
        float cursor = c.x + PADDING_X;
        for (Part p : c.parts) {
            if (!visibleMap.get(p)) continue;
            float pw = getPartWidth(p);
            if (mx >= cursor && mx <= cursor + pw) return p;
            cursor += pw + GAP;
        }
        return null;
    }

    public void onMousePress(double mx, double my, int button) {
        Cluster c = getClusterAt(mx, my);
        if (c == null) return;

        if (c.parts.size() <= 1) {
            this.draggedCluster = c;
            c.dragging = true;
            c.dragOffsetX = (float) mx - c.x;
            c.dragOffsetY = (float) my - c.y;
            return;
        }

        Part p = getPartAt(c, mx, my);
        if (p != null) {
            this.draggedPart = p;
            this.sourceCluster = c;
            this.partStartX = (float) mx;
            this.partStartY = (float) my;
            this.isDetached = false;
        } else {
            this.draggedCluster = c;
            c.dragging = true;
            c.dragOffsetX = (float) mx - c.x;
            c.dragOffsetY = (float) my - c.y;
        }
    }

    public void onMouseMove(double mx, double my) {
        this.mouseX = (float) mx;
        this.mouseY = (float) my;
        HudManager hm = HudManager.getInstance();

        if (draggedCluster != null) {
            float screenW = mc.getWindow().getGuiScaledWidth();
            float screenH = mc.getWindow().getGuiScaledHeight();

            float desiredX = (float) mx - draggedCluster.dragOffsetX;
            float desiredY = (float) my - draggedCluster.dragOffsetY;

            if (hm.isSnappingEnabled()) {
                desiredX = hm.applySnappingX(draggedCluster, desiredX, draggedCluster.width, screenW);
                desiredY = hm.applySnappingY(draggedCluster, desiredY, HEIGHT, screenH);
            }

            desiredX = Math.max(0.0F, Math.min(screenW - draggedCluster.width, desiredX));
            desiredY = Math.max(0.0F, Math.min(screenH - HEIGHT, desiredY));

            if (hm.isCollisionsEnabled()) {
                float oldX = draggedCluster.x;
                float oldY = draggedCluster.y;

                draggedCluster.x = desiredX;
                if (hm.checkOverlapBoxes(draggedCluster.x, draggedCluster.y, draggedCluster.width, HEIGHT, draggedCluster)) {
                    draggedCluster.x = oldX;
                }

                draggedCluster.y = desiredY;
                if (hm.checkOverlapBoxes(draggedCluster.x, draggedCluster.y, draggedCluster.width, HEIGHT, draggedCluster)) {
                    draggedCluster.y = oldY;
                }
            } else {
                draggedCluster.x = desiredX;
                draggedCluster.y = desiredY;
            }

            targetMergeCluster = null;
            for (Cluster other : clusters) {
                if (other == draggedCluster) continue;
                boolean yClose = Math.abs(draggedCluster.y - other.y) < 14.0F;
                boolean xOverlap = (draggedCluster.x + draggedCluster.width >= other.x - 12.0F) &&
                        (draggedCluster.x <= other.x + other.width + 12.0F);

                if (yClose && xOverlap) {
                    targetMergeCluster = other;
                    break;
                }
            }
            return;
        }

        if (draggedPart != null && sourceCluster != null) {
            float distY = Math.abs((float) my - partStartY);
            float distX = Math.abs((float) mx - partStartX);

            if (distY < DETACH_THRESHOLD && mx >= sourceCluster.x - 6.0F && mx <= sourceCluster.x + sourceCluster.width + 6.0F) {
                this.isDetached = false;
                float cursor = sourceCluster.x + PADDING_X;
                for (int i = 0; i < sourceCluster.parts.size(); i++) {
                    Part other = sourceCluster.parts.get(i);
                    float pw = getPartWidth(other);
                    if (mx > cursor && mx < cursor + pw && other != draggedPart) {
                        sourceCluster.parts.remove(draggedPart);
                        sourceCluster.parts.add(i, draggedPart);
                        break;
                    }
                    cursor += pw + GAP;
                }
            } else if (distY >= DETACH_THRESHOLD || distX > sourceCluster.width) {
                this.isDetached = true;
                targetMergeCluster = getClusterAt(mx, my);
            }
        }
    }

    public void onMouseRelease(double mx, double my) {
        if (draggedCluster != null) {
            draggedCluster.dragging = false;

            if (targetMergeCluster != null && targetMergeCluster != draggedCluster) {
                if (draggedCluster.x < targetMergeCluster.x) {
                    targetMergeCluster.parts.addAll(0, draggedCluster.parts);
                    targetMergeCluster.x = draggedCluster.x;
                } else {
                    targetMergeCluster.parts.addAll(draggedCluster.parts);
                }
                clusters.remove(draggedCluster);
            }
            draggedCluster = null;
            targetMergeCluster = null;
        }

        if (draggedPart != null && sourceCluster != null) {
            if (isDetached) {
                Cluster targetCluster = getClusterAt(mx, my);
                if (targetCluster != null && targetCluster != sourceCluster) {
                    sourceCluster.parts.remove(draggedPart);
                    targetCluster.parts.add(draggedPart);
                } else if (targetCluster == null) {
                    sourceCluster.parts.remove(draggedPart);
                    Cluster newCluster = new Cluster((float) mx - 15.0F, (float) my - HEIGHT / 2.0F, draggedPart);
                    clusters.add(newCluster);
                }
            }
            draggedPart = null;
            sourceCluster = null;
            targetMergeCluster = null;
            isDetached = false;
        }
    }

    @Override
    public float drawContextMenu(float menuX, float menuY, double mouseX, double mouseY, float alpha) {
        chipBounds.clear();
        float menuW = 145.0F;

        float startX = menuX + 6.0F;
        float curX = startX;
        float curY = menuY + 16.0F;
        float chipH = 12.0F;
        float maxRowW = menuW - 12.0F;
        float fontSize = 8.5F;
        float padX = 4.5F;

        for (Part p : Part.values()) {
            float textW = Fonts.SF_MEDIUM.getWidth(p.title, fontSize) + (p.icon != null ? 10.0F : 0.0F);
            float chipW = textW + (padX * 2.0F);
            if (curX + chipW > startX + maxRowW) {
                curX = startX;
                curY += chipH + 3.0F;
            }
            curX += chipW + 3.0F;
        }
        float menuH = (curY - menuY) + chipH + 6.0F;
        this.lastMenuH = menuH;

        Render2D.drawShadow(menuX, menuY, menuW, menuH, 5.0F, 8.0F, ColorUtil.multiplyAlpha(SHADOW_COLOR, alpha));
        Render2D.drawBlur(menuX, menuY, menuW, menuH, 5.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(14, 14, 18, 240), alpha), 1.0F);

        Fonts.drawString(Fonts.SF_MEDIUM, "Elements", menuX + 6.0F, menuY + 4.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));

        curX = startX;
        curY = menuY + 16.0F;

        for (Part p : Part.values()) {
            boolean active = visibleMap.get(p);
            float textW = Fonts.SF_MEDIUM.getWidth(p.title, fontSize) + (p.icon != null ? 10.0F : 0.0F);
            float chipW = textW + (padX * 2.0F);

            if (curX + chipW > startX + maxRowW) {
                curX = startX;
                curY += chipH + 3.0F;
            }

            chipBounds.put(p, new float[]{curX, curY, chipW, chipH});

            int bg = active ? Theme.getAccentColor() : 0x351C1F2E;
            int textCol = active ? 0xFFFFFFFF : Theme.TEXT_MUTED;

            Render2D.drawRoundedRect(curX, curY, chipW, chipH, 2.5F, ColorUtil.multiplyAlpha(bg, alpha));

            float textStartX = curX + padX;
            if (p.icon != null) {
                Fonts.drawIcon(p.icon, textStartX, curY + 2.0F, 7.5F, ColorUtil.multiplyAlpha(textCol, alpha));
                textStartX += 9.5F;
            }
            Fonts.drawString(Fonts.SF_MEDIUM, p.title, textStartX, curY + 2.0F, fontSize, ColorUtil.multiplyAlpha(textCol, alpha));

            curX += chipW + 3.0F;
        }

        return menuH;
    }

    @Override
    public boolean handleContextMenuClick(float menuX, float menuY, double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float menuW = 145.0F;

        for (Map.Entry<Part, float[]> entry : chipBounds.entrySet()) {
            float[] b = entry.getValue();
            if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                Part p = entry.getKey();
                boolean newState = !visibleMap.get(p);
                visibleMap.put(p, newState);

                for (Cluster c : clusters) {
                    c.parts.remove(p);
                }

                if (newState) {
                    if (clusters.isEmpty()) {
                        clusters.add(new Cluster(this.x, this.y));
                    }
                    clusters.get(0).parts.add(p);
                }
                return true;
            }
        }

        return mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + lastMenuH;
    }
}