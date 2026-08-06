package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.ModelData;
import noppes.npcs.ModelDataShared;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelData.class)
public abstract class MixinModelData extends ModelDataShared {

    @Inject(method = "getEntity", at = @At(value = "FIELD", target = "Lnoppes/npcs/ModelData;entity:Lnet/minecraft/world/entity/LivingEntity;"), remap = false)
    public void onEntityFieldAssigned(EntityNPCInterface npc, CallbackInfoReturnable<LivingEntity> cir){
        if(!(entity instanceof EntityCustomModel modelEntity)) return;
        // 防御：配置为空时避免 new ResourceLocation(null/无效) 抛异常
        // （异常会冒泡进 getEntity 的 catch，导致模型实体配置加载失败）
        String model = ((IDataDisplay)npc.display).getCustomModelData().getModel();
        if (model != null && !model.isEmpty()) {
            modelEntity.modelResLoc = new ResourceLocation(model);
        }
        String animFile = ((IDataDisplay)npc.display).getCustomModelData().getAnimFile();
        if (animFile != null && !animFile.isEmpty()) {
            modelEntity.animResLoc = new ResourceLocation(animFile);
        }
        modelEntity.idleAnim = ((IDataDisplay)npc.display).getCustomModelData().getIdleAnim();
    }
}
