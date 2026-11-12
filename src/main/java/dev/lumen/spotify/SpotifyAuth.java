package dev.lumen.spotify;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lumen.config.LumenConfig;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Spotify login with the PKCE flow: no client secret needed.
 * 1) Create an app at developer.spotify.com/dashboard
 * 2) Add the redirect URI  http://127.0.0.1:8888/callback
 * 3) Paste the Client ID into the Lumen menu (Music > Spotify) and press Connect.
 */
public final class SpotifyAuth {
    public static final String REDIRECT = "http://127.0.0.1:8888/callback";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static volatile String status = "Not connected";
    private static volatile String accessToken = "";
    private static volatile long expiresAtMs = 0;

    private SpotifyAuth() {}

    public static boolean connected() { return !LumenConfig.spotifyRefreshToken.isEmpty(); }

    /** Returns a valid access token or null. Safe to call from any thread. */
    public static synchronized String token() {
        try {
            if (!connected()) return null;
            if (System.currentTimeMillis() < expiresAtMs - 30_000 && !accessToken.isEmpty()) return accessToken;
            String body = form("grant_type", "refresh_token",
                    "refresh_token", LumenConfig.spotifyRefreshToken,
                    "client_id", LumenConfig.spotifyClientId);
            JsonObject o = post(body);
            if (o.has("access_token")) {
                accessToken = o.get("access_token").getAsString();
                expiresAtMs = System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000;
                if (o.has("refresh_token")) {
                    LumenConfig.spotifyRefreshToken = o.get("refresh_token").getAsString();
                    LumenConfig.save();
                }
                status = "Connected";
                return accessToken;
            }
            status = "Login expired - connect again";
        } catch (Exception e) {
            status = "Error: " + e.getMessage();
        }
        return null;
    }

    public static void disconnect() {
        LumenConfig.spotifyRefreshToken = "";
        accessToken = "";
        status = "Not connected";
        LumenConfig.save();
    }

    /** Starts the browser login. Runs on its own thread. */
    public static void connect() {
        if (LumenConfig.spotifyClientId.isBlank()) { status = "Enter your Client ID first"; return; }
        status = "Waiting for browser login...";
        Thread t = new Thread(SpotifyAuth::runLogin, "Lumen-SpotifyLogin");
        t.setDaemon(true);
        t.start();
    }

    private static void runLogin() {
        try (ServerSocket server = new ServerSocket(8888, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(180_000);
            SecureRandom rnd = new SecureRandom();
            byte[] vb = new byte[48];
            rnd.nextBytes(vb);
            String verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(vb);
            String challenge = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            String state = Long.toHexString(rnd.nextLong());

            String url = "https://accounts.spotify.com/authorize?response_type=code"
                    + "&client_id=" + enc(LumenConfig.spotifyClientId)
                    + "&scope=" + enc("user-read-currently-playing user-read-playback-state")
                    + "&redirect_uri=" + enc(REDIRECT)
                    + "&code_challenge_method=S256&code_challenge=" + challenge
                    + "&state=" + state;
            openBrowser(url);

            try (Socket s = server.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                String line = in.readLine(); // GET /callback?code=...&state=... HTTP/1.1
                String code = null, gotState = null;
                if (line != null && line.contains("?")) {
                    String q = line.substring(line.indexOf('?') + 1, line.lastIndexOf(' '));
                    for (String p : q.split("&")) {
                        String[] kv = p.split("=", 2);
                        if (kv.length == 2 && kv[0].equals("code")) code = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        if (kv.length == 2 && kv[0].equals("state")) gotState = kv[1];
                    }
                }
                boolean ok = code != null && state.equals(gotState);
                String html = "<html><body style='background:#0b0714;color:#fff;font-family:sans-serif;text-align:center;padding-top:20vh'>"
                        + (ok ? "<h1>Lumen connected to Spotify</h1><p>You can close this tab.</p>" : "<h1>Login failed</h1>")
                        + "</body></html>";
                byte[] b = html.getBytes(StandardCharsets.UTF_8);
                OutputStream out = s.getOutputStream();
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + b.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(b);
                out.flush();
                if (!ok) { status = "Login failed"; return; }

                JsonObject o = post(form("grant_type", "authorization_code",
                        "code", code, "redirect_uri", REDIRECT,
                        "client_id", LumenConfig.spotifyClientId, "code_verifier", verifier));
                if (o.has("refresh_token")) {
                    LumenConfig.spotifyRefreshToken = o.get("refresh_token").getAsString();
                    accessToken = o.get("access_token").getAsString();
                    expiresAtMs = System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000;
                    LumenConfig.save();
                    status = "Connected";
                } else {
                    status = "Spotify refused the login";
                }
            }
        } catch (SocketTimeoutException e) {
            status = "Login timed out";
        } catch (BindException e) {
            status = "Port 8888 is in use";
        } catch (Exception e) {
            status = "Error: " + e.getMessage();
        }
    }

    private static JsonObject post(String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(res.body()).getAsJsonObject();
    }

    private static String form(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append('&');
            sb.append(enc(kv[i])).append('=').append(enc(kv[i + 1]));
        }
        return sb.toString();
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static void openBrowser(String url) throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
        else if (os.contains("mac")) new ProcessBuilder("open", url).start();
        else new ProcessBuilder("xdg-open", url).start();
    }
}
