package com.goodbird.cnpcgeckoaddon.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import noppes.npcs.entity.EntityCustomNpc;

public class PacketSyncTexture implements CustomPacketPayload {
    private int id;
    private String texture;

    public PacketSyncTexture(int entityId, String texture) {
        this.id = entityId;
        this.texture = texture;
    }

    public PacketSyncTexture(){

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUtf(texture);
    }

    public static PacketSyncTexture decode(FriendlyByteBuf buf) {
        return new PacketSyncTexture(buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketSyncTexture packet) {
        Entity entity = Minecraft.getInstance().player.getCommandSenderWorld().getEntity(packet.id);
        if(!(entity instanceof EntityCustomNpc npc)) return;
        npc.display.setSkinTexture(packet.texture);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return new Type<>(ResourceLocation.parse("cnpcgeckoaddon:"+getClass().getSimpleName().toLowerCase()));
    }
}