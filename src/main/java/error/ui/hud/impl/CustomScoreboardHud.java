package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CustomScoreboardHud extends HudElement {

    public record ScoreLine(Component name, Component score, int scoreW) {}

    private final Animation widthAnim = new Animation(110.0F, 0.20F);
    private final Animation heightAnim = new Animation(80.0F, 0.20F);

    public CustomScoreboardHud() {
        super("custom_scoreboard", "Scoreboard", 0.0F, 0.0F, 120.0F, 100.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.customScoreboard.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    public static Objective getSidebarObjective() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        Scoreboard sb = mc.level.getScoreboard();
        PlayerTeam team = sb.getPlayersTeam(mc.player.getScoreboardName());
        if (team != null && team.getColor() != null && team.getColor().isPresent()) {
            DisplaySlot slot = team.getColor().get().displaySlot();
            if (slot != null) {
                Objective teamObj = sb.getDisplayObjective(slot);
                if (teamObj != null) return teamObj;
            }
        }
        return sb.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    @Override
    public void draw(Render2DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.font == null) return;

        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;
        Objective objective = getSidebarObjective();

        if (objective == null) {
            if (!inChat) {
                this.width = 0;
                this.height = 0;
                return;
            }

            // Preview in ChatScreen
            String previewTitle = "⚡ Анархия-101";
            List<String> previewLines = List.of(
                    "├─ Статус: Игрок",
                    "├─ Монеты: 15,420",
                    "├─ Убийств: 14",
                    "└─ errorclient.su"
            );

            int titleW = mc.font.width(previewTitle);
            int maxLineW = titleW;
            for (String s : previewLines) {
                int w = mc.font.width(s);
                if (w > maxLineW) maxLineW = w;
            }

            float cardW = Math.max(90.0F, maxLineW + 16.0F);
            float cardH = 10.0F + 12.0F + (5.0F + previewLines.size() * 10.0F) + 4.0F;

            if (!this.dragging && this.x == 0 && this.y == 0) {
                this.x = mc.getWindow().getGuiScaledWidth() - cardW - 8.0F;
                this.y = mc.getWindow().getGuiScaledHeight() * 0.35F;
            }

            this.width = cardW;
            this.height = cardH;

            int accent = Theme.getAccentColor();
            Render2D.drawHudCard(this.x, this.y, cardW, cardH, 8.0F, 1.0F);

            var extractor = event.getGuiGraphicsExtractor();
            if (extractor != null) {
                Render2DUtil.flush();

                // Centered Title
                float titleX = this.x + (cardW - titleW) * 0.5F;
                float titleY = this.y + 6.0F;
                extractor.text(mc.font, previewTitle, (int) titleX, (int) titleY, 0xFFFFFFFF, false);

                // Divider Line
                float divY = titleY + 11.0F;
                Render2D.drawRoundedRect(this.x + 6.0F, divY, cardW - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, 30));

                // Tree Lines
                float startLineY = divY + 4.0F;
                int redBranchColor = ColorUtil.rgba(245, 60, 60, 255);
                for (int i = 0; i < previewLines.size(); i++) {
                    String line = previewLines.get(i);
                    float ly = startLineY + i * 10.0F;
                    float lx = this.x + 8.0F;

                    if (line.startsWith("├─") || line.startsWith("└─")) {
                        String branch = line.substring(0, 2);
                        String rest = line.substring(2);
                        int branchW = mc.font.width(branch);
                        extractor.text(mc.font, branch, (int) lx, (int) ly, redBranchColor, false);
                        extractor.text(mc.font, rest, (int) (lx + branchW), (int) ly, 0xFFE0E0E0, false);
                    } else {
                        extractor.text(mc.font, line, (int) lx, (int) ly, 0xFFFFFFFF, false);
                    }
                }
            }
            return;
        }

        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);

        List<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective)
                .stream()
                .filter(e -> !e.isHidden())
                .sorted(Comparator.comparing(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
                .limit(15L)
                .toList();

        Component title = objective.getDisplayName();
        int titleW = mc.font.width(title);
        int maxLineW = titleW;

        List<ScoreLine> lines = new ArrayList<>();
        for (PlayerScoreEntry entry : entries) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component formattedName = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            Component scoreComp = entry.formatValue(numFormat);
            int scoreW = scoreComp != null ? mc.font.width(scoreComp) : 0;
            int nameW = mc.font.width(formattedName);
            int totalW = nameW + (scoreW > 0 ? (mc.font.width(": ") + scoreW) : 0);
            if (totalW > maxLineW) {
                maxLineW = totalW;
            }
            lines.add(new ScoreLine(formattedName, scoreComp, scoreW));
        }

        float cardW = Math.max(80.0F, maxLineW + 16.0F);
        float cardH = 10.0F + 12.0F + (lines.isEmpty() ? 0 : (5.0F + lines.size() * 10.0F)) + 4.0F;

        widthAnim.setTarget(cardW);
        widthAnim.update();
        heightAnim.setTarget(cardH);
        heightAnim.update();

        this.width = widthAnim.getValue();
        this.height = heightAnim.getValue();

        // Default position anchored to right side if not set/dragged
        if (!this.dragging && this.x == 0 && this.y == 0) {
            this.x = mc.getWindow().getGuiScaledWidth() - this.width - 8.0F;
            this.y = mc.getWindow().getGuiScaledHeight() * 0.35F - this.height * 0.5F;
        }

        int accent = Theme.getAccentColor();

        Interface iface = Interface.getInstance();
        if (iface != null) {
            String mode = iface.scoreboardMode.getValue();
            if ("Скрыт".equalsIgnoreCase(mode)) {
                return;
            }
            if ("Ванильный".equalsIgnoreCase(mode)) {
                return;
            }
        }

        boolean dropShadow = iface == null || iface.scoreboardShadow.getValue();
        boolean removeScores = iface != null && iface.scoreboardRemoveScores.getValue();

        // 1. Draw Liquid Glass Card
        Render2D.drawHudCard(this.x, this.y, this.width, this.height, 8.0F, 1.0F);

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor == null) return;

        Render2DUtil.flush();

        // 2. Centered Title
        float titleX = this.x + (this.width - titleW) * 0.5F;
        float titleY = this.y + 6.0F;
        extractor.text(mc.font, title, (int) titleX, (int) titleY, 0xFFFFFFFF, dropShadow);

        // 3. Divider Line
        if (!lines.isEmpty()) {
            float divY = titleY + 11.0F;
            Render2D.drawRoundedRect(this.x + 6.0F, divY, this.width - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, 30));

            // 4. Lines with server color codes and red tree branches
            float startLineY = divY + 4.0F;
            for (int i = 0; i < lines.size(); i++) {
                ScoreLine sl = lines.get(i);
                float ly = startLineY + i * 10.0F;

                extractor.text(mc.font, sl.name, (int) (this.x + 7.0F), (int) ly, 0xFFFFFFFF, dropShadow);

                if (!removeScores && sl.scoreW > 0 && sl.score != null) {
                    float sx = this.x + this.width - 7.0F - sl.scoreW;
                    extractor.text(mc.font, sl.score, (int) sx, (int) ly, ColorUtil.rgba(230, 80, 80, 240), dropShadow);
                }
            }
        }
    }
}
