package com.darkgod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** The five active powers. IDs match the keybinds / network packet. */
public final class DarkAbilities {
    public static final int BEAM = 0;
    public static final int GRAVITY = 1;
    public static final int BOOM_TUBE = 2;
    public static final int SNAP = 3;
    public static final int AURA = 4;
    private static final int COUNT = 5;

    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();

    private DarkAbilities() {}

    private static long[] cds(ServerPlayer p) {
        return COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new long[COUNT]);
    }

    public static void resetCooldowns(ServerPlayer p) {
        COOLDOWNS.remove(p.getUUID());
    }

    public static void activate(ServerPlayer p, int id) {
        if (id < 0 || id >= COUNT) return;
        if (!DarkGodData.isDarkGod(p)) {
            p.displayClientMessage(Component.translatable("message.darkgod.notgod"), true);
            return;
        }
        if (TransformationManager.isTransforming(p.getUUID())) return;

        long now = p.level().getGameTime();
        long[] cd = cds(p);
        if (now < cd[id]) {
            long secs = (cd[id] - now + 19) / 20;
            p.displayClientMessage(Component.translatable("message.darkgod.cooldown", name(id), secs), true);
            return;
        }

        switch (id) {
            case BEAM -> { omegaBeams(p); cd[id] = now + 15; }
            case GRAVITY -> { gravityCrush(p); cd[id] = now + 20 * 12; }
            case BOOM_TUBE -> { if (boomTube(p)) cd[id] = now + 20 * 4; }
            case SNAP -> { snap(p); cd[id] = now + 20L * DarkGodConfig.SNAP_COOLDOWN_SECONDS.get(); }
            case AURA -> { toggleAura(p); cd[id] = now + 10; }
            default -> { }
        }
    }

    private static String name(int id) {
        return switch (id) {
            case BEAM -> "Omega Beams";
            case GRAVITY -> "Gravity Crush";
            case BOOM_TUBE -> "Boom Tube";
            case SNAP -> "The Snap";
            default -> "Dark Aura";
        };
    }

    private static boolean isAlly(ServerPlayer p, LivingEntity e) {
        return (e instanceof TamableAnimal t && t.isOwnedBy(p)) || e.isAlliedTo(p);
    }

    // ------------------------------------------------------------------ Omega Beams

    private static void omegaBeams(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 dir = p.getLookAngle();
        double range = 96.0;
        Vec3 far = eye.add(dir.scale(range));

        BlockHitResult hit = lvl.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        boolean hitBlock = hit.getType() != HitResult.Type.MISS;
        Vec3 end = hitBlock ? hit.getLocation() : far;
        double dist = end.distanceTo(eye);

        // Two beams, one per eye.
        Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize().scale(0.18);
        Vec3 start = eye.add(dir.scale(0.4)).subtract(0, 0.05, 0);
        for (double d = 0; d < dist; d += 0.5) {
            Vec3 pt = start.add(dir.scale(d));
            lvl.sendParticles(DarkPowers.RED_DUST, pt.x + side.x, pt.y, pt.z + side.z, 1, 0.02, 0.02, 0.02, 0);
            lvl.sendParticles(DarkPowers.RED_DUST, pt.x - side.x, pt.y, pt.z - side.z, 1, 0.02, 0.02, 0.02, 0);
            if (((int) (d * 2)) % 6 == 0) {
                lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pt.x, pt.y, pt.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }

        // Damage everything the beam touches.
        AABB box = new AABB(eye, end).inflate(2.0);
        List<LivingEntity> targets = lvl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != p && e.isAlive() && !e.isSpectator() && !isAlly(p, e));
        for (LivingEntity e : targets) {
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.7).clip(eye, end);
            if (clip.isPresent() || e.getBoundingBox().inflate(0.7).contains(end)) {
                e.hurt(lvl.damageSources().playerAttack(p), DarkGodConfig.BEAM_DAMAGE.get().floatValue());
                e.setSecondsOnFire(10);
            }
        }

        // Impact
        Level.ExplosionInteraction mode = DarkGodConfig.BEAM_BREAKS_BLOCKS.get()
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        if (hitBlock) {
            lvl.explode(p, end.x, end.y, end.z, 4.0f, mode);
        }
        lvl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0f, 0.7f);
    }

    // ------------------------------------------------------------------ Gravity Crush

    private static void gravityCrush(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        double r = 24.0;
        Vec3 c = p.position();

        List<LivingEntity> list = lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !isAlly(p, e));
        for (LivingEntity e : list) {
            if (e.distanceToSqr(p) > r * r) continue;
            Vec3 pull = c.subtract(e.position());
            Vec3 v = pull.lengthSqr() < 1.0 ? Vec3.ZERO : pull.normalize().scale(1.4);
            e.setDeltaMovement(v.x, 0.9, v.z);
            e.hurtMarked = true;
            e.hurt(lvl.damageSources().playerAttack(p), 20.0f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
        }

        for (int ring = 1; ring <= 3; ring++) {
            double rr = r * ring / 3.0;
            int n = 30 + ring * 25;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                lvl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x + Math.cos(a) * rr, c.y + 0.5, c.z + Math.sin(a) * rr,
                        1, 0, 0.2, 0, -0.3);
            }
        }
        lvl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 0.5f);
        lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.4f);
    }

    // ------------------------------------------------------------------ Boom Tube

    private static boolean boomTube(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 far = eye.add(p.getLookAngle().scale(256.0));
        BlockHitResult hit = lvl.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.MISS) {
            p.displayClientMessage(Component.literal("§7The Boom Tube needs something to anchor to."), true);
            return false;
        }
        BlockPos dest = hit.getBlockPos().relative(hit.getDirection());
        Vec3 from = p.position();

        tubeFx(lvl, from);
        p.teleportTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5);
        p.fallDistance = 0.0f;
        tubeFx(lvl, p.position());

        lvl.playSound(null, BlockPos.containing(from), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.5f);
        lvl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 2.0f, 0.5f);
        return true;
    }

    private static void tubeFx(ServerLevel lvl, Vec3 at) {
        lvl.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1, at.z, 80, 0.6, 1.0, 0.6, 0.6);
        lvl.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 25, 0.4, 0.8, 0.4, 0.15);
        lvl.sendParticles(DarkPowers.RED_DUST, at.x, at.y + 1, at.z, 30, 0.6, 1.0, 0.6, 0.0);
    }

    // ------------------------------------------------------------------ The Snap

    private static void snap(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        double r = DarkGodConfig.SNAP_RADIUS.get();
        boolean hitPlayers = DarkGodConfig.SNAP_AFFECTS_PLAYERS.get();

        List<LivingEntity> list = lvl.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r),
                e -> e != p && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && !isAlly(p, e)
                        && !e.getType().is(Tags.EntityTypes.BOSSES)
                        && (hitPlayers || !(e instanceof net.minecraft.world.entity.player.Player)));

        int dusted = 0;
        for (LivingEntity e : list) {
            if (e.distanceToSqr(p) > r * r) continue;
            if (lvl.random.nextBoolean()) continue; // perfectly balanced
            lvl.sendParticles(ParticleTypes.ASH, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 60,
                    e.getBbWidth() / 2, e.getBbHeight() / 2, e.getBbWidth() / 2, 0.08);
            lvl.sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(), 15,
                    e.getBbWidth() / 3, e.getBbHeight() / 3, e.getBbWidth() / 3, 0.03);
            if (e instanceof net.minecraft.world.entity.player.Player) {
                e.hurt(lvl.damageSources().genericKill(), Float.MAX_VALUE);
            } else {
                e.discard(); // turned to dust: no drops, no corpse
            }
            dusted++;
        }

        lvl.sendParticles(ParticleTypes.FLASH, p.getX(), p.getEyeY(), p.getZ(), 1, 0, 0, 0, 0);
        for (ServerPlayer o : lvl.players()) {
            if (o.distanceToSqr(p) < 48.0 * 48.0) {
                o.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            }
        }
        lvl.playSound(null, p.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 3.0f, 0.5f);
        lvl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 1.5f, 0.6f);
        p.displayClientMessage(Component.literal("§4Perfectly balanced, as all things should be. §7(" + dusted + " turned to dust)"), true);
    }

    // ------------------------------------------------------------------ Aura toggle

    private static void toggleAura(ServerPlayer p) {
        boolean on = !DarkGodData.isAuraOn(p);
        DarkGodData.setAuraOn(p, on);
        p.displayClientMessage(Component.literal(on ? "§4Dark Aura: §cACTIVE" : "§4Dark Aura: §7dormant"), true);
    }
}
