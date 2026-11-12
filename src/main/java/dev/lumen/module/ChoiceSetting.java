package dev.lumen.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ChoiceSetting extends Setting {
    public final String[] options;
    public int index;

    public ChoiceSetting(String name, String example, int def, String... options) {
        super(name, example);
        this.options = options;
        this.index = def;
    }

    public String get() { return options[index]; }
    public void cycle(int dir) { index = Math.floorMod(index + dir, options.length); }

    @Override public JsonElement save() { return new JsonPrimitive(index); }
    @Override public void load(JsonElement e) { index = Math.floorMod(e.getAsInt(), options.length); }
}
