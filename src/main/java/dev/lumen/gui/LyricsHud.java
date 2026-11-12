package dev.lumen.gui;

import dev.lumen.fx.Modules;
import dev.lumen.lyrics.LyricLine;
import dev.lumen.lyrics.LyricsProvider;
import dev.lumen.spotify.SpotifyPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Draws the lyrics. "In the air" works without any world-rendering API: the text is pinned to a
 * yaw/pitch direction and projected to the screen, so it stays where you placed it while you look around.
 */
public final class LyricsHud {
    private static float anchorYaw, anchorPitch;
    private static boolean anchorSet = false;
    private static int lastLine = -2;
    private static long lineChangedAt = 0;

    private LyricsHud() {}

    /** Pins the air-lyrics to where the player is looking right now. */
    public static void placeHere() {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        anchorYaw = p.getYRot();
        anchorPitch = Mth.clamp(p.getXRot() - 5f, -80f, 80f);
        anchorSet = true;
    }

    public static void render(GuiGraphicsExtractor g, float partialTick) {
        if (!Modules.LYRICS.enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!SpotifyPlayer.isPlaying()) return;
        List<LyricLine> lines = LyricsProvider.lines;
        if (lines.isEmpty()) return;

        long now = SpotifyPlayer.progress();
        int idx = LyricsProvider.indexAt(now);
        if (idx < 0) return;
        if (idx != lastLine) { lastLine = idx; lineChangedAt = System.currentTimeMillis(); }

        LyricLine line = lines.get(idx);
        if (line.text().isBlank()) return; // instrumental gap
        long end = idx + 1 < lines.size() ? lines.get(idx + 1).startMs() : line.startMs() + 5000;
        end = Math.min(end, line.startMs() + 9000);

        int sw = g.guiWidth(), sh = g.guiHeight();
        float cx = sw / 2f + (float) Modules.L_X.value;
        float cy = sh / 2f + (float) Modules.L_Y.value;
        float alpha = 1f;

        if (Modules.L_PLACE.index == 1) { // in the air
            if (!anchorSet) placeHere();
            float yaw = mc.player.getViewYRot(partialTick), pitch = mc.player.getViewXRot(partialTick);
            float ppd = sh / (float) Math.max(30, mc.options.fov().get()); // pixels per degree
            cx = sw / 2f + Mth.wrapDegrees(anchorYaw - yaw) * ppd + (float) Modules.L_X.value;
            cy = sh / 2f + (pitch - anchorPitch) * ppd + (float) Modules.L_Y.value;
            float edge = Math.min(Math.min(cx, sw - cx), Math.min(cy, sh - cy));
            alpha = Mth.clamp(edge / 60f + 0.2f, 0f, 1f); // fade out when you look away
            if (alpha <= 0f) return;
        }
        if (Modules.L_FLOAT.value) cy += (float) Math.sin(System.currentTimeMillis() / 700.0) * 3f;

        float fadeIn = Mth.clamp((System.currentTimeMillis() - lineChangedAt) / 220f, 0f, 1f);
        float ease = 1f - (1f - fadeIn) * (1f - fadeIn);
        alpha *= ease;
        cy += (1f - ease) * 6f; // slides up into place

        float scale = Modules.L_SIZE.f();
        int font = Modules.L_FONT.index;
        boolean glow = Modules.L_GLOW.value;
        int rgb = Modules.L_COLOR.rgb();
        int full = Modules.L_COLOR.argb(alpha);
        int dim = (((int) (alpha * 90)) << 24) | rgb;

        switch (Modules.L_DISPLAY.index) {
            case 0 -> Draw.textCentered(g, Fonts.text(line.text(), font), cx, cy, full, scale, glow);
            case 1 -> drawKaraoke(g, line.text(), font, cx, cy, scale, now, line.startMs(), end, full, dim, glow);
            default -> drawSingleWord(g, line.text(), font, cx, cy, scale, now, line.startMs(), end, alpha, rgb, glow);
        }

        if (Modules.L_NEXT.value && idx + 1 < lines.size() && Modules.L_DISPLAY.index != 2) {
            String next = lines.get(idx + 1).text();
            if (!next.isBlank())
                Draw.textCentered(g, Fonts.text(next, font), cx, cy + 11 * scale + 4, (((int) (alpha * 110)) << 24) | rgb, scale * 0.6f, false);
        }
    }

    /** Words are timed proportionally to their length. Returns {startFraction, endFraction} per word. */
    private static float[][] wordTimes(String[] words) {
        float total = 0;
        for (String w : words) total += w.length() + 1;
        float[][] t = new float[words.length][2];
        float acc = 0;
        for (int i = 0; i < words.length; i++) {
            t[i][0] = acc / total;
            acc += words[i].length() + 1;
            t[i][1] = acc / total;
        }
        return t;
    }

    private static void drawKaraoke(GuiGraphicsExtractor g, String text, int font, float cx, float cy, float scale,
                                    long now, long start, long end, int full, int dim, boolean glow) {
        String[] words = text.trim().split("\\s+");
        float[][] t = wordTimes(words);
        float p = Mth.clamp((now - start) / (float) Math.max(1, (end - start) * 0.85f), 0f, 1f);
        float space = Draw.width(Fonts.text(" ", font)) * scale;
        float totalW = -space;
        for (String w : words) totalW += Draw.width(Fonts.text(w, font)) * scale + space;
        float x = cx - totalW / 2f;
        for (int i = 0; i < words.length; i++) {
            Component c = Fonts.text(words[i], font);
            float w = Draw.width(c) * scale;
            float k = Mth.clamp((p - t[i][0]) / Math.max(0.001f, t[i][1] - t[i][0]), 0f, 1f);
            int col = Draw.mix(dim, full, k);
            float lift = (float) Math.sin(k * Math.PI) * 2f * scale; // the word you are on hops a little
            if (glow && k > 0.05f) Draw.text(g, c, x, cy - lift, (((col >>> 24) * 40 / 255) << 24) | (col & 0xFFFFFF), scale * 1.06f, false);
            Draw.text(g, c, x, cy - lift, col, scale, false);
            x += w + space;
        }
    }

    private static void drawSingleWord(GuiGraphicsExtractor g, String text, int font, float cx, float cy, float scale,
                                       long now, long start, long end, float alpha, int rgb, boolean glow) {
        String[] words = text.trim().split("\\s+");
        float[][] t = wordTimes(words);
        float p = Mth.clamp((now - start) / (float) Math.max(1, (end - start) * 0.85f), 0f, 0.999f);
        int w = 0;
        for (int i = 0; i < words.length; i++) if (p >= t[i][0]) w = i;
        float local = Mth.clamp((p - t[w][0]) / Math.max(0.001f, t[w][1] - t[w][0]), 0f, 1f);
        float pop = Mth.clamp(local * 6f, 0f, 1f);
        float s = scale * 1.4f * (0.85f + 0.15f * (1f - (1f - pop) * (1f - pop)));
        int col = (((int) (alpha * pop * 255)) << 24) | rgb;
        Draw.textCentered(g, Fonts.text(words[w], font), cx, cy - 4 * s, col, s, glow);
    }
}
