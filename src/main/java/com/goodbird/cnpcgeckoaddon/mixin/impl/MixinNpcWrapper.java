package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.wrapper.EntityLivingWrapper;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(NPCWrapper.class)
public abstract class MixinNpcWrapper extends EntityLivingWrapper<EntityNPCInterface> {

    public MixinNpcWrapper(EntityNPCInterface entity) {
        super(entity);
    }

    @Unique
    public CustomModelData getModelData(){
        return ((IDataDisplay)getMCEntity().display).getCustomModelData();
    }

    @Unique
    public void setGeckoModel(String model) {
        getModelData().setModel(model);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoTexture(String texture) {
        getMCEntity().display.setSkinTexture(texture);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoAnimationFile(String animation) {
        getModelData().setAnimFile(animation);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoIdleAnimation(String animation) {
        getModelData().setIdleAnim(animation);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoWalkAnimation(String animation) {
        getModelData().setWalkAnim(animation);
        getMCEntity().updateClient();
    }

    @Unique
    public void syncAnimationsFor(IPlayer player, String animName) {
        NetworkWrapper.sendToPlayer(new PacketSyncAnimation(entity.getId(), animName, false), player.getMCEntity());
    }
    @Unique
    public void syncAnimationsForAll(String animName) {
        NetworkWrapper.sendToAll(new PacketSyncAnimation(entity.getId(), animName, false));
    }

    @Unique
    public void syncInstantAnimationsFor(IPlayer player, String animName) {
        NetworkWrapper.sendToPlayer(new PacketSyncAnimation(entity.getId(), animName, true), player.getMCEntity());
    }
    @Unique
    public void syncInstantAnimationsForAll(String animName) {
        NetworkWrapper.sendToAll(new PacketSyncAnimation(entity.getId(), animName, true));
    }

}
