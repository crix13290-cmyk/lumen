package dev.lumen.module;

import java.util.ArrayList;
import java.util.List;

public class Feature {
    public final String name, description, category;
    public boolean enabled;
    public float anim;   // toggle animation 0..1
    public float hover;  // card hover animation 0..1
    public final List<Setting> settings = new ArrayList<>();

    public Feature(String name, String description, String category, boolean enabled) {
        this.name = name; this.description = description; this.category = category;
        this.enabled = enabled; this.anim = enabled ? 1f : 0f;
    }

    public <T extends Setting> T add(T s) { settings.add(s); return s; }
    public void toggle() { enabled = !enabled; }
}
