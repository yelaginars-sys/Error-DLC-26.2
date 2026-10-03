package error.module.impl.misc;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.persiki.ChatUtil;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;

import java.util.*;

public class SpecScan extends Module {
    public static SpecScan INSTANCE;

    public final MultiModeSetting notifications = multiMode("Уведомления", List.of("Chat", "Notify"), "Chat", "Notify");
    public final SliderSetting radius = slider("Радиус скана", 60.0f, 10.0f, 200.0f, 5.0f);
    public final CheckBox soundAlert = checkbox("Звуковой сигнал", true);
    public final CheckBox autoLeave = checkbox("Авто-выход при наблюдателе", false);
    public final ModeSetting leaveCommand = mode("Команда выхода", "/hub", "/hub", "/spawn", "/home", "Disconnect");

    private final Set<String> detectedSpectators = new HashSet<>();
    private long lastScanTime = 0;

    public SpecScan() {
        super("SpecScan", "Сканирование и обнаружение наблюдателей и спектаторов рядом", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        detectedSpectators.clear();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || mc.level == null || mc.getConnection() == null) return;

        long now = System.currentTimeMillis();
        if (now - lastScanTime < 500) return; // Scan twice per second
        lastScanTime = now;

        Set<String> currentSpecs = new HashSet<>();

        // 1. Scan loaded player entities for Spectator / Invis
        for (Player p : mc.level.players()) {
            if (p == player() || !p.isAlive()) continue;

            double dist = player().distanceTo(p);
            if (dist > radius.getValue()) continue;

            boolean isSpec = p.isSpectator();
            boolean isInvis = p.isInvisible();

            if (isSpec || isInvis) {
                String name = p.getName().getString();
                String modeText = isSpec ? "Spectator" : "Invisible";
                currentSpecs.add(name);

                if (!detectedSpectators.contains(name)) {
                    notifySpectator(name, modeText, dist);
                }
            }
        }

        // 2. Scan Tab list for spectators
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info.getGameMode() == net.minecraft.world.level.GameType.SPECTATOR) {
                String name = info.getProfile().name();
                if (!name.equalsIgnoreCase(player().getName().getString())) {
                    currentSpecs.add(name);
                    if (!detectedSpectators.contains(name)) {
                        notifySpectator(name, "Tab Spectator", 0);
                    }
                }
            }
        }

        detectedSpectators.clear();
        detectedSpectators.addAll(currentSpecs);

        if (!currentSpecs.isEmpty() && autoLeave.getValue()) {
            executeAutoLeave();
        }
    }

    private void notifySpectator(String name, String type, double dist) {
        String distText = dist > 0 ? String.format(Locale.ROOT, " (%.1fm)", dist) : "";
        String msg = "Обнаружен наблюдатель: §c" + name + " §7[" + type + "]" + distText;

        if (notifications.isEnabled("Chat")) {
            ChatUtil.info("[SpecScan] » " + msg);
        }

        if (notifications.isEnabled("Notify")) {
            Notify.add("SpecScan Наблюдатель!", name + " (" + type + ")", IconUse.WARN, ColorUtil.rgba(255, 170, 0, 255));
        }

        if (soundAlert.getValue() && mc.level != null && player() != null) {
            mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 1.5f);
        }
    }

    private void executeAutoLeave() {
        if (mc.getConnection() == null) return;
        String cmd = leaveCommand.getValue();
        if ("Disconnect".equalsIgnoreCase(cmd)) {
            mc.getConnection().getConnection().disconnect(Component.literal("§cSpecScan: Обнаружен наблюдатель!"));
        } else {
            mc.getConnection().sendCommand(cmd.replace("/", ""));
        }
    }
}
