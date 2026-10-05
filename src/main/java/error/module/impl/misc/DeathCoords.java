package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.ui.hud.impl.DynamicIslandHud;
import error.util.client.clients.Theme;
import error.util.client.persiki.ChatUtil;
import error.util.client.clients.ColorUtil;
import error.util.render.Render3D;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;

public class DeathCoords extends Module {
    public static DeathCoords INSTANCE;

    public final CheckBox notifyChat = checkbox("Уведомление в чат", true);
    public final CheckBox copyClipboard = checkbox("Копировать в буфер", true);
    public final CheckBox renderWaypoint = checkbox("Подсветка места смерти", true);
    public final CheckBox clearOnRespawn = checkbox("Очищать при возрождении", false);

    private boolean died = false;
    private Vec3 lastDeathPos = null;

    public DeathCoords() {
        super("DeathCoords", "Сохраняет и выводит координаты вашей смерти", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        died = false;
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;

        boolean isDead = player().isDeadOrDying() || player().getHealth() <= 0.0F || screen() instanceof DeathScreen;

        if (isDead) {
            if (!died) {
                died = true;
                int x = (int) Math.floor(player().getX());
                int y = (int) Math.floor(player().getY());
                int z = (int) Math.floor(player().getZ());
                lastDeathPos = new Vec3(x, y, z);

                String coords = "X: " + x + " Y: " + y + " Z: " + z;

                if (notifyChat.getValue()) {
                    ChatUtil.info("Координаты смерти: §e" + coords);
                }

                DynamicIslandHud.showNotification("Координаты смерти: " + coords, true, 4000L);

                if (copyClipboard.getValue()) {
                    mc.keyboardHandler.setClipboard(coords);
                }
            }
        } else {
            if (player().isAlive() && !(screen() instanceof DeathScreen)) {
                if (died && clearOnRespawn.getValue()) {
                    lastDeathPos = null;
                }
                died = false;
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!renderWaypoint.getValue() || lastDeathPos == null) return;

        BlockPos pos = BlockPos.containing(lastDeathPos);
        int accent = Theme.getAccentColor();
        Color fillCol = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 60);
        Color outCol = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 240);

        Render3D.drawBox(pos, fillCol, outCol, true, true, true);
    }
}
