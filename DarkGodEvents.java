package com.darkgod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DarkGodMod.MODID)
public final class DarkGodEvents {
    /** Hold-to-eat time in ticks. Constant on purpose: client and server must agree. */
    public static final int EAT_TICKS = 40;

    private static final Map<UUID, Long> RESURRECT_READY = new HashMap<>();

    private DarkGodEvents() {}

    public static boolean isCommandBlock(ItemStack s) {
        return s.is(Items.COMMAND_BLOCK) || s.is(Items.CHAIN_COMMAND_BLOCK) || s.is(Items.REPEATING_COMMAND_BLOCK);
    }

    // ===================================================================== eating the Command Block
    // Sneak + hold right-click with a command block in hand to devour it.

    /** Stop the block from being placed when sneak-clicking on a block with it. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getEntity().isShiftKeyDown() && isCommandBlock(e.getItemStack())) {
            e.setUseItem(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        Player p = e.getEntity();
        if (!p.isShiftKeyDown() || !isCommandBlock(e.getItemStack())) return;
        if (!p.isUsingItem()) {
            p.startUsingItem(e.getHand());
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.CONSUME);
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (e.getEntity() instanceof Player && isCommandBlock(e.getItem())) {
            e.setDuration(EAT_TICKS);
        }
    }

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !isCommandBlock(e.getItem())) return;
        int elapsed = EAT_TICKS - e.getDuration();
        ServerLevel lvl = sp.serverLevel();
        if (elapsed % 4 == 0) {
            lvl.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 0.6f);
            lvl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, e.getItem()),
                    sp.getX(), sp.getEyeY() - 0.2, sp.getZ(), 8, 0.25, 0.2, 0.25, 0.05);
        }
        if (elapsed % 2 == 0) {
            lvl.sendParticles(DarkPowers.RED_DUST, sp.getX(), sp.getEyeY(), sp.getZ(), 3 + elapsed / 6, 0.4, 0.5, 0.4, 0.0);
        }
        if (elapsed == 20) {
            lvl.playSound(null, sp.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 2.0f, 0.5f);
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || !isCommandBlock(e.getItem())) return;

        ItemStack result = e.getResultStack().copy();
        if (!sp.getAbilities().instabuild) {
            result.shrink(1);
        }
        e.setResultStack(result);

        if (DarkGodData.isDarkGod(sp) || TransformationManager.isTransforming(sp.getUUID())) {
            if (DarkGodData.isDarkGod(sp)) DarkPowers.feast(sp);
        } else {
            TransformationManager.begin(sp);
        }
    }

    // ===================================================================== ticking

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        TransformationManager.tick(p);
        if (DarkGodData.isDarkGod(p)) {
            DarkPowers.tick(p);
        }
    }

    // ===================================================================== invulnerability

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        DamageSource s = e.getSource();
        if (s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill and the void

        if (TransformationManager.isTransforming(p.getUUID())) {
            e.setCanceled(true);
            return;
        }
        if (!DarkGodData.isDarkGod(p)) return;

        boolean ignored = s.is(DamageTypeTags.IS_FALL) || s.is(DamageTypeTags.IS_FIRE)
                || s.is(DamageTypeTags.IS_DROWNING) || s.is(DamageTypeTags.IS_EXPLOSION)
                || s.is(DamageTypeTags.IS_LIGHTNING)
                || s.is(DamageTypes.WITHER) || s.is(DamageTypes.MAGIC) || s.is(DamageTypes.FREEZE)
                || s.is(DamageTypes.IN_WALL) || s.is(DamageTypes.CRAMMING) || s.is(DamageTypes.STARVE)
                || s.is(DamageTypes.DRY_OUT) || s.is(DamageTypes.CACTUS) || s.is(DamageTypes.SWEET_BERRY_BUSH)
                || s.is(DamageTypes.HOT_FLOOR) || s.is(DamageTypes.FLY_INTO_WALL)
                || s.is(DamageTypes.FALLING_ANVIL) || s.is(DamageTypes.FALLING_STALACTITE)
                || s.is(DamageTypes.STALAGMITE);
        if (ignored || e.getAmount() <= 6.0f) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !DarkGodData.isDarkGod(p)) return;
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        e.setAmount(e.getAmount() * DarkGodConfig.DAMAGE_TAKEN_MULTIPLIER.get().floatValue());
    }

    // ===================================================================== refusing death

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !DarkGodData.isDarkGod(p)) return;
        DamageSource s = e.getSource();
        if (s.is(DamageTypes.GENERIC_KILL)) return; // /kill always works
        int cdSeconds = DarkGodConfig.RESURRECT_COOLDOWN_SECONDS.get();
        if (cdSeconds <= 0) return;

        long now = p.level().getGameTime();
        if (now < RESURRECT_READY.getOrDefault(p.getUUID(), 0L)) return;

        e.setCanceled(true);
        RESURRECT_READY.put(p.getUUID(), now + 20L * cdSeconds);

        ServerLevel lvl = p.serverLevel();
        if (s.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            BlockPos top = lvl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p.blockPosition());
            p.teleportTo(top.getX() + 0.5, top.getY() + 2.0, top.getZ() + 0.5);
        }
        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        p.fallDistance = 0.0f;
        p.clearFire();
        DarkPowers.applyAttributes(p);

        lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + 1, p.getZ(), 120, 1.0, 1.0, 1.0, 0.2);
        lvl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        lvl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0f, 0.5f);
        p.displayClientMessage(Component.literal("DARKNESS CANNOT BE UNMADE").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    // ===================================================================== persistence

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        DarkGodData.copy(e.getOriginal(), e.getEntity());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            p.setNoGravity(false); // in case they logged out mid-ascension
            DarkGodNet.syncTo(p);
            DarkGodNet.broadcast(p);
            if (DarkGodData.isDarkGod(p)) {
                DarkPowers.applyAttributes(p);
                ViltrumiteBridge.grantSafe(p);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DarkGodData.isDarkGod(p)) {
            DarkPowers.applyAttributes(p);
            ViltrumiteBridge.grantSafe(p);
            DarkGodNet.broadcast(p);
            p.setHealth(p.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && DarkGodData.isDarkGod(p)) {
            DarkPowers.applyAttributes(p);
            DarkGodNet.broadcast(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            TransformationManager.cancel(p);
        }
    }
}
