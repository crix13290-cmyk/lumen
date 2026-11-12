package dev.lumen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumen.config.LumenConfig;
import dev.lumen.fx.HitEffect;
import dev.lumen.fx.Modules;
import dev.lumen.gui.ClickGui;
import dev.lumen.gui.LyricsHud;
import dev.lumen.spotify.SpotifyPlayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public class LumenClient implements ClientModInitializer {
    private static KeyMapping openKey, anchorKey;
    private static final int KEY_RIGHT_SHIFT = 344, KEY_J = 74; // GLFW key codes

    @Override
    public void onInitializeClient() {
        Modules.registerAll();
        LumenConfig.load();
        SpotifyPlayer.start();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("lumen", "main"));
        InputConstants.Type kb = InputConstants.Type.values()[0]; // KEYSYM (keyboard)
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lumen.open", kb, KEY_RIGHT_SHIFT, cat));
        anchorKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lumen.anchor", kb, KEY_J, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) mc.gui.setScreen(new ClickGui());
            while (anchorKey.consumeClick()) LyricsHud.placeHere();
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("lumen", "lyrics"),
                (graphics, tickCounter) -> LyricsHud.render(graphics, tickCounter.getGameTimeDeltaPartialTick(false)));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide()) HitEffect.onAttack(entity);
            return InteractionResult.PASS;
        });
    }
}
