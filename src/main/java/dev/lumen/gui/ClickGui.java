package dev.lumen.gui;

import dev.lumen.config.LumenConfig;
import dev.lumen.fx.Modules;
import dev.lumen.module.*;
import dev.lumen.spotify.SpotifyAuth;
import dev.lumen.spotify.SpotifyPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu: category pills on top, module cards (click = toggle, right-click = settings), settings drawer on the right.
 * ADJUSTMENT POINT 4: the mouse method signatures (MouseButtonEvent) follow the newest Minecraft versions.
 */
public class ClickGui extends Screen {
    private static final String SPOTIFY = "__spotify__";

    private int category = 0;
    private Module selected = null;
    private boolean spotifyOpen = false;
    private float drawer = 0f;
    private EditBox clientIdBox;

    // layout
    private float px, py, pw, ph;
    // hit areas rebuilt every frame
    private record Hit(float x, float y, float w, float h, Runnable onLeft, Runnable onRight) {
        boolean in(double mx, double my) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    }
    private final List<Hit> hits = new ArrayList<>();
    private NumSetting dragNum; private float dragX, dragW;
    private ColorSetting dragHue; private int dragHueKind; // 0 hue, 1 sat, 2 bri

    public ClickGui() { super(Component.literal("Lumen")); }

    @Override
    protected void init() {
        clientIdBox = new EditBox(this.font, 0, 0, 150, 16, Component.literal("Client ID"));
        clientIdBox.setMaxLength(64);
        clientIdBox.setValue(LumenConfig.spotifyClientId);
        clientIdBox.setResponder(v -> LumenConfig.spotifyClientId = v.trim());
        clientIdBox.visible = false;
        addRenderableWidget(clientIdBox);
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) { /* own backdrop, no vanilla blur */ }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() { LumenConfig.save(); }

    private int accent() { return Modules.ACCENT.rgb(); }
    private float radius() { return (float) Modules.RADIUS.value; }
    private Component t(String s) { return Fonts.text(s, Modules.UI_FONT.index); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        hits.clear();
        float step = Math.min(1f, delta * 0.35f + 0.12f);
        g.fill(0, 0, width, height, 0xB0070510);

        pw = Math.min(width - 24, 480); ph = Math.min(height - 24, 280);
        px = (width - pw) / 2f; py = (height - ph) / 2f;
        int acc = accent();
        float r = radius() + 4;

        Draw.glow(g, px, py, pw, ph, r, acc, 0.55f);
        Draw.roundRect(g, px, py, pw, ph, r, 0xF00D0918);
        Draw.roundOutline(g, px, py, pw, ph, r, 0x40FFFFFF, 0xF00D0918);

        // title + category pills
        Draw.text(g, t("LUMEN"), px + 14, py + 13, 0xFFFFFFFF, 1.1f, false);
        float tx = px + pw / 2f - (ModuleManager.CATEGORIES.length * 62) / 2f;
        for (int i = 0; i < ModuleManager.CATEGORIES.length; i++) {
            final int ci = i;
            boolean sel = i == category;
            float bx = tx + i * 62, by = py + 8, bw = 58, bh = 18;
            boolean hov = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
            int fill = sel ? (0xFF000000 | acc) : (hov ? 0x30FFFFFF : 0x14FFFFFF);
            Draw.roundRect(g, bx, by, bw, bh, 9, fill);
            Component c = t(ModuleManager.CATEGORIES[i]);
            Draw.text(g, c, bx + (bw - Draw.width(c) * 0.9f) / 2f, by + 5, sel ? 0xFF0D0918 : 0xFFD8D0E8, 0.9f, false);
            hits.add(new Hit(bx, by, bw, bh, () -> { category = ci; selected = null; spotifyOpen = false; }, null));
        }

        // module cards
        List<Module> mods = ModuleManager.inCategory(ModuleManager.CATEGORIES[category]);
        int cols = 3;
        float pad = 14, gap = 8, top = py + 38;
        float gridW = (spotifyOpen || selected != null) ? pw * 0.45f - pad : pw - pad * 2;
        if (gridW < 200) gridW = pw - pad * 2;
        cols = gridW < 260 ? 1 : (gridW < 360 ? 2 : 3);
        float cw = (gridW - gap * (cols - 1)) / cols, chh = 46;
        int n = 0;
        List<Object> cards = new ArrayList<>(mods);
        if (ModuleManager.CATEGORIES[category].equals("Music")) cards.add(SPOTIFY);
        for (Object o : cards) {
            float cx = px + pad + (n % cols) * (cw + gap), cy = top + (n / cols) * (chh + gap);
            n++;
            if (o == SPOTIFY) { drawSpotifyCard(g, cx, cy, cw, chh, mx, my, step); continue; }
            Module m = (Module) o;
            drawCard(g, m, cx, cy, cw, chh, mx, my, step);
        }

        // settings drawer
        boolean open = spotifyOpen || selected != null;
        drawer = Draw.approach(drawer, open ? 1f : 0f, step);
        clientIdBox.visible = false;
        if (drawer > 0.02f) {
            float dw = pw * 0.55f - pad;
            float dx = px + pw - dw - pad + (1f - drawer) * 30f;
            float dy = py + 38, dh = ph - 38 - pad;
            Draw.roundRect(g, dx, dy, dw, dh, radius(), (int) (drawer * 0xE0) << 24 | 0x141020);
            if (spotifyOpen) drawSpotifyPanel(g, dx, dy, dw, dh, mx, my);
            else if (selected != null) drawSettings(g, selected, dx, dy, dw, dh, mx, my, step);
        }

        Draw.text(g, t("Click = toggle  |  Right-click = settings  |  Esc = close"), px + 14, py + ph - 11, 0x80FFFFFF, 0.7f, false);
        super.render(g, mx, my, delta); // renders the text box
    }

    private void drawCard(GuiGraphics g, Module m, float x, float y, float w, float h, int mx, int my, float step) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        m.hover = Draw.approach(m.hover, hov ? 1f : 0f, step);
        m.anim = Draw.approach(m.anim, m.enabled ? 1f : 0f, step);
        int acc = accent();
        float lift = m.hover * 1.5f;
        y -= lift;
        if (m.hover > 0.05f) Draw.glow(g, x, y, w, h, radius(), acc, m.hover * 0.5f);
        Draw.roundRect(g, x, y, w, h, radius(), Draw.mix(0xE01A1428, 0xF0241B3A, m.hover));
        Draw.roundOutline(g, x, y, w, h, radius(), Draw.mix(0x22FFFFFF, 0xAA000000 | acc, m.hover), Draw.mix(0xE01A1428, 0xF0241B3A, m.hover));
        // icon tile
        Draw.roundRect(g, x + 8, y + 8, 18, 18, 5, Draw.mix(0x30FFFFFF, 0xFF000000 | acc, m.anim));
        Component ic = t(m.name.substring(0, 1));
        Draw.text(g, ic, x + 17 - Draw.width(ic) * 0.5f, y + 13, m.enabled ? 0xFF0D0918 : 0xFFAAA3C0, 1f, false);
        // switch
        drawSwitch(g, x + w - 30, y + 9, m.anim);
        Draw.text(g, t(m.name), x + 8, y + 31, 0xFFFFFFFF, 0.9f, false);
        String d = m.description.length() > 34 ? m.description.substring(0, 33) + "…" : m.description;
        if (w > 150) Draw.text(g, t(d), x + 8, y + 40, 0x80FFFFFF, 0.6f, false);
        hits.add(new Hit(x, y, w, h, m::toggle, () -> { selected = m; spotifyOpen = false; }));
    }

    private void drawSwitch(GuiGraphics g, float x, float y, float anim) {
        int acc = accent();
        Draw.roundRect(g, x, y, 22, 12, 6, Draw.mix(0xFF2A2440, 0xFF000000 | acc, anim));
        Draw.roundRect(g, x + 1.5f + anim * 10f, y + 1.5f, 9, 9, 4.5f, 0xFFFFFFFF);
    }

    private void drawSpotifyCard(GuiGraphics g, float x, float y, float w, float h, int mx, int my, float step) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int acc = accent();
        if (hov) Draw.glow(g, x, y, w, h, radius(), acc, 0.4f);
        Draw.roundRect(g, x, y, w, h, radius(), hov ? 0xF0241B3A : 0xE01A1428);
        Draw.roundOutline(g, x, y, w, h, radius(), hov ? (0xAA000000 | acc) : 0x22FFFFFF, hov ? 0xF0241B3A : 0xE01A1428);
        Draw.roundRect(g, x + 8, y + 8, 18, 18, 5, 0xFF1DB954);
        Draw.text(g, t("S"), x + 14, y + 13, 0xFF0D0918, 1f, false);
        Draw.text(g, t("Spotify"), x + 8, y + 31, 0xFFFFFFFF, 0.9f, false);
        Draw.text(g, t(SpotifyAuth.status), x + 8, y + 40, SpotifyAuth.connected() ? 0xFF7BE0A0 : 0x80FFFFFF, 0.6f, false);
        hits.add(new Hit(x, y, w, h, () -> { spotifyOpen = true; selected = null; }, () -> { spotifyOpen = true; selected = null; }));
    }

    private void drawSpotifyPanel(GuiGraphics g, float x, float y, float w, float h, int mx, int my) {
        Draw.text(g, t("Spotify"), x + 10, y + 9, 0xFFFFFFFF, 1f, false);
        Draw.text(g, t("1) developer.spotify.com/dashboard > Create app"), x + 10, y + 26, 0xB0FFFFFF, 0.65f, false);
        Draw.text(g, t("2) Redirect URI: " + SpotifyAuth.REDIRECT), x + 10, y + 36, 0xB0FFFFFF, 0.65f, false);
        Draw.text(g, t("3) Paste the Client ID, press Connect"), x + 10, y + 46, 0xB0FFFFFF, 0.65f, false);
        clientIdBox.visible = true;
        clientIdBox.setX((int) (x + 10)); clientIdBox.setY((int) (y + 62));
        clientIdBox.setWidth((int) (w - 20));

        float by = y + 90;
        button(g, x + 10, by, 70, 16, SpotifyAuth.connected() ? "Reconnect" : "Connect", mx, my, SpotifyAuth::connect);
        if (SpotifyAuth.connected()) button(g, x + 86, by, 70, 16, "Disconnect", mx, my, SpotifyAuth::disconnect);
        Draw.text(g, t("Status: " + SpotifyAuth.status), x + 10, by + 24, 0xFFFFFFFF, 0.75f, false);
        if (!SpotifyPlayer.title.isEmpty())
            Draw.text(g, t("Now: " + SpotifyPlayer.artist + " - " + SpotifyPlayer.title), x + 10, by + 36, 0xB0FFFFFF, 0.7f, false);
        Draw.text(g, t("Lyrics: " + dev.lumen.lyrics.LyricsProvider.state), x + 10, by + 48, 0xB0FFFFFF, 0.7f, false);
    }

    private void button(GuiGraphics g, float x, float y, float w, float h, String label, int mx, int my, Runnable r) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        Draw.roundRect(g, x, y, w, h, radius() * 0.8f, hov ? (0xFF000000 | accent()) : 0x30FFFFFF);
        Component c = t(label);
        Draw.text(g, c, x + (w - Draw.width(c) * 0.8f) / 2f, y + 4.5f, hov ? 0xFF0D0918 : 0xFFFFFFFF, 0.8f, false);
        hits.add(new Hit(x, y, w, h, r, null));
    }

    private void drawSettings(GuiGraphics g, Module m, float x, float y, float w, float h, int mx, int my, float step) {
        Draw.text(g, t(m.name), x + 10, y + 9, 0xFFFFFFFF, 1f, false);
        float cy = y + 26;
        for (Setting s : m.settings) {
            float rowH = 24;
            Draw.text(g, t(s.name), x + 10, cy + 2, 0xFFFFFFFF, 0.8f, false);
            if (s.example != null && w > 150) {
                String ex = s.example.length() > 46 ? s.example.substring(0, 45) + "…" : s.example;
                Draw.text(g, t(ex), x + 10, cy + 12, 0x70FFFFFF, 0.55f, false);
            }
            float cx = x + w - 10;
            if (s instanceof BoolSetting b) {
                b.anim = Draw.approach(b.anim, b.value ? 1f : 0f, step);
                drawSwitch(g, cx - 22, cy + 2, b.anim);
                hits.add(new Hit(cx - 22, cy, 22, 14, () -> b.value = !b.value, null));
            } else if (s instanceof ChoiceSetting c) {
                Component lab = t(c.get());
                float bw = Math.max(60, Draw.width(lab) * 0.8f + 14);
                boolean hov = mx >= cx - bw && mx <= cx && my >= cy && my <= cy + 14;
                Draw.roundRect(g, cx - bw, cy, bw, 14, 7, hov ? 0x40FFFFFF : 0x20FFFFFF);
                Draw.text(g, lab, cx - bw + (bw - Draw.width(lab) * 0.8f) / 2f, cy + 3.5f, 0xFFFFFFFF, 0.8f, false);
                hits.add(new Hit(cx - bw, cy, bw, 14, () -> c.cycle(1), () -> c.cycle(-1)));
            } else if (s instanceof NumSetting n) {
                float tw = Math.min(90, w * 0.35f), tx = cx - tw;
                Draw.roundRect(g, tx, cy + 5, tw, 4, 2, 0x30FFFFFF);
                Draw.roundRect(g, tx, cy + 5, (float) (tw * n.norm()), 4, 2, 0xFF000000 | accent());
                Draw.roundRect(g, tx + (float) (tw * n.norm()) - 4, cy + 3, 8, 8, 4, 0xFFFFFFFF);
                Component v = t(String.format(java.util.Locale.ROOT, n.step >= 1 ? "%.0f" : "%.2f", n.value));
                Draw.text(g, v, tx - Draw.width(v) * 0.7f - 4, cy + 3.5f, 0xB0FFFFFF, 0.7f, false);
                final float ftx = tx, ftw = tw;
                hits.add(new Hit(tx - 2, cy, tw + 4, 14, () -> { dragNum = n; dragX = ftx; dragW = ftw; n.set(n.min + (n.max - n.min) * Math.max(0, Math.min(1, (mx - ftx) / ftw))); }, null));
            } else if (s instanceof ColorSetting c) {
                for (int i = 0; i < ColorSetting.NAMES.length; i++) {
                    final int pi = i;
                    float dx = cx - (ColorSetting.NAMES.length - i) * 13f;
                    int col = i == ColorSetting.CUSTOM ? java.awt.Color.HSBtoRGB(c.hue, c.sat, c.bri) & 0xFFFFFF : ColorSetting.RGB[i];
                    if (c.preset == i) Draw.roundRect(g, dx - 1.5f, cy + 0.5f, 13, 13, 6.5f, 0xFFFFFFFF);
                    Draw.roundRect(g, dx, cy + 2, 10, 10, 5, 0xFF000000 | col);
                    hits.add(new Hit(dx, cy + 2, 10, 10, () -> c.preset = pi, null));
                }
                if (c.preset == ColorSetting.CUSTOM) {
                    rowH = 50;
                    float tw = Math.min(110, w * 0.45f), tx = cx - tw;
                    float[] vals = {c.hue, c.sat, c.bri};
                    String[] labs = {"Hue", "Sat", "Bri"};
                    for (int k = 0; k < 3; k++) {
                        float ty = cy + 18 + k * 9;
                        Draw.text(g, t(labs[k]), tx - 22, ty - 1, 0x90FFFFFF, 0.55f, false);
                        Draw.roundRect(g, tx, ty + 1, tw, 3, 1.5f, 0x30FFFFFF);
                        Draw.roundRect(g, tx, ty + 1, tw * vals[k], 3, 1.5f, 0xFF000000 | c.rgb());
                        Draw.roundRect(g, tx + tw * vals[k] - 3, ty - 1, 6, 6, 3, 0xFFFFFFFF);
                        final int kind = k; final float ftx = tx, ftw = tw;
                        hits.add(new Hit(tx - 2, ty - 2, tw + 4, 8, () -> { dragHue = c; dragHueKind = kind; dragX = ftx; dragW = ftw; applyHue(mx); }, null));
                    }
                }
            }
            cy += rowH;
            if (cy > y + h - 8) break;
        }
    }

    private void applyHue(double mx) {
        float v = (float) Math.max(0, Math.min(1, (mx - dragX) / dragW));
        if (dragHueKind == 0) dragHue.hue = v; else if (dragHueKind == 1) dragHue.sat = v; else dragHue.bri = v;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (spotifyOpen && super.mouseClicked(event, doubleClick)) return true; // text box
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(event.x(), event.y())) continue;
            Runnable r = event.button() == 1 ? h.onRight() : h.onLeft();
            if (r != null) { r.run(); return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragNum != null) {
            dragNum.set(dragNum.min + (dragNum.max - dragNum.min) * Math.max(0, Math.min(1, (event.x() - dragX) / dragW)));
            return true;
        }
        if (dragHue != null) { applyHue(event.x()); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragNum = null; dragHue = null;
        return super.mouseReleased(event);
    }
}
