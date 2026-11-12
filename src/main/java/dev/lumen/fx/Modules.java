package dev.lumen.fx;

import dev.lumen.module.*;

/** All module definitions in one place. Add a module here + register it, and it shows up in the menu. */
public final class Modules {
    public static final Feature INTERFACE = new Feature("Interface", "Look of the menu itself", "Settings", true);
    public static final ChoiceSetting UI_FONT = INTERFACE.add(new ChoiceSetting("Menu font", "Default / Clean / Mono", 0, "Default", "Clean", "Mono"));
    public static final ColorSetting ACCENT = INTERFACE.add(new ColorSetting("Accent colour", "Toggles, glow, highlights", 1));
    public static final NumSetting RADIUS = INTERFACE.add(new NumSetting("Corner radius", "2 = sharp, 12 = very round", 2, 12, 1, 8));

    public static final Feature LYRICS = new Feature("Lyrics", "Spotify lyrics on screen or floating in the air", "Music", true);
    public static final ChoiceSetting L_DISPLAY = LYRICS.add(new ChoiceSetting("Display", "Lines = whole line, Karaoke = word lights up, Single word = one word at a time", 1, "Lines", "Karaoke", "Single word"));
    public static final ChoiceSetting L_PLACE = LYRICS.add(new ChoiceSetting("Placement", "On screen = fixed, In the air = stays in the world", 1, "On screen", "In the air"));
    public static final ChoiceSetting L_FONT = LYRICS.add(new ChoiceSetting("Font", "Default / Clean / Mono", 0, "Default", "Clean", "Mono"));
    public static final ColorSetting L_COLOR = LYRICS.add(new ColorSetting("Colour", "Colour of the sung text", 1));
    public static final NumSetting L_SIZE = LYRICS.add(new NumSetting("Size", "1.0 small, 3.0 big", 0.8, 5, 0.1, 2.0));
    public static final BoolSetting L_GLOW = LYRICS.add(new BoolSetting("Glow", "Soft light behind the text", true));
    public static final BoolSetting L_NEXT = LYRICS.add(new BoolSetting("Show next line", "Small preview under the line", true));
    public static final BoolSetting L_FLOAT = LYRICS.add(new BoolSetting("Float animation", "Gentle up and down drift", true));
    public static final NumSetting L_X = LYRICS.add(new NumSetting("Offset X", "Move left / right", -300, 300, 5, 0));
    public static final NumSetting L_Y = LYRICS.add(new NumSetting("Offset Y", "Move up / down", -200, 200, 5, -40));

    public static final Feature HIT = new Feature("Hit Colour", "Players flash in a colour when you hit them", "Visuals", true);
    public static final ColorSetting H_COLOR = HIT.add(new ColorSetting("Colour", "Gold by default, any colour possible", 0));
    public static final NumSetting H_TIME = HIT.add(new NumSetting("Duration (ms)", "150 = quick flash, 800 = slow fade", 100, 1200, 25, 350));
    public static final NumSetting H_STRENGTH = HIT.add(new NumSetting("Strength", "0.3 subtle, 1.0 full colour", 0.1, 1.0, 0.05, 0.85));
    public static final BoolSetting H_ONLY_PLAYERS = HIT.add(new BoolSetting("Players only", "Off = mobs flash too", true));

    private Modules() {}

    public static void registerAll() {
        ModuleManager.register(LYRICS);
        ModuleManager.register(HIT);
        ModuleManager.register(INTERFACE);
    }
}
