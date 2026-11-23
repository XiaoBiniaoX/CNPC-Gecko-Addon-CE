package com.goodbird.cnpcgeckoaddon.network;

import com.goodbird.cnpcgeckoaddon.tile.TileEntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
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
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet)));
        context.setPacketHandled(true);
    }

    /** 渲染方块实体的创建与动画赋值只在客户端进行。 */
    @OnlyIn(Dist.CLIENT)
    private static class ClientHandler {
        private static void handle(PacketSyncTileAnimation packet) {
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
        }
    }
}
