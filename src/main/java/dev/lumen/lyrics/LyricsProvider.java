package dev.lumen.lyrics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Time-synced lyrics from LRCLIB (free, no key). Spotify's own lyrics are not available through
 * the public API, so we look the song up by title / artist / length instead.
 */
public final class LyricsProvider {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern TIME = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]");

    public static volatile List<LyricLine> lines = List.of();
    public static volatile String state = "No song";
    private static volatile String currentId = "";

    private LyricsProvider() {}

    public static void request(String id, String title, String artist, long durationMs) {
        currentId = id;
        lines = List.of();
        state = "Loading lyrics...";
        Thread t = new Thread(() -> load(id, title, artist, durationMs), "Lumen-Lyrics");
        t.setDaemon(true);
        t.start();
    }

    private static void load(String id, String title, String artist, long durationMs) {
        try {
            String synced = null;
            String get = "https://lrclib.net/api/get?track_name=" + enc(title) + "&artist_name=" + enc(artist)
                    + "&duration=" + Math.round(durationMs / 1000.0);
            JsonObject o = fetchObject(get);
            if (o != null && o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull())
                synced = o.get("syncedLyrics").getAsString();

            if (synced == null) { // fallback: search
                HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(
                        URI.create("https://lrclib.net/api/search?q=" + enc(artist + " " + title))).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    JsonArray arr = JsonParser.parseString(res.body()).getAsJsonArray();
                    for (JsonElement e : arr) {
                        JsonObject r = e.getAsJsonObject();
                        if (r.has("syncedLyrics") && !r.get("syncedLyrics").isJsonNull()) {
                            synced = r.get("syncedLyrics").getAsString();
                            break;
                        }
                    }
                }
            }
            if (!id.equals(currentId)) return; // song changed meanwhile
            if (synced == null) { state = "No synced lyrics found"; return; }
            lines = parse(synced);
            state = lines.isEmpty() ? "No synced lyrics found" : "OK";
        } catch (Exception e) {
            if (id.equals(currentId)) state = "Lyrics error";
        }
    }

    private static JsonObject fetchObject(String url) throws Exception {
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return null;
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    static List<LyricLine> parse(String lrc) {
        List<LyricLine> out = new ArrayList<>();
        for (String raw : lrc.split("\n")) {
            Matcher m = TIME.matcher(raw);
            if (!m.find()) continue;
            long min = Long.parseLong(m.group(1)), sec = Long.parseLong(m.group(2));
            String frac = m.group(3) == null ? "0" : m.group(3);
            long ms = Long.parseLong((frac + "00").substring(0, 3));
            String text = raw.substring(m.end()).trim();
            out.add(new LyricLine(min * 60_000 + sec * 1000 + ms, text));
        }
        return out;
    }

    /** Index of the line that is active at the given time, or -1 before the first line. */
    public static int indexAt(long ms) {
        List<LyricLine> l = lines;
        int idx = -1;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).startMs() <= ms) idx = i; else break;
        }
        return idx;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
