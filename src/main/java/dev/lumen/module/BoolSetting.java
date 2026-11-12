package dev.lumen.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting {
    public boolean value;
    public float anim; // 0..1 for the switch animation

    public BoolSetting(String name, String example, boolean def) {
        super(name, example);
        this.value = def;
        this.anim = def ? 1f : 0f;
    }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { value = e.getAsBoolean(); anim = value ? 1f : 0f; }
}
