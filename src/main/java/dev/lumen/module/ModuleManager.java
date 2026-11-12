package dev.lumen.module;

import java.util.ArrayList;
import java.util.List;

/** Add new modules by creating them in LumenClient and calling ModuleManager.register(...). */
public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    public static final String[] CATEGORIES = {"Visuals", "Music", "Settings"};

    private ModuleManager() {}

    public static <T extends Module> T register(T m) { ALL.add(m); return m; }

    public static List<Module> inCategory(String c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category.equals(c)) out.add(m);
        return out;
    }
}
