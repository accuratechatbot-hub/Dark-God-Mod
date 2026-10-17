package com.darkgod;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DarkGodConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue HEALTH_BONUS;
    public static final ForgeConfigSpec.DoubleValue ATTACK_BONUS;
    public static final ForgeConfigSpec.DoubleValue ARMOR_BONUS;
    public static final ForgeConfigSpec.DoubleValue SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue REACH_BONUS;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_TAKEN_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue RESURRECT_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.DoubleValue BEAM_DAMAGE;
    public static final ForgeConfigSpec.BooleanValue BEAM_BREAKS_BLOCKS;
    public static final ForgeConfigSpec.IntValue SNAP_RADIUS;
    public static final ForgeConfigSpec.BooleanValue SNAP_AFFECTS_PLAYERS;
    public static final ForgeConfigSpec.IntValue SNAP_COOLDOWN_SECONDS;

    public static final ForgeConfigSpec.IntValue AURA_RADIUS;
    public static final ForgeConfigSpec.BooleanValue AURA_AFFECTS_PLAYERS;
    public static final ForgeConfigSpec.BooleanValue CORRUPTION_ENABLED;
    public static final ForgeConfigSpec.IntValue CORRUPTION_RADIUS;

    public static final ForgeConfigSpec.BooleanValue USE_GODHOOD_CORE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("body");
        HEALTH_BONUS = b.comment("Extra max health (20 base + this).").defineInRange("healthBonus", 380.0, 0.0, 10000.0);
        ATTACK_BONUS = b.comment("Extra attack damage.").defineInRange("attackBonus", 40.0, 0.0, 1000.0);
        ARMOR_BONUS = b.comment("Extra armor points (toughness is set to 20 on top).").defineInRange("armorBonus", 30.0, 0.0, 30.0);
        SPEED_MULTIPLIER = b.comment("Movement speed bonus as a fraction of base (0.4 = +40%).").defineInRange("speedBonus", 0.4, 0.0, 5.0);
        REACH_BONUS = b.comment("Extra block and entity reach.").defineInRange("reachBonus", 6.0, 0.0, 100.0);
        DAMAGE_TAKEN_MULTIPLIER = b.comment("Remaining damage multiplier for non-ignored damage (0.05 = take 5%).").defineInRange("damageTakenMultiplier", 0.05, 0.0, 1.0);
        RESURRECT_COOLDOWN_SECONDS = b.comment("A Dark God refuses death once per this many seconds. 0 disables resurrection.").defineInRange("resurrectCooldownSeconds", 300, 0, 100000);
        b.pop();

        b.push("abilities");
        BEAM_DAMAGE = b.comment("Omega Beams damage per hit.").defineInRange("beamDamage", 60.0, 0.0, 100000.0);
        BEAM_BREAKS_BLOCKS = b.comment("Omega Beams explosion destroys blocks.").define("beamBreaksBlocks", true);
        SNAP_RADIUS = b.comment("The Snap radius in blocks.").defineInRange("snapRadius", 96, 8, 512);
        SNAP_AFFECTS_PLAYERS = b.comment("The Snap can also dust other players (50% each).").define("snapAffectsPlayers", false);
        SNAP_COOLDOWN_SECONDS = b.comment("The Snap cooldown.").defineInRange("snapCooldownSeconds", 60, 1, 100000);
        b.pop();

        b.push("aura");
        AURA_RADIUS = b.comment("Dark Aura radius.").defineInRange("auraRadius", 20, 4, 128);
        AURA_AFFECTS_PLAYERS = b.comment("Dark Aura withers other players too.").define("auraAffectsPlayers", false);
        CORRUPTION_ENABLED = b.comment("The aura slowly corrupts the terrain around the Dark God (Wither Storm style).").define("corruptionEnabled", true);
        CORRUPTION_RADIUS = b.comment("Corruption radius.").defineInRange("corruptionRadius", 14, 2, 64);
        b.pop();

        b.push("compat");
        USE_GODHOOD_CORE = b.comment("If Godhood Core is installed, also grant its powers and stats.").define("useGodhoodCore", true);
        b.pop();

        SPEC = b.build();
    }

    private DarkGodConfig() {}
}
