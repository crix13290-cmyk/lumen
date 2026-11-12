package dev.lumen.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * ADJUSTMENT POINT 2: all drawing goes through here, so a rendering-API change only needs fixing in this file.
 * Shapes are drawn in real window pixels (not GUI-scaled pixels), which is what makes the menu look crisp / HD.
 */
public final class Draw {
    private Draw() {}

    private static double scale() { return Minecraft.getInstance().getWindow().getGuiScale(); }

    public static void roundRect(GuiGraphicsExtractor g, float x, float y, float w, float h, float r, int argb) {
        if ((argb >>> 24) == 0) return;
        double s = scale();
        int px = Math.round((float) (x * s)), py = Math.round((float) (y * s));
        int pw = Math.round((float) (w * s)), ph = Math.round((float) (h * s));
        int pr = (int) Math.min(r * s, Math.min(pw, ph) / 2.0);
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale((float) (1.0 / s), (float) (1.0 / s));
        for (int i = 0; i < ph; i++) {
            int inset = 0;
            int d = Math.min(i, ph - 1 - i);
            if (d < pr) {
                double dy = pr - d - 0.5;
                inset = (int) Math.ceil(pr - Math.sqrt(Math.max(0, (double) pr * pr - dy * dy)));
            }
            g.fill(px + inset, py + i, px + pw - inset, py + i + 1, argb);
        }
        pose.popMatrix();
    }

    /** 1px (physical) outline built from two round rects. */
    public static void roundOutline(GuiGraphicsExtractor g, float x, float y, float w, float h, float r, int argb, int innerArgb) {
        float t = (float) (1.0 / scale());
        roundRect(g, x, y, w, h, r, argb);
        roundRect(g, x + t, y + t, w - 2 * t, h - 2 * t, Math.max(0, r - t), innerArgb);
    }

    /** Soft glow: several expanding translucent rects. */
    public static void glow(GuiGraphicsExtractor g, float x, float y, float w, float h, float r, int rgb, float strength) {
        for (int i = 6; i >= 1; i--) {
            float e = i * 1.6f;
            int a = (int) (strength * 40f / i);
            roundRect(g, x - e, y - e, w + 2 * e, h + 2 * e, r + e, (Math.min(255, a) << 24) | (rgb & 0xFFFFFF));
        }
    }

    public static int width(Component c) { return Minecraft.getInstance().font.width(c); }

    public static void text(GuiGraphicsExtractor g, Component c, float x, float y, int argb, float scale, boolean shadow) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.text(Minecraft.getInstance().font, c, 0, 0, argb, shadow);
        pose.popMatrix();
    }

    /** Centered text, optionally with a soft coloured glow behind it. */
    public static void textCentered(GuiGraphicsExtractor g, Component c, float cx, float y, int argb, float scale, boolean glow) {
        float w = width(c) * scale;
        float x = cx - w / 2f;
        if (glow) {
            int ga = ((argb >>> 24) * 40 / 255) << 24 | (argb & 0xFFFFFF);
            for (float[] o : new float[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1.5f, -1.5f}, {1.5f, 1.5f}, {-1.5f, 1.5f}, {1.5f, -1.5f}})
                text(g, c, x + o[0] * scale, y + o[1] * scale, ga, scale, false);
        }
        text(g, c, x, y, argb, scale, false);
    }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float approach(float cur, float target, float speed) {
        float d = target - cur;
        return Math.abs(d) < 0.001f ? target : cur + d * Math.min(1f, speed);
    }
    public static int mix(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
        return ((int) lerp(aa, ba, t) << 24) | ((int) lerp(ar, br, t) << 16) | ((int) lerp(ag, bg, t) << 8) | (int) lerp(ab, bb, t);
    }
}
