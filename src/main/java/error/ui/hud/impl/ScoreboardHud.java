package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScoreboardHud extends HudElement implements IMinecraft {

    public ScoreboardHud() {
        super("scoreboard", "Scoreboard", 10.0F, 120.0F, 120.0F, 80.0F, true);
    }

    private static class SidebarLine {
        final String name;
        final String score;
        final int scoreWidth;

        SidebarLine(String name, String score, int scoreWidth) {
            this.name = name;
            this.score = score;
            this.scoreWidth = scoreWidth;
        }
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null || mc.level == null) return;

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);

        if (objective == null) {
            if (mc.gui.screen() instanceof ChatScreen) {
                // Render preview box when configuring HUD in chat screen
                float previewW = 100.0F;
                float previewH = 32.0F;
                this.width = previewW;
                this.height = previewH;
                float px = getX();
                float py = getY();
                int shadowCol = ColorUtil.rgba(0, 0, 0, 140);
                int bgCol = ColorUtil.rgba(20, 18, 28, 215);
                int borderCol = ColorUtil.rgba(255, 255, 255, 35);
                Render2D.drawShadow(px, py, previewW, previewH, 6.0F, 8.0F, shadowCol);
                Render2D.drawBlur(px, py, previewW, previewH, 6.0F, 14.0F, bgCol, 1.0F);
                Render2D.drawRoundedRect(px, py, previewW, previewH, 6.0F, bgCol);
                Render2D.drawRoundedOutline(px, py, previewW, previewH, 6.0F, 1.0F, borderCol);
                Fonts.drawString(Fonts.SF_MEDIUM, "Scoreboard", px + 8.0F, py + 10.0F, 7.5F, ColorUtil.rgba(240, 240, 255, 220));
            } else {
                this.width = 0.0F;
                this.height = 0.0F;
            }
            return;
        }

        String titleText = objective.getDisplayName().getString();
        List<SidebarLine> lines = new ArrayList<>();

        var scores = scoreboard.listPlayerScores(objective);
        var sortedScores = new ArrayList<>(scores);
        sortedScores.sort(Comparator.comparingInt(s -> -s.value()));

        float maxContentW = Fonts.SF_MEDIUM.getWidth(titleText, 7.5F);

        int maxEntries = Math.min(15, sortedScores.size());
        for (int i = 0; i < maxEntries; i++) {
            var score = sortedScores.get(i);
            String rawOwnerName = score.owner();

            PlayerTeam team = scoreboard.getPlayersTeam(rawOwnerName);
            Component teamNameComp = PlayerTeam.formatNameForTeam(team, Component.literal(rawOwnerName));
            String lineName = teamNameComp.getString();

            String scoreValText = String.valueOf(score.value());
            int scoreW = (int) Fonts.SF_MEDIUM.getWidth(scoreValText, 7.0F);

            lines.add(new SidebarLine(lineName, scoreValText, scoreW));

            float lineW = Fonts.SF_MEDIUM.getWidth(lineName, 7.0F) + (scoreW > 0 ? (scoreW + 10.0F) : 0.0F);
            maxContentW = Math.max(maxContentW, lineW);
        }

        float cardW = Math.max(90.0F, maxContentW + 18.0F);
        float lineH = 10.0F;
        float headerH = 18.0F;
        float cardH = headerH + (lines.isEmpty() ? 0.0F : (4.0F + lines.size() * lineH + 4.0F));

        this.width = cardW;
        this.height = cardH;

        float x = getX();
        float y = getY();

        int shadowCol = ColorUtil.rgba(0, 0, 0, 150);
        int glassFill = ColorUtil.rgba(18, 16, 26, 225);
        int glassBorder = ColorUtil.rgba(255, 255, 255, 38);
        int themeAccent = Theme.getAccentColor();

        // Background Glass Panel
        Render2D.drawShadow(x, y, cardW, cardH, 6.5F, 10.0F, shadowCol);
        Render2D.drawBlur(x, y, cardW, cardH, 6.5F, 16.0F, glassFill, 1.0F);
        Render2D.drawRoundedRect(x, y, cardW, cardH, 6.5F, glassFill);
        Render2D.drawRoundedOutline(x, y, cardW, cardH, 6.5F, 1.0F, glassBorder);

        // Header Title
        float titleW = Fonts.SF_MEDIUM.getWidth(titleText, 7.5F);
        float titleX = x + (cardW - titleW) / 2.0F;
        float titleY = y + 5.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, titleText, titleX, titleY, 7.5F, ColorUtil.rgba(255, 255, 255, 245));

        // Separator line
        if (!lines.isEmpty()) {
            float sepY = y + headerH;
            Render2D.drawRoundedRect(x + 6.0F, sepY, cardW - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, 30));

            float curY = sepY + 4.0F;
            for (SidebarLine line : lines) {
                Fonts.drawString(Fonts.SF_MEDIUM, line.name, x + 7.0F, curY, 7.0F, ColorUtil.rgba(230, 230, 245, 230));
                if (line.scoreWidth > 0) {
                    float scoreX = x + cardW - 7.0F - line.scoreWidth;
                    Fonts.drawString(Fonts.SF_MEDIUM, line.score, scoreX, curY, 7.0F, ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), 220));
                }
                curY += lineH;
            }
        }
    }
}
