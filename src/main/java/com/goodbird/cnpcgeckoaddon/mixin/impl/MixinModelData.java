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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelData.class)
public abstract class MixinModelData extends ModelDataShared {

    @Inject(method = "setEntity", at = @At("HEAD"), cancellable = true, remap = false)
    public void onSetEntity(ResourceLocation resourceLocation, CallbackInfo ci) {
        // 同名重设（GUI 保存配置时 entityName 没变）会调用 clearEntity() 清掉模型实体，
        // 导致攻击状态（currentAttackAnim/attackAnimStartTick 等）丢失、守卫失效、
        // 帧伤害计时被重置——「10 秒延时伤害不生效」的根因。同名时直接跳过重建。
        if (this.entityName != null && this.entityName.equals(resourceLocation)) {
            ci.cancel();
        }
    }

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
