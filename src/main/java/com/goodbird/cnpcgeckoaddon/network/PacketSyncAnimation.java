package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import noppes.npcs.entity.EntityCustomNpc;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.function.Supplier;

public class PacketSyncAnimation {
    private int id;
    private String animName;
    private boolean instant = false;

    public PacketSyncAnimation(int entityId, String animName, boolean instant) {
        this.id = entityId;
        this.animName = animName;
        this.instant = instant;
    }

    public PacketSyncAnimation(){

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeBoolean(instant);
        buf.writeBoolean(animName != null);
        if (animName != null) {
            buf.writeUtf(animName);
        }
    }

    public static PacketSyncAnimation decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        boolean instant = buf.readBoolean();
        boolean hasAnim = buf.readBoolean();
        String animName = hasAnim ? buf.readUtf() : null;
        return new PacketSyncAnimation(id, animName, instant);
    }

    public static void handle(PacketSyncAnimation packet, Supplier<NetworkEvent.Context> ctx) {
        Entity entity = Minecraft.getInstance().player.getCommandSenderWorld().getEntity(packet.id);
        if(!(entity instanceof EntityCustomNpc)) return;
        EntityCustomNpc npc = (EntityCustomNpc) entity;
        if(npc.modelData==null || !(npc.modelData.getEntity(npc) instanceof EntityCustomModel)) return;
        EntityCustomModel entityCustomModel = (EntityCustomModel) npc.modelData.getEntity(npc);
        entityCustomModel.manualAnimName = packet.animName;
        entityCustomModel.manualAnimInstant = packet.instant;
    }
}
