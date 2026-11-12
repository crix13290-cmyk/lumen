# Lumen – Minecraft client (Fabric)

Features (v0.1): clean HD menu (Right Shift), Spotify lyrics (lines / karaoke / single words, on screen or floating in the air),
hit colour (gold preset, any colour), accent colour + corner radius for the menu.

## Build
1. Open https://fabricmc.net/develop and copy the versions for YOUR Minecraft version into `gradle.properties`
   (minecraft_version, loader_version, loom_version, fabric_api_version).
2. Needs JDK 25 (Minecraft 26.x). Run `gradle wrapper` once (or copy the wrapper from the Fabric example mod), then `./gradlew build`.
3. Jar is in `build/libs/` -> put it in your mods folder together with Fabric API.

## HD text (optional but recommended)
Put `clean.ttf` (e.g. Inter) and `mono.ttf` into `src/main/resources/assets/lumen/font/`. Then pick Font = Clean / Mono in the menu.

## Spotify
Menu > Music > Spotify: create an app on developer.spotify.com, redirect URI `http://127.0.0.1:8888/callback`,
paste Client ID, Connect. Lyrics come from LRCLIB (Spotify itself offers no lyrics API). Word timing is estimated from the line time.
Air mode: look where you want the lyrics, press J.

## NOT TESTED – likely fix points (all isolated)
This code was written without a compiler. Minecraft 26.x changed rendering/input APIs a lot, so expect small compile errors in:
1. gui/Fonts.java (Style font API)  2. gui/Draw.java (GuiGraphics pose / drawString)  3. mixin/LivingEntityRendererMixin.java (method names, hit tint)
4. gui/ClickGui.java (mouse event signatures)  5. LumenClient.java (KeyMapping category, HUD registry).
Paste the error list to Claude and each is a one-line fix.

## Build ohne Installation (GitHub)
1. Neues Repository auf github.com erstellen, den Inhalt dieses Ordners hochladen (inkl. .github).
2. Tab "Actions" > "build" laufen lassen. Danach unter "Artifacts" das lumen-jar herunterladen.
3. Bei einem Fehler: das rote Log kopieren und an Claude schicken.
