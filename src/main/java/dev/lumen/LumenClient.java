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
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class LumenClient implements ClientModInitializer {
    private static KeyMapping openKey, anchorKey;

    @Override
    public void onInitializeClient() {
        Modules.registerAll();
        LumenConfig.load();
        SpotifyPlayer.start();

        // ADJUSTMENT POINT 5: KeyMapping category API changed in recent versions.
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.lumen.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
        anchorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.lumen.anchor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) mc.setScreen(new ClickGui());
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
