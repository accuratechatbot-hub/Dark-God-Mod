package com.darkgod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Persistent per-player state, stored in the player's persistent data (survives death and relog). */
public final class DarkGodData {
    private static final String KEY_GOD = "darkgod";
    private static final String KEY_AURA_OFF = "darkgod_aura_off";
    private static final String KEY_BRIDGE = "darkgod_bridge";

    private DarkGodData() {}

    private static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, 10)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static boolean isDarkGod(Player p) { return persisted(p).getBoolean(KEY_GOD); }
    public static void setDarkGod(Player p, boolean v) { persisted(p).putBoolean(KEY_GOD, v); }

    public static boolean isAuraOn(Player p) { return !persisted(p).getBoolean(KEY_AURA_OFF); }
    public static void setAuraOn(Player p, boolean on) { persisted(p).putBoolean(KEY_AURA_OFF, !on); }

    /** Tag we use to remember the stats we changed in Godhood Core so they can be restored. */
    public static CompoundTag bridgeTag(Player p) {
        CompoundTag c = persisted(p);
        if (!c.contains(KEY_BRIDGE, 10)) {
            c.put(KEY_BRIDGE, new CompoundTag());
        }
        return c.getCompound(KEY_BRIDGE);
    }

    public static void clearBridgeTag(Player p) { persisted(p).remove(KEY_BRIDGE); }

    /** Copy everything from an old player (death / dimension clone). */
    public static void copy(Player from, Player to) {
        CompoundTag old = from.getPersistentData();
        if (old.contains(Player.PERSISTED_NBT_TAG, 10)) {
            to.getPersistentData().put(Player.PERSISTED_NBT_TAG, old.getCompound(Player.PERSISTED_NBT_TAG).copy());
        }
    }
}
