package com.goodbird.cnpcgeckoaddon.mixin.impl;

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
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;

@Mixin(EntityUtil.class)
public class MixinEntityUtil {

    @Inject(method = "Copy", at = @At("TAIL"), remap = false)
    private static void copy(LivingEntity copied, LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof EntityCustomModel && copied instanceof EntityNPCInterface) {
            EntityCustomModel modelEntity = (EntityCustomModel) entity;
            EntityNPCInterface npc = (EntityNPCInterface) copied;
            npc.noCulling = true;
            IDataDisplay display = (IDataDisplay) npc.display;
            modelEntity.textureResLoc = NpcTextureUtils.getNpcTexture((EntityNPCInterface) copied);
            modelEntity.modelResLoc = new ResourceLocation(display.getCustomModelData().getModel());
            modelEntity.animResLoc = new ResourceLocation(display.getCustomModelData().getAnimFile());
            modelEntity.idleAnim = display.getCustomModelData().getIdleAnim();
            modelEntity.walkAnim = display.getCustomModelData().getWalkAnim();
            modelEntity.attackAnim = display.getCustomModelData().getAttackAnim();
            modelEntity.hurtAnim = display.getCustomModelData().getHurtAnim();
            modelEntity.owner = npc;
            System.arraycopy(display.getCustomModelData().getAttackAnimNames(), 0, modelEntity.attackAnimNames, 0, modelEntity.MAX_ATTACKS);
            System.arraycopy(display.getCustomModelData().getAttackWeights(), 0, modelEntity.attackWeights, 0, modelEntity.MAX_ATTACKS);
            System.arraycopy(display.getCustomModelData().getAttackFrames(), 0, modelEntity.attackFrames, 0, modelEntity.MAX_ATTACKS);
            modelEntity.attackCount = display.getCustomModelData().getAttackCount();
            System.arraycopy(display.getCustomModelData().getHurtAnimNames(), 0, modelEntity.hurtAnimNames, 0, modelEntity.MAX_HURTS);
            System.arraycopy(display.getCustomModelData().getHurtWeights(), 0, modelEntity.hurtWeights, 0, modelEntity.MAX_HURTS);
            modelEntity.hurtAnimCount = display.getCustomModelData().getHurtAnimCount();
            System.arraycopy(display.getCustomModelData().getDeathAnimNames(), 0, modelEntity.deathAnimNames, 0, modelEntity.MAX_DEATHS);
            System.arraycopy(display.getCustomModelData().getDeathWeights(), 0, modelEntity.deathWeights, 0, modelEntity.MAX_DEATHS);
            System.arraycopy(display.getCustomModelData().getDeathHealthThresholds(), 0, modelEntity.deathHealthThresholds, 0, modelEntity.MAX_DEATHS);
            System.arraycopy(display.getCustomModelData().getDeathAnimDurations(), 0, modelEntity.deathAnimDurations, 0, modelEntity.MAX_DEATHS);
            modelEntity.deathAnimCount = display.getCustomModelData().getDeathAnimCount();
            modelEntity.size = npc.display.getSize();
            if(npc.inventory.getLeftHand()!=null) {
                modelEntity.leftHeldItem = npc.inventory.getLeftHand().getMCItemStack();
            }
            modelEntity.headBoneName = display.getCustomModelData().getHeadBoneName();
            AnimatableManager animationData = modelEntity.getAnimatableInstanceCache().getManagerForId(modelEntity.getUUID().hashCode());
            for(Object obj : animationData.getAnimationControllers().values()){
                AnimationController controller = (AnimationController) obj;
                controller.transitionLength(display.getCustomModelData().getTransitionLengthTicks());
            }
            if(display.getCustomModelData().getHeight()!=modelEntity.getBbHeight() || display.getCustomModelData().getWidth() != modelEntity.getBbWidth()){
                modelEntity.setSize(display.getCustomModelData().getWidth(), display.getCustomModelData().getHeight());
                npc.refreshDimensions();
            }
        }
    }
}
