package com.darkgod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class DarkGodNet {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DarkGodMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private DarkGodNet() {}

    public static void register() {
        CHANNEL.registerMessage(0, AbilityPacket.class, AbilityPacket::encode, AbilityPacket::decode,
                AbilityPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    /** Client -> server: "use ability N". */
    public static final class AbilityPacket {
        private final int id;

        public AbilityPacket(int id) { this.id = id; }

        public static void encode(AbilityPacket msg, FriendlyByteBuf buf) { buf.writeVarInt(msg.id); }

        public static AbilityPacket decode(FriendlyByteBuf buf) { return new AbilityPacket(buf.readVarInt()); }

        public static void handle(AbilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> {
                ServerPlayer sender = c.getSender();
                if (sender != null) DarkAbilities.activate(sender, msg.id);
            });
            c.setPacketHandled(true);
        }
    }
}
