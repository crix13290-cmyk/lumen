package dev.lumen.mixin;

import dev.lumen.fx.HitEffect;
import dev.lumen.fx.Modules;
import dev.lumen.gui.Draw;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ADJUSTMENT POINT 3 (best effort, untested): tints a hit entity.
 * If Minecraft renamed these methods, the injections are skipped (require = 0) and only the tint is missing.
 * Check the real names with the Minecraft sources in your IDE: LivingEntityRenderer#extractRenderState and #getModelTint.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("HEAD"), require = 0)
    private void lumen$capture(LivingEntity entity, @org.spongepowered.asm.mixin.injection.Coerce Object state, float partialTick, CallbackInfo ci) {
        HitEffect.CURRENT.set(entity);
    }

    @Inject(method = "getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void lumen$tint(@org.spongepowered.asm.mixin.injection.Coerce Object state, CallbackInfoReturnable<Integer> cir) {
        float k = HitEffect.intensity(HitEffect.CURRENT.get());
        if (k <= 0f) return;
        int base = cir.getReturnValue();
        int target = 0xFF000000 | Modules.H_COLOR.rgb();
        cir.setReturnValue(Draw.mix(base, target, k) | 0xFF000000);
    }
}
