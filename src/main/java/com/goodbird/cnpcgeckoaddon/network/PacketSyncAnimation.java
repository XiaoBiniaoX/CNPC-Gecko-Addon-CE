package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // Never touch Minecraft.player here: it is null while joining/leaving a world,
            // and this packet can arrive in that window (crash on old saves).
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            Entity entity = level.getEntity(packet.id);
            if (!(entity instanceof EntityCustomNpc npc)) return;
            if (npc.modelData == null) return;
            if (!(npc.modelData.getEntity(npc) instanceof EntityCustomModel entityCustomModel)) return;
            entityCustomModel.manualAnimName = packet.animName;
            entityCustomModel.manualAnimInstant = packet.instant;
        });
        context.setPacketHandled(true);
    }
}
