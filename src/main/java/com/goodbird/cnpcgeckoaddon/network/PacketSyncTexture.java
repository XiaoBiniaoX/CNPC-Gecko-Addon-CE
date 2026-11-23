package com.goodbird.cnpcgeckoaddon.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import noppes.npcs.entity.EntityCustomNpc;

import java.util.function.Supplier;

public class PacketSyncTexture {
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

    public static void handle(PacketSyncTexture packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet)));
        context.setPacketHandled(true);
    }

    /** 玩家与世界查询只由客户端执行。 */
    @OnlyIn(Dist.CLIENT)
    private static class ClientHandler {
        private static void handle(PacketSyncTexture packet) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            Level level = player.level();
            if (level == null) return;
            Entity entity = level.getEntity(packet.id);
            if (!(entity instanceof EntityCustomNpc npc)) return;
            npc.display.setSkinTexture(packet.texture);
        }
    }
}
