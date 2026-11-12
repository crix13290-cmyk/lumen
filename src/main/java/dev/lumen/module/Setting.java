package dev.lumen.module;

import com.google.gson.JsonElement;

/** Base class for every module setting. 'example' is the hint shown next to it in the menu. */
public abstract class Setting {
    public final String name;
    public final String example;

    protected Setting(String name, String example) {
        this.name = name;
        this.example = example;
    }

    public abstract JsonElement save();
    public abstract void load(JsonElement e);
}
