package dev.lumen.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumSetting extends Setting {
    public final double min, max, step;
    public double value;

    public NumSetting(String name, String example, double min, double max, double step, double def) {
        super(name, example);
        this.min = min; this.max = max; this.step = step; this.value = def;
    }

    public void set(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) v = Math.round(v / step) * step;
        value = v;
    }

    public float f() { return (float) value; }
    public double norm() { return (value - min) / (max - min); }

    @Override public JsonElement save() { return new JsonPrimitive(value); }
    @Override public void load(JsonElement e) { set(e.getAsDouble()); }
}
