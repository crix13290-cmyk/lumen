package dev.lumen.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.Color;

/** A preset (Gold, Purple, ...) or a fully custom colour (hue / saturation / brightness). */
public class ColorSetting extends Setting {
    public static final String[] NAMES = {"Gold", "Purple", "Cyan", "Rose", "Emerald", "Ice", "Custom"};
    public static final int[] RGB = {0xFFB830, 0xA855F7, 0x22D3EE, 0xF472B6, 0x34D399, 0xDDE6FF, 0};
    public static final int CUSTOM = 6;

    public int preset;
    public float hue = 0.1f, sat = 0.8f, bri = 1f;

    public ColorSetting(String name, String example, int defaultPreset) {
        super(name, example);
        this.preset = defaultPreset;
    }

    public int rgb() {
        if (preset == CUSTOM) return Color.HSBtoRGB(hue, sat, bri) & 0xFFFFFF;
        return RGB[preset];
    }

    public int argb(float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | rgb();
    }

    @Override public JsonElement save() {
        JsonObject o = new JsonObject();
        o.addProperty("preset", preset);
        o.addProperty("hue", hue); o.addProperty("sat", sat); o.addProperty("bri", bri);
        return o;
    }

    @Override public void load(JsonElement e) {
        JsonObject o = e.getAsJsonObject();
        preset = Math.floorMod(o.get("preset").getAsInt(), NAMES.length);
        hue = o.get("hue").getAsFloat(); sat = o.get("sat").getAsFloat(); bri = o.get("bri").getAsFloat();
    }
}
