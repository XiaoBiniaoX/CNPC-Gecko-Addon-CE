package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;

public class NetworkWrapper {
    private static final String PROTOCOL = "10";
    public static final SimpleChannel wrapper = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CNPCGeckoAddon.MODID,"chan"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );


    public static void init() {
        wrapper.registerMessage(0,PacketSyncAnimation.class,PacketSyncAnimation::encode,PacketSyncAnimation::decode,PacketSyncAnimation::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        wrapper.registerMessage(1,PacketSyncTileAnimation.class,PacketSyncTileAnimation::encode,PacketSyncTileAnimation::decode,PacketSyncTileAnimation::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        wrapper.registerMessage(2,PacketSyncTexture.class,PacketSyncTexture::encode,PacketSyncTexture::decode,PacketSyncTexture::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        wrapper.registerMessage(3,PacketInstructionKeyframe.class,PacketInstructionKeyframe::encode,PacketInstructionKeyframe::decode,PacketInstructionKeyframe::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToServer(Object message) {
        wrapper.sendToServer(message);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        wrapper.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }


    public static void sendToAll(Object message) {
        for(ServerPlayer player: ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
            sendToPlayer(message,player);
    }
}
