package com.goodbird.cnpcgeckoaddon.client;

import com.goodbird.cnpcgeckoaddon.CommonProxy;
import com.goodbird.cnpcgeckoaddon.client.renderer.RenderCustomModel;
import com.goodbird.cnpcgeckoaddon.client.renderer.RenderTileCustomModel;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.tile.TileEntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import software.bernie.geckolib3.GeckoLib;

public class ClientProxy  extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent ev) {
        super.preInit(ev);
        RenderingRegistry.registerEntityRenderingHandler(EntityCustomModel.class, RenderCustomModel::new);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCustomModel.class, new RenderTileCustomModel());
    }

    @Override
    public void init(FMLInitializationEvent ev) {
        super.init(ev);
        GeckoLib.initialize();
    }

    public World getWorldById(int id){
        return Minecraft.getMinecraft().world;
    }

    @Override
    public void handleAnimation(com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            net.minecraft.entity.Entity entity = mc.world.getEntityByID(message.entityId);
            if (!(entity instanceof noppes.npcs.entity.EntityCustomNpc)) return;
            noppes.npcs.entity.EntityCustomNpc npc = (noppes.npcs.entity.EntityCustomNpc) entity;
            if (!(npc.modelData.getEntity(npc) instanceof EntityCustomModel)) return;
            EntityCustomModel model = (EntityCustomModel) npc.modelData.getEntity(npc);
            if (message.asHurt) model.hurtAnim = message.builder;
            else model.manualAnim = message.builder;
        });
    }

    @Override
    public void handleTileAnimation(com.goodbird.cnpcgeckoaddon.network.CPacketSyncTileManualAnim message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.world == null) return;
            net.minecraft.tileentity.TileEntity entity = mc.world.getTileEntity(message.pos);
            if (!(entity instanceof noppes.npcs.blocks.tiles.TileScripted)) return;
            noppes.npcs.blocks.tiles.TileScripted tile = (noppes.npcs.blocks.tiles.TileScripted) entity;
            if (tile.renderTile == null) tile.renderTile = new TileEntityCustomModel();
            if (tile.renderTile instanceof TileEntityCustomModel) ((TileEntityCustomModel) tile.renderTile).manualAnim = message.builder;
        });
    }
}
