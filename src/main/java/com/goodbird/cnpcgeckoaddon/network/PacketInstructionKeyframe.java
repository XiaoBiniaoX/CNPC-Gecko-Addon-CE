package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.api.event.AnimationKeyframeEvent;
import com.goodbird.cnpcgeckoaddon.constants.GAEnumScriptType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.entity.EntityCustomNpc;

public class PacketInstructionKeyframe implements CustomPacketPayload {
    private int id;
    private String instruction;

    public PacketInstructionKeyframe(int entityId, String instruction) {
        this.id = entityId;
        this.instruction = instruction;
    }

    public PacketInstructionKeyframe(){

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(id);
        buf.writeUtf(instruction);
    }

    public static PacketInstructionKeyframe decode(FriendlyByteBuf buf) {
        return new PacketInstructionKeyframe(buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketInstructionKeyframe packet, MinecraftServer server, ServerPlayer player) {
        Entity entity = player.serverLevel().getEntity(packet.id);
        if(!(entity instanceof EntityCustomNpc npc)) return;

        if("attack;".equals(packet.instruction)){
            if(npc.getTarget()!=null) {
                npc.doHurtTarget(npc.getTarget());
            }
        }

        AnimationKeyframeEvent event = new AnimationKeyframeEvent((ICustomNpc) NpcAPI.Instance().getIEntity(npc), player, packet.instruction);
        GAEnumScriptType.runAnimationInstruction(npc.script, event);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return new Type<>(ResourceLocation.parse("cnpcgeckoaddon:"+getClass().getSimpleName().toLowerCase()));
    }
}
