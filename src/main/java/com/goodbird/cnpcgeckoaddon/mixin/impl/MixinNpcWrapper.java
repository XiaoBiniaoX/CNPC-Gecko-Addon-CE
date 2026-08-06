package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import net.minecraftforge.server.ServerLifecycleHooks;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.wrapper.EntityLivingWrapper;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.entity.EntityNPCInterface;
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
        // 该方法设计为服务端脚本调用；客户端（单机内部/客户端脚本）没有服务端实例，
        // 直接发包会 NPE，这里判空保护。
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        NetworkWrapper.sendToAll(new PacketSyncAnimation(entity.getId(), animName, false));
    }

    @Unique
    public void syncInstantAnimationsFor(IPlayer player, String animName) {
        NetworkWrapper.sendToPlayer(new PacketSyncAnimation(entity.getId(), animName, true), player.getMCEntity());
    }
    @Unique
    public void syncInstantAnimationsForAll(String animName) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        NetworkWrapper.sendToAll(new PacketSyncAnimation(entity.getId(), animName, true));
    }

}
