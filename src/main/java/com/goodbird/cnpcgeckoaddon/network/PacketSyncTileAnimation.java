package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.tile.TileEntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import noppes.npcs.blocks.tiles.TileScripted;

import java.util.function.Supplier;

public class PacketSyncTileAnimation {
    private BlockPos pos;
    private String animName;

    public PacketSyncTileAnimation(BlockPos pos, String animName) {
        this.pos = pos;
        this.animName = animName;
    }

    public PacketSyncTileAnimation(){

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(animName != null);
        if (animName != null) {
            buf.writeUtf(animName);
        }
    }

    public static PacketSyncTileAnimation decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean hasAnim = buf.readBoolean();
        String animName = hasAnim ? buf.readUtf() : null;
        return new PacketSyncTileAnimation(pos, animName);
    }

    public static void handle(PacketSyncTileAnimation packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            // 不能直接用 Minecraft.getInstance().level：字段类型 ClientLevel 带 @OnlyIn(CLIENT)，服务器加载会崩。
            // player 在加入/离开世界窗口时为 null，需先判空。
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return;
            Level level = player.level();
            if (level == null) return;
            BlockEntity entity = level.getBlockEntity(packet.pos);
            if (!(entity instanceof TileScripted tile)) return;
            if (tile.renderTile == null) {
                tile.renderTile = new TileEntityCustomModel(tile);
            }
            if (!(tile.renderTile instanceof TileEntityCustomModel geckoTile)) return;
            geckoTile.manualAnimName = packet.animName;
        });
        context.setPacketHandled(true);
    }
}
