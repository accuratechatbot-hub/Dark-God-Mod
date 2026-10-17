package com.darkgod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.UUID;
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
        CHANNEL.registerMessage(1, GodSyncPacket.class, GodSyncPacket::encode, GodSyncPacket::decode,
                GodSyncPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Tell every client whether this player is currently a Dark God (drives the skin). */
    public static void broadcast(ServerPlayer p) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new GodSyncPacket(p.getUUID(), DarkGodData.isDarkGod(p)));
    }

    /** When someone joins, tell them about every Dark God already online. */
    public static void syncTo(ServerPlayer joiner) {
        for (ServerPlayer o : joiner.server.getPlayerList().getPlayers()) {
            if (DarkGodData.isDarkGod(o)) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> joiner), new GodSyncPacket(o.getUUID(), true));
            }
        }
    }

    /** Server -> client: player X is / is not a Dark God. */
    public static final class GodSyncPacket {
        private final UUID id;
        private final boolean god;

        public GodSyncPacket(UUID id, boolean god) { this.id = id; this.god = god; }

        public static void encode(GodSyncPacket m, FriendlyByteBuf buf) { buf.writeUUID(m.id); buf.writeBoolean(m.god); }

        public static GodSyncPacket decode(FriendlyByteBuf buf) { return new GodSyncPacket(buf.readUUID(), buf.readBoolean()); }

        public static void handle(GodSyncPacket m, Supplier<NetworkEvent.Context> ctx) {
            NetworkEvent.Context c = ctx.get();
            c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.darkgod.client.ClientGods.set(m.id, m.god)));
            c.setPacketHandled(true);
        }
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
