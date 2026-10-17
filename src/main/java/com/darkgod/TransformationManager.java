package com.darkgod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ascension: a ~6.5 second cinematic after eating the Command Block.
 * The player rises, the sky goes dark, lightning falls, then the Dark God is born in a shockwave.
 */
public final class TransformationManager {
    public static final int DURATION = 130;
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    private TransformationManager() {}

    public static boolean isTransforming(UUID id) { return ACTIVE.containsKey(id); }

    public static void begin(ServerPlayer p) {
        if (ACTIVE.containsKey(p.getUUID()) || DarkGodData.isDarkGod(p)) return;
        ACTIVE.put(p.getUUID(), 0);
        ServerLevel lvl = p.serverLevel();

        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION + 20, 4, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION + 5, 6, false, false));
        p.setNoGravity(true);

        for (ServerPlayer o : lvl.players()) {
            if (o.distanceToSqr(p) < 64.0 * 64.0) {
                o.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DURATION + 40, 0, false, false));
            }
        }
        lvl.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.5f);
        p.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.darkgod.awaken", p.getDisplayName()).withStyle(ChatFormatting.DARK_RED), false);
    }

    /** Called every server tick for every player. */
    public static void tick(ServerPlayer p) {
        Integer cur = ACTIVE.get(p.getUUID());
        if (cur == null) return;
        if (!p.isAlive()) { cancel(p); return; }

        int t = cur + 1;
        ACTIVE.put(p.getUUID(), t);
        ServerLevel lvl = p.serverLevel();

        // Rise slowly, then hover.
        p.setDeltaMovement(0, t < 90 ? 0.03 : 0.0, 0);
        p.hurtMarked = true;
        p.fallDistance = 0.0f;

        // Spiralling dark energy
        double radius = Math.max(0.6, 3.0 - t * 0.018);
        for (int i = 0; i < 3; i++) {
            double a = t * 0.35 + i * (Math.PI * 2.0 / 3.0);
            double x = p.getX() + Math.cos(a) * radius;
            double z = p.getZ() + Math.sin(a) * radius;
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, p.getY() + 0.2 + (t % 40) * 0.05, z, 1, 0, 0.05, 0, 0.01);
            lvl.sendParticles(DarkPowers.RED_DUST, x, p.getY() + 1.0, z, 2, 0.1, 0.4, 0.1, 0.0);
        }
        lvl.sendParticles(DarkPowers.BLACK_DUST, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.8, 1.0, 0.8, 0.0);

        if (t == 40) lvl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0f, 0.6f);
        if (t == 85) lvl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0f, 0.6f);

        if (t % 14 == 0 && t > 20) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(lvl);
            if (bolt != null) {
                double ang = lvl.random.nextDouble() * Math.PI * 2;
                double dist = 6 + lvl.random.nextDouble() * 10;
                BlockPos at = BlockPos.containing(p.getX() + Math.cos(ang) * dist, p.getY(), p.getZ() + Math.sin(ang) * dist);
                bolt.moveTo(Vec3.atBottomCenterOf(lvl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, at)));
                bolt.setVisualOnly(true);
                lvl.addFreshEntity(bolt);
            }
        }

        if (t >= DURATION) finish(p, lvl);
    }

    public static void cancel(ServerPlayer p) {
        if (ACTIVE.remove(p.getUUID()) != null) {
            p.setNoGravity(false);
        }
    }

    private static void finish(ServerPlayer p, ServerLevel lvl) {
        ACTIVE.remove(p.getUUID());
        p.setNoGravity(false);
        p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        Vec3 c = p.position();

        // Shockwave
        lvl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 3, 2, 1, 2, 0);
        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 3.5;
            int n = 40 + ring * 20;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                lvl.sendParticles(ring % 2 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.LARGE_SMOKE,
                        c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
                lvl.sendParticles(DarkPowers.RED_DUST, c.x + Math.cos(a) * r, c.y + 0.5, c.z + Math.sin(a) * r, 1, 0, 0.3, 0, 0);
            }
        }
        lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 5.0f, 0.5f);
        lvl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 6.0f, 0.6f);

        AABB box = p.getBoundingBox().inflate(30);
        List<LivingEntity> victims = lvl.getEntitiesOfClass(LivingEntity.class, box, e -> e != p);
        for (LivingEntity e : victims) {
            Vec3 d = new Vec3(e.getX() - c.x, 0, e.getZ() - c.z).normalize();
            e.setDeltaMovement(d.x * 3.0, 1.2, d.z * 3.0);
            e.hurtMarked = true;
            if (!(e instanceof Player)) e.hurt(lvl.damageSources().playerAttack(p), 40.0f);
        }

        // The world turns stormy.
        lvl.setWeatherParameters(0, 12000, true, true);

        // Become the Dark God.
        DarkPowers.grant(p);

        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 30));
        p.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("title.darkgod.main").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("title.darkgod.sub").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }
}
