package com.goodbird.cnpcgeckoaddon.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
        context.enqueueWork(() -> {
            // 不能直接用 Minecraft.getInstance().level：字段类型 ClientLevel 带 @OnlyIn(CLIENT)，服务器加载会崩。
            // player 在加入/离开世界窗口时为 null，需先判空。
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            Level level = player.level();
            if (level == null) return;
            Entity entity = level.getEntity(packet.id);
            if (!(entity instanceof EntityCustomNpc npc)) return;
            npc.display.setSkinTexture(packet.texture);
        });
        context.setPacketHandled(true);
    }
}

