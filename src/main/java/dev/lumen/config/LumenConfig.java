package dev.lumen.config;

import com.google.gson.*;
import dev.lumen.module.Feature;
import dev.lumen.module.ModuleManager;
import dev.lumen.module.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LumenConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("lumen.json");

    public static String spotifyClientId = "";
    public static String spotifyRefreshToken = "";

    private LumenConfig() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (root.has("spotifyClientId")) spotifyClientId = root.get("spotifyClientId").getAsString();
            if (root.has("spotifyRefreshToken")) spotifyRefreshToken = root.get("spotifyRefreshToken").getAsString();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Feature m : ModuleManager.ALL) {
                if (!mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("enabled")) { m.enabled = o.get("enabled").getAsBoolean(); m.anim = m.enabled ? 1f : 0f; }
                JsonObject s = o.has("settings") ? o.getAsJsonObject("settings") : new JsonObject();
                for (Setting st : m.settings) {
                    try { if (s.has(st.name)) st.load(s.get(st.name)); } catch (Exception ignored) { }
                }
            }
        } catch (Exception e) {
            System.err.println("[Lumen] Could not read config: " + e);
        }
    }

    public static synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("spotifyClientId", spotifyClientId);
            root.addProperty("spotifyRefreshToken", spotifyRefreshToken);
            JsonObject mods = new JsonObject();
            for (Feature m : ModuleManager.ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                JsonObject s = new JsonObject();
                for (Setting st : m.settings) s.add(st.name, st.save());
                o.add("settings", s);
                mods.add(m.name, o);
            }
            root.add("modules", mods);
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) {
            System.err.println("[Lumen] Could not save config: " + e);
        }
    }
}
