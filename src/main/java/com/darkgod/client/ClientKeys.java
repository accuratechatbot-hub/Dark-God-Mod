package com.darkgod.client;

import com.darkgod.DarkAbilities;
import com.darkgod.DarkGodMod;
import com.darkgod.DarkGodNet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

public final class ClientKeys {
    private static final String CATEGORY = "key.categories.darkgod";

    public static final KeyMapping BEAM = new KeyMapping("key.darkgod.beam", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping GRAVITY = new KeyMapping("key.darkgod.gravity", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping BOOM_TUBE = new KeyMapping("key.darkgod.boomtube", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping SNAP = new KeyMapping("key.darkgod.snap", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping AURA = new KeyMapping("key.darkgod.aura", GLFW.GLFW_KEY_B, CATEGORY);

    private ClientKeys() {}

    @Mod.EventBusSubscriber(modid = DarkGodMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
            e.register(BEAM);
            e.register(GRAVITY);
            e.register(BOOM_TUBE);
            e.register(SNAP);
            e.register(AURA);
        }
    }

    @Mod.EventBusSubscriber(modid = DarkGodMod.MODID, value = Dist.CLIENT)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            fire(BEAM, DarkAbilities.BEAM);
            fire(GRAVITY, DarkAbilities.GRAVITY);
            fire(BOOM_TUBE, DarkAbilities.BOOM_TUBE);
            fire(SNAP, DarkAbilities.SNAP);
            fire(AURA, DarkAbilities.AURA);
        }

        private static void fire(KeyMapping key, int id) {
            while (key.consumeClick()) {
                DarkGodNet.CHANNEL.sendToServer(new DarkGodNet.AbilityPacket(id));
            }
        }

        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent e) {
            if (e.getItemStack().is(Items.COMMAND_BLOCK) || e.getItemStack().is(Items.CHAIN_COMMAND_BLOCK)
                    || e.getItemStack().is(Items.REPEATING_COMMAND_BLOCK)) {
                e.getToolTip().add(Component.translatable("tooltip.darkgod.eat").withStyle(net.minecraft.ChatFormatting.DARK_RED));
                e.getToolTip().add(Component.translatable("tooltip.darkgod.eat2").withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
            }
        }
    }
}
