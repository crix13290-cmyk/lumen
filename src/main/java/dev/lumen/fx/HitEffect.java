package dev.lumen.fx;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Remembers when each entity was last hit and turns that into a 0..1 intensity. */
public final class HitEffect {
    private static final Map<Integer, Long> LAST_HIT = new HashMap<>();
    /** Set by the renderer mixin while an entity's render state is being built. */
    public static final ThreadLocal<Entity> CURRENT = new ThreadLocal<>();

    private HitEffect() {}

    public static void onAttack(Entity target) {
        if (!Modules.HIT.enabled) return;
        if (Modules.H_ONLY_PLAYERS.value && !(target instanceof Player)) return;
        LAST_HIT.put(target.getId(), System.currentTimeMillis());
    }

    /** 1 right after the hit, fading to 0. */
    public static float intensity(Entity e) {
        if (e == null || !Modules.HIT.enabled) return 0f;
        Long t = LAST_HIT.get(e.getId());
        if (t == null) return 0f;
        float age = System.currentTimeMillis() - t;
        float dur = Modules.H_TIME.f();
        if (age >= dur) { LAST_HIT.remove(e.getId()); return 0f; }
        float x = 1f - age / dur;
        return x * x * (3f - 2f * x) * Modules.H_STRENGTH.f(); // smoothstep fade-out
    }
}
