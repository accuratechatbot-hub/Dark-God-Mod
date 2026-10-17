package com.darkgod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Everything a Dark God has passively: stats, regeneration, flight, aura, corruption. */
public final class DarkPowers {
    public static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.85f, 0.02f, 0.04f), 1.6f);
    public static final DustParticleOptions BLACK_DUST = new DustParticleOptions(new Vector3f(0.05f, 0.0f, 0.08f), 2.0f);

    private static final UUID ID_HEALTH = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0001");
    private static final UUID ID_ATTACK = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0002");
    private static final UUID ID_ARMOR = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0003");
    private static final UUID ID_TOUGH = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0004");
    private static final UUID ID_KB = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0005");
    private static final UUID ID_SPEED = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0006");
    private static final UUID ID_REACH_E = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0007");
    private static final UUID ID_REACH_B = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0008");
    private static final UUID ID_STEP = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a0009");
    private static final UUID ID_ASPEED = UUID.fromString("b3b1d0a4-5a3e-4c7e-9f0a-1d0a6f0a000a");

    private DarkPowers() {}

    // ------------------------------------------------------------------ grant / revoke

    /** Make this player a Dark God right now (no cinematic). */
    public static void grant(ServerPlayer p) {
        DarkGodData.setDarkGod(p, true);
        applyAttributes(p);
        ViltrumiteBridge.grantSafe(p);
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
    }

    public static void revoke(ServerPlayer p) {
        DarkGodData.setDarkGod(p, false);
        removeAttributes(p);
        ViltrumiteBridge.revokeSafe(p);
        Abilities a = p.getAbilities();
        if (!p.isCreative() && !p.isSpectator() && a.mayfly) {
            a.mayfly = false;
            a.flying = false;
            a.setFlyingSpeed(0.05f);
            p.onUpdateAbilities();
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Eating another Command Block while already a god. */
    public static void feast(ServerPlayer p) {
        p.setHealth(p.getMaxHealth());
        DarkAbilities.resetCooldowns(p);
        ServerLevel lvl = p.serverLevel();
        lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, 1, 1, 1, 0.1);
        p.displayClientMessage(net.minecraft.network.chat.Component.literal("§4The Command Block feeds your godhood. All powers restored."), true);
    }

    // ------------------------------------------------------------------ attributes

    private static void put(Player p, Attribute attr, UUID id, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null) {
            if (existing.getAmount() == amount) return;
            inst.removeModifier(id);
        }
        inst.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    private static void drop(Player p, Attribute attr, UUID id) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }

    public static void applyAttributes(Player p) {
        put(p, Attributes.MAX_HEALTH, ID_HEALTH, "Dark God health", DarkGodConfig.HEALTH_BONUS.get(), AttributeModifier.Operation.ADDITION);
        put(p, Attributes.ATTACK_DAMAGE, ID_ATTACK, "Dark God strength", DarkGodConfig.ATTACK_BONUS.get(), AttributeModifier.Operation.ADDITION);
        put(p, Attributes.ATTACK_SPEED, ID_ASPEED, "Dark God swiftness", 1.5, AttributeModifier.Operation.ADDITION);
        put(p, Attributes.ARMOR, ID_ARMOR, "Dark God armor", DarkGodConfig.ARMOR_BONUS.get(), AttributeModifier.Operation.ADDITION);
        put(p, Attributes.ARMOR_TOUGHNESS, ID_TOUGH, "Dark God toughness", 20.0, AttributeModifier.Operation.ADDITION);
        put(p, Attributes.KNOCKBACK_RESISTANCE, ID_KB, "Dark God immovable", 1.0, AttributeModifier.Operation.ADDITION);
        put(p, Attributes.MOVEMENT_SPEED, ID_SPEED, "Dark God speed", DarkGodConfig.SPEED_MULTIPLIER.get(), AttributeModifier.Operation.MULTIPLY_BASE);
        put(p, ForgeMod.ENTITY_REACH.get(), ID_REACH_E, "Dark God entity reach", DarkGodConfig.REACH_BONUS.get(), AttributeModifier.Operation.ADDITION);
        put(p, ForgeMod.BLOCK_REACH.get(), ID_REACH_B, "Dark God block reach", DarkGodConfig.REACH_BONUS.get(), AttributeModifier.Operation.ADDITION);
        put(p, ForgeMod.STEP_HEIGHT_ADDITION.get(), ID_STEP, "Dark God stride", 1.0, AttributeModifier.Operation.ADDITION);
    }

    public static void removeAttributes(Player p) {
        drop(p, Attributes.MAX_HEALTH, ID_HEALTH);
        drop(p, Attributes.ATTACK_DAMAGE, ID_ATTACK);
        drop(p, Attributes.ATTACK_SPEED, ID_ASPEED);
        drop(p, Attributes.ARMOR, ID_ARMOR);
        drop(p, Attributes.ARMOR_TOUGHNESS, ID_TOUGH);
        drop(p, Attributes.KNOCKBACK_RESISTANCE, ID_KB);
        drop(p, Attributes.MOVEMENT_SPEED, ID_SPEED);
        drop(p, ForgeMod.ENTITY_REACH.get(), ID_REACH_E);
        drop(p, ForgeMod.BLOCK_REACH.get(), ID_REACH_B);
        drop(p, ForgeMod.STEP_HEIGHT_ADDITION.get(), ID_STEP);
    }

    // ------------------------------------------------------------------ per-tick

    public static void tick(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();

        // Flight (Godhood Core brings its own flight system when it is active).
        if (!ViltrumiteBridge.isActive() && !p.isCreative() && !p.isSpectator()) {
            Abilities a = p.getAbilities();
            if (!a.mayfly) {
                a.mayfly = true;
                a.setFlyingSpeed(0.12f);
                p.onUpdateAbilities();
            }
        }

        if (p.tickCount % 20 == 0) {
            applyAttributes(p);
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(20.0f);
            p.setAirSupply(p.getMaxAirSupply());
            if (p.getHealth() < p.getMaxHealth()) p.heal(p.getMaxHealth() * 0.02f);

            for (MobEffectInstance inst : new ArrayList<>(p.getActiveEffects())) {
                if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                    p.removeEffect(inst.getEffect());
                }
            }
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 400, 0, false, false, false));
        }

        // Ambient dread
        if (p.tickCount % 3 == 0) {
            lvl.sendParticles(RED_DUST, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.5, 0.9, 0.5, 0.0);
            lvl.sendParticles(BLACK_DUST, p.getX(), p.getY() + 0.3, p.getZ(), 3, 0.7, 0.2, 0.7, 0.0);
        }
        if (p.tickCount % 10 == 0) {
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getEyeY() - 0.1, p.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
        }

        if (DarkGodData.isAuraOn(p)) {
            if (p.tickCount % 10 == 0) aura(p, lvl);
            if (DarkGodConfig.CORRUPTION_ENABLED.get() && p.tickCount % 20 == 0) corrupt(p, lvl);
        }
    }

    // ------------------------------------------------------------------ dark aura

    private static boolean isAlly(Player owner, LivingEntity e) {
        if (e instanceof TamableAnimal t && t.isOwnedBy(owner)) return true;
        return e.isAlliedTo(owner);
    }

    private static void aura(ServerPlayer p, ServerLevel lvl) {
        double r = DarkGodConfig.AURA_RADIUS.get();
        List<LivingEntity> list = lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !isAlly(p, e));
        for (LivingEntity e : list) {
            if (e.distanceToSqr(p) > r * r) continue;
            if (e instanceof Player && !DarkGodConfig.AURA_AFFECTS_PLAYERS.get()) continue;
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            if (e instanceof Enemy && e.distanceToSqr(p) < 100.0) {
                e.hurt(lvl.damageSources().magic(), 4.0f);
            }
        }
    }

    // Wither-Storm style corruption of the surrounding terrain.
    private static void corrupt(ServerPlayer p, ServerLevel lvl) {
        int r = DarkGodConfig.CORRUPTION_RADIUS.get();
        BlockPos origin = p.blockPosition();
        for (int i = 0; i < 10; i++) {
            BlockPos pos = origin.offset(
                    lvl.random.nextInt(r * 2 + 1) - r,
                    lvl.random.nextInt(13) - 6,
                    lvl.random.nextInt(r * 2 + 1) - r);
            if (pos.distSqr(origin) > (double) r * r) continue;
            if (!lvl.isLoaded(pos) || !lvl.mayInteract(p, pos)) continue;

            BlockState s = lvl.getBlockState(pos);
            Block target = null;
            if (s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                    || s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN)) {
                target = Blocks.AIR;
            } else if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.PODZOL)
                    || s.is(Blocks.MYCELIUM) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT)) {
                target = Blocks.SOUL_SOIL;
            } else if (s.is(Blocks.SAND) || s.is(Blocks.RED_SAND)) {
                target = Blocks.SOUL_SAND;
            } else if (s.is(Blocks.STONE) || s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE)
                    || s.is(Blocks.GRANITE) || s.is(Blocks.COBBLESTONE)) {
                target = Blocks.BLACKSTONE;
            } else if (s.is(Blocks.DEEPSLATE)) {
                target = Blocks.POLISHED_BLACKSTONE;
            }
            if (target != null) {
                lvl.setBlock(pos, target.defaultBlockState(), 3);
                lvl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
            }
        }
    }
}
