package com.darkgod;

import dev.baranhan.viltrumitecore.util.ViltrumiteCorePlayer;
import dev.baranhan.viltrumitecore.util.ViltrumiteStatHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Integration with Godhood Core (mod id "viltrumitecore").
 *
 * This is the ONLY class that touches Godhood Core classes. It is never loaded unless
 * {@link #isActive()} returned true, so Dark God still works if Godhood Core is absent.
 *
 * Godhood Core mixes ViltrumiteCorePlayer / ViltrumiteStatHolder into Player at runtime.
 * "Viltrumite" status is what unlocks its flight, punch, dash, thunderclap, grab, etc.
 */
public final class ViltrumiteBridge {
    private ViltrumiteBridge() {}

    public static boolean isActive() {
        return DarkGodConfig.USE_GODHOOD_CORE.get() && ModList.get().isLoaded("viltrumitecore");
    }

    /** Safe entry points: never throw, never load this class when the mod is missing. */
    public static void grantSafe(ServerPlayer p) {
        if (!isActive()) return;
        try {
            grant(p);
        } catch (Throwable t) {
            DarkGodMod.LOGGER.warn("Godhood Core integration failed on grant (continuing without it)", t);
        }
    }

    public static void revokeSafe(ServerPlayer p) {
        if (!ModList.get().isLoaded("viltrumitecore")) return;
        try {
            revoke(p);
        } catch (Throwable t) {
            DarkGodMod.LOGGER.warn("Godhood Core integration failed on revoke", t);
        }
    }

    private static void grant(ServerPlayer p) {
        Object o = p;
        CompoundTag saved = DarkGodData.bridgeTag(p);

        if (o instanceof ViltrumiteCorePlayer core) {
            if (!saved.contains("wasViltrumite")) {
                saved.putBoolean("wasViltrumite", core.isViltrumite());
            }
            core.setChosenRace(true);   // skip the race-choice screen
            core.setViltrumite(true);   // unlocks every Godhood Core power
        }

        if (o instanceof ViltrumiteStatHolder stats) {
            // Remember the originals once, then scale relative to them (we don't know the mod's units).
            if (!saved.contains("origDamage")) {
                saved.putFloat("origDamage", stats.getBaseDamage());
                saved.putFloat("origIgnore", stats.getDamageIgnoreThreshold());
                saved.putFloat("origReduction", stats.getDamageReduction());
                saved.putFloat("origHeal", stats.getHealFactor());
            }
            float orig = saved.getFloat("origReduction");
            // Reduction is a percent (config default 97). If it looks like a fraction, stay a fraction.
            stats.setDamageReduction(orig <= 1.0f ? 0.999f : 99.9f);
            stats.setDamageIgnoreThreshold(Math.max(saved.getFloat("origIgnore"), 4.0f));
            if (saved.getFloat("origDamage") > 0f) stats.setBaseDamage(saved.getFloat("origDamage") * 3.0f);
            if (saved.getFloat("origHeal") > 0f) stats.setHealFactor(saved.getFloat("origHeal") * 3.0f);
        }
    }

    private static void revoke(ServerPlayer p) {
        Object o = p;
        CompoundTag saved = DarkGodData.bridgeTag(p);

        if (o instanceof ViltrumiteStatHolder stats && saved.contains("origDamage")) {
            stats.setBaseDamage(saved.getFloat("origDamage"));
            stats.setDamageIgnoreThreshold(saved.getFloat("origIgnore"));
            stats.setDamageReduction(saved.getFloat("origReduction"));
            stats.setHealFactor(saved.getFloat("origHeal"));
        }
        if (o instanceof ViltrumiteCorePlayer core && saved.contains("wasViltrumite")) {
            core.setViltrumite(saved.getBoolean("wasViltrumite"));
        }
        DarkGodData.clearBridgeTag(p);
    }
}
