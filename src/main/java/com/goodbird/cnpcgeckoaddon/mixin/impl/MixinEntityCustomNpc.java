package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityCustomNpc.class)
public class MixinEntityCustomNpc extends EntityNPCInterface {

    @Shadow(remap = false)
    public ModelData modelData;

    public MixinEntityCustomNpc(EntityType<? extends PathfinderMob> type, Level world) {
        super(type, world);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        IDataDisplay display = (IDataDisplay) this.display;
        Entity entity = this.modelData.getEntity(this);
        if (!(entity instanceof EntityCustomModel)) return;
        EntityCustomModel modelEntity = (EntityCustomModel) entity;
        if (display.getCustomModelData().getHeight() != modelEntity.getBbHeight() || display.getCustomModelData().getWidth() != modelEntity.getBbWidth()) {
            modelEntity.setSize(display.getCustomModelData().getWidth(), display.getCustomModelData().getHeight());
            this.refreshDimensions();
        }

        modelEntity.deathTime = this.deathTime;
        if (display.getCustomModelData().isHurtTintEnabled()) {
            modelEntity.hurtTime = this.hurtTime;
        } else {
            modelEntity.hurtTime = 0;
        }

        if (this.hurtTime > 0 && !modelEntity.hurtAnimationPlaying) {
            if (modelEntity.currentAttackAnim == null) {
                String hurtAnim = modelEntity.pickWeightedHurtAnim();
                if (hurtAnim == null) hurtAnim = display.getCustomModelData().getHurtAnim();
                if (hurtAnim != null && !hurtAnim.isEmpty()) {
                    modelEntity.playHurtAnimation(hurtAnim);
                    modelEntity.hurtAnimationPlaying = true;
                }
            }
        }
        if (this.hurtTime <= 0) {
            modelEntity.hurtAnimationPlaying = false;
        }

        if (this.deathTime == 1 && !modelEntity.deathAnimationPlaying) {
            String deathAnim = modelEntity.pickWeightedDeathAnim();
            if (deathAnim == null || deathAnim.isEmpty()) {
                deathAnim = display.getCustomModelData().getHurtAnim();
            }
            if (deathAnim != null && !deathAnim.isEmpty()) {
                modelEntity.playDeathAnimation(deathAnim);
            }
        }
        if (this.deathTime <= 0) {
            modelEntity.deathAnimationPlaying = false;
        }

        if (!level().isClientSide) {
            if (modelEntity.currentAttackAnim != null && modelEntity.attackingTarget != null && !modelEntity.attackDamageDealt && modelEntity.currentAttackFrame > 0) {
                int elapsedTicks = tickCount - modelEntity.attackAnimStartTick;
                int targetTicks = (int)(modelEntity.currentAttackFrame * 20.0f);
                if (elapsedTicks >= targetTicks && targetTicks > 0) {
                    modelEntity.frameAttackInProgress = true;
                    this.doHurtTarget(modelEntity.attackingTarget);
                    modelEntity.frameAttackInProgress = false;
                    modelEntity.attackDamageDealt = true;
                }
            }
            int animTimeout = 200;
            if (modelEntity.currentAttackAnim != null && (tickCount - modelEntity.attackAnimStartTick > animTimeout)) {
                if (!modelEntity.attackDamageDealt && modelEntity.attackingTarget != null && modelEntity.attackingTarget.isAlive()) {
                    this.doHurtTarget(modelEntity.attackingTarget);
                }
                modelEntity.resetAttackState();
            }
        }
    }
}
