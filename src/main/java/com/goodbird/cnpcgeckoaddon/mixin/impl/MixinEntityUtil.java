package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.utils.NpcTextureUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;

@Mixin(EntityUtil.class)
public class MixinEntityUtil {

    @Inject(method = "Copy", at = @At("TAIL"), remap = false)
    private static void copy(LivingEntity copied, LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof EntityCustomModel && copied instanceof EntityNPCInterface) {
            EntityCustomModel modelEntity = (EntityCustomModel) entity;
            EntityNPCInterface npc = (EntityNPCInterface) copied;
            npc.noCulling = true;
            IDataDisplay display = (IDataDisplay) npc.display;
            CustomModelData data = display.getCustomModelData();
            modelEntity.textureResLoc = NpcTextureUtils.getNpcTexture(npc);
            modelEntity.modelResLoc = ResourceLocation.parse(data.getModel());
            modelEntity.animResLoc = ResourceLocation.parse(data.getAnimFile());
            modelEntity.idleAnim = data.getIdleAnim();
            modelEntity.walkAnim = data.getWalkAnim();
            modelEntity.size = npc.display.getSize();
            if(data.isHurtTintEnabled()){
                modelEntity.hurtTime = npc.hurtTime;
                modelEntity.deathTime = npc.deathTime;
            } else {
                modelEntity.hurtTime = 0;
                modelEntity.deathTime = 0;
            }
            if(npc.inventory.getLeftHand()!=null) {
                modelEntity.leftHeldItem = npc.inventory.getLeftHand().getMCItemStack();
            }
            modelEntity.headBoneName = data.getHeadBoneName();

            modelEntity.attackCount = data.getAttackCount();
            System.arraycopy(data.getAttackAnimNames(), 0, modelEntity.attackAnimNames, 0, CustomModelData.MAX_ATTACKS);
            System.arraycopy(data.getAttackWeights(), 0, modelEntity.attackWeights, 0, CustomModelData.MAX_ATTACKS);
            System.arraycopy(data.getAttackFrames(), 0, modelEntity.attackFrames, 0, CustomModelData.MAX_ATTACKS);

            modelEntity.hurtAnimCount = data.getHurtAnimCount();
            System.arraycopy(data.getHurtAnimNames(), 0, modelEntity.hurtAnimNames, 0, CustomModelData.MAX_HURTS);
            System.arraycopy(data.getHurtWeights(), 0, modelEntity.hurtWeights, 0, CustomModelData.MAX_HURTS);

            modelEntity.deathAnimCount = data.getDeathAnimCount();
            System.arraycopy(data.getDeathAnimNames(), 0, modelEntity.deathAnimNames, 0, CustomModelData.MAX_DEATHS);
            System.arraycopy(data.getDeathWeights(), 0, modelEntity.deathWeights, 0, CustomModelData.MAX_DEATHS);
            System.arraycopy(data.getDeathHealthThresholds(), 0, modelEntity.deathHealthThresholds, 0, CustomModelData.MAX_DEATHS);
            System.arraycopy(data.getDeathAnimDurations(), 0, modelEntity.deathAnimDurations, 0, CustomModelData.MAX_DEATHS);

            AnimatableManager animationData = modelEntity.getAnimatableInstanceCache().getManagerForId(modelEntity.getUUID().hashCode());
            for(Object obj : animationData.getAnimationControllers().values()){
                AnimationController controller = (AnimationController) obj;
                controller.transitionLength(data.getTransitionLengthTicks());
            }
            if(data.getHeight()!=modelEntity.getBbHeight() || data.getWidth() != modelEntity.getBbWidth()){
                modelEntity.setSize(data.getWidth(), data.getHeight());
                npc.refreshDimensions();
            }
        }
    }
}