package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * AutoInvest module ported from exclusive.
 * Automatically invests coins into clan by bind or balance threshold.
 */
public class AutoInvest extends Module {

    public final ModeSetting mode = mode("Режим", "Бинд", "Бинд", "Авто");
    public final BindSetting investKey = bind("Клавиша инвеста", GLFW.GLFW_KEY_UNKNOWN).visible(() -> mode.is("Бинд"));
    public final SliderSetting bindPercent = slider("% от баланса (Бинд)", 50.0f, 1.0f, 100.0f, 1.0f).visible(() -> mode.is("Бинд"));
    public final SliderSetting triggerAmount = slider("Порог монет", 10000000.0f, 100000.0f, 100000000.0f, 500000.0f).visible(() -> mode.is("Авто"));
    public final SliderSetting autoPercent = slider("% от баланса (Авто)", 50.0f, 1.0f, 100.0f, 1.0f).visible(() -> mode.is("Авто"));

    private int cooldownTicks = 0;

    public AutoInvest() {
        super("AutoInvest", "Автоматические инвестиции в клан по бинду или авто-порогу", Category.MISC);
    }

    @Override
    protected void onDisable() {
        cooldownTicks = 0;
        super.onDisable();
    }

    @EventTarget
    public void onKey(KeyboardInputEvent event) {
        if (!inGame() || player() == null || !mode.is("Бинд")) return;

        if (event.getAction() == GLFW.GLFW_PRESS && investKey.matches(event.getKey())) {
            int balance = getBalanceFromScoreboard();
            if (balance <= 0) {
                return;
            }
            int amount = (int) (balance * (bindPercent.getValue() / 100.0f));
            if (amount <= 0) return;

            if (mc.getConnection() != null) {
                mc.getConnection().sendCommand("clan invest " + amount);
            }
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || !mode.is("Авто")) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        int balance = getBalanceFromScoreboard();
        if (balance <= 0) return;

        int trigger = triggerAmount.getValue().intValue();
        if (balance < trigger) return;

        int amount = (int) (balance * (autoPercent.getValue() / 100.0f));
        if (amount <= 0) return;

        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand("clan invest " + amount);
        }
        cooldownTicks = 100;
    }

    private int getBalanceFromScoreboard() {
        if (mc.level == null || player() == null) return -1;
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return -1;

        List<PlayerScoreEntry> entries = new java.util.ArrayList<>(scoreboard.listPlayerScores(objective));
        for (PlayerScoreEntry entry : entries) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component formattedName = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            int balance = parseMoneyLine(formattedName.getString());
            if (balance > 0) return balance;
        }
        return -1;
    }

    private int parseMoneyLine(String line) {
        if (line == null) return -1;
        String text = line.replaceAll("§.", "").replaceAll("[\\u00A7].", "").toLowerCase();
        if (!text.contains("монет") && !text.contains("баланс")) return -1;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return -1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
