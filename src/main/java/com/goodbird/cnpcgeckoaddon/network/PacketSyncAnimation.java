package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import noppes.npcs.entity.EntityCustomNpc;

public class PacketSyncAnimation implements CustomPacketPayload {
    public static final byte TYPE_DEATH = 0;
    public static final byte TYPE_ATTACK = 1;
    public static final byte TYPE_HURT = 2;
    public static final byte TYPE_MANUAL = 3;

    private int id;
    private String animName;
    private boolean instant;
    private byte type;

    public PacketSyncAnimation(int entityId, String animName, boolean instant, byte type) {
        this.id = entityId;
        this.animName = animName;
        this.instant = instant;
        this.type = type;
    }

    public PacketSyncAnimation(){

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUtf(animName);
        buf.writeBoolean(instant);
        buf.writeByte(type);
    }

    public static PacketSyncAnimation decode(FriendlyByteBuf buf) {
        PacketSyncAnimation p = new PacketSyncAnimation(buf.readInt(), buf.readUtf(), buf.readBoolean(), buf.readByte());
        return p;
    }

    public static void handle(PacketSyncAnimation packet) {
        Entity entity = Minecraft.getInstance().player.getCommandSenderWorld().getEntity(packet.id);
        if(!(entity instanceof EntityCustomNpc npc)) return;
        if(npc.modelData==null || !(npc.modelData.getEntity(npc) instanceof EntityCustomModel)) return;
        EntityCustomModel em = (EntityCustomModel) npc.modelData.getEntity(npc);
        switch (packet.type) {
            case TYPE_DEATH:
                if (!em.deathAnimationPlaying) {
                    em.playDeathAnimation(packet.animName);
                }
                break;
            case TYPE_ATTACK:
                em.currentAttackAnim = packet.animName;
                em.manualAnimName = packet.animName;
                em.manualAnimInstant = false;
                break;
            case TYPE_HURT:
                em.manualAnimName = packet.animName;
                em.manualAnimInstant = true;
                break;
            case TYPE_MANUAL:
            default:
                em.manualAnimName = packet.animName;
                em.manualAnimInstant = packet.instant;
                break;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return new Type<>(ResourceLocation.parse("cnpcgeckoaddon:"+getClass().getSimpleName().toLowerCase()));
    }
}
