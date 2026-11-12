package dev.lumen.spotify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumen.lyrics.LyricsProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Polls "currently playing" and interpolates the progress between polls. */
public final class SpotifyPlayer {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final ScheduledExecutorService POOL = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Lumen-SpotifyPoll");
        t.setDaemon(true);
        return t;
    });

    public static volatile String trackId = "", title = "", artist = "";
    public static volatile long durationMs = 0;
    private static volatile long progressMs = 0;
    private static volatile boolean playing = false;
    private static volatile long fetchedAtNanos = 0;
    private static volatile boolean started = false;

    private SpotifyPlayer() {}

    public static synchronized void start() {
        if (started) return;
        started = true;
        POOL.scheduleWithFixedDelay(SpotifyPlayer::poll, 1, 2, TimeUnit.SECONDS);
    }

    public static boolean isPlaying() { return playing && !trackId.isEmpty(); }

    public static long progress() {
        if (!playing) return progressMs;
        return progressMs + (System.nanoTime() - fetchedAtNanos) / 1_000_000L;
    }

    private static void poll() {
        try {
            String token = SpotifyAuth.token();
            if (token == null) { playing = false; return; }
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                    .header("Authorization", "Bearer " + token).GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200 || res.body().isBlank()) { playing = false; return; }
            JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
            JsonElement item = o.get("item");
            if (item == null || !item.isJsonObject()) { playing = false; return; }
            JsonObject t = item.getAsJsonObject();

            String id = t.get("id").getAsString();
            String name = t.get("name").getAsString();
            String art = t.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
            long dur = t.get("duration_ms").getAsLong();

            progressMs = o.get("progress_ms").getAsLong();
            fetchedAtNanos = System.nanoTime();
            playing = o.get("is_playing").getAsBoolean();

            if (!id.equals(trackId)) {
                trackId = id; title = name; artist = art; durationMs = dur;
                LyricsProvider.request(id, name, art, dur);
            }
        } catch (Exception e) {
            playing = false;
        }
    }
}
