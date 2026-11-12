package dev.lumen.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * ADJUSTMENT POINT 1: the only place that touches the font-selection API of Style.
 * Index 0 = vanilla, 1 = assets/lumen/font/clean.json, 2 = mono.json (drop clean.ttf / mono.ttf next to them).
 */
public final class Fonts {
    public static final String[] NAMES = {"Default", "Clean", "Mono"};
    private static final String[] IDS = {null, "clean", "mono"};

    private Fonts() {}

    public static Component text(String s, int fontIndex) {
        Component c = Component.literal(s);
        if (fontIndex <= 0 || fontIndex >= IDS.length) return c;
        Identifier id = Identifier.fromNamespaceAndPath("lumen", IDS[fontIndex]);
        return c.copy().withStyle(st -> st.withFont(new FontDescription.Resource(id)));
    }
}
