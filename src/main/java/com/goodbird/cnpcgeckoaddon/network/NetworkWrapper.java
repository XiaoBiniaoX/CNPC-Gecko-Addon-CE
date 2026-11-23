package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import noppes.npcs.CustomNpcs;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Function;

@EventBusSubscriber(bus=EventBusSubscriber.Bus.MOD, modid=CNPCGeckoAddon.MODID)
public class NetworkWrapper {


    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registerServerToClient(registrar, PacketSyncAnimation.class,PacketSyncAnimation::encode,PacketSyncAnimation::decode,PacketSyncAnimation::handle);
        registerServerToClient(registrar, PacketSyncTileAnimation.class,PacketSyncTileAnimation::encode,PacketSyncTileAnimation::decode,PacketSyncTileAnimation::handle);
        registerPacket(registrar, PacketInstructionKeyframe.class,PacketInstructionKeyframe::encode,PacketInstructionKeyframe::decode,PacketInstructionKeyframe::handle);
        registerServerToClient(registrar, PacketSyncTexture.class,PacketSyncTexture::encode,PacketSyncTexture::decode,PacketSyncTexture::handle);
    }

    public static <MSG extends CustomPacketPayload> void registerServerToClient(PayloadRegistrar registrar, Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, IPayloadHandler<MSG> handle) {
        // 这些包需要已进入世界的玩家，默认 MAIN 线程由 NeoForge 调度。
        registrar.playToClient(new CustomPacketPayload.Type<>(ResourceLocation.parse("cnpcgeckoaddon:"+messageType.getSimpleName().toLowerCase())), CustomPacketPayload.codec(encoder::accept, decoder::apply), handle);
    }

    public static <MSG extends CustomPacketPayload> void registerPacket(PayloadRegistrar registrar , Class<MSG> messageType, BiConsumer<MSG, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, MSG> decoder, TriConsumer<MSG, MinecraftServer, ServerPlayer> handle) {
        registrar.playToServer(
                new CustomPacketPayload.Type<>(ResourceLocation.parse("cnpcgeckoaddon:"+messageType.getSimpleName().toLowerCase())),
                CustomPacketPayload.codec(encoder::accept, decoder::apply),
                (packet, context) -> handle.accept(packet, context.player().getServer(), (ServerPlayer) context.player())
        );
    }

    public static <MSG extends CustomPacketPayload> void send(ServerPlayer player, MSG msg) {
        PacketDistributor.sendToPlayer(player, msg);
    }


    public static <MSG extends CustomPacketPayload> void sendAll(MSG msg) {
        for(ServerPlayer player: CustomNpcs.Server.getPlayerList().getPlayers()) {
            send(player, msg);
        }
    }

    public static <MSG extends CustomPacketPayload> void sendToServer(MSG msg) {
        PacketDistributor.sendToServer(msg);
    }
}
