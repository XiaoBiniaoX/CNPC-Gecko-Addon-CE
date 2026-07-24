package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.utils.NpcTextureUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private int lastHurtTime = 0;
    @Unique
    private float lastHealth = 0;
    @Unique
    private boolean hurtAnimPlayedForThisHit = false;
    @Unique
    private int lastDeathTime = 0;
    @Unique
    private String currentHurtSound = null;

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        IDataDisplay display = (IDataDisplay) this.display;
        Entity entity = this.modelData.getEntity(this);
        if (!(entity instanceof EntityCustomModel)) return;
        EntityCustomModel modelEntity = (EntityCustomModel) entity;
        modelEntity.owner = this;
        CustomModelData data = display.getCustomModelData();

        modelEntity.modelResLoc = ResourceLocation.parse(data.getModel());
        modelEntity.animResLoc = ResourceLocation.parse(data.getAnimFile());
        modelEntity.textureResLoc = NpcTextureUtils.getNpcTexture(this);
        modelEntity.idleAnim = data.getIdleAnim();
        modelEntity.walkAnim = data.getWalkAnim();
        modelEntity.headBoneName = data.getHeadBoneName();
        modelEntity.size = this.display.getSize();

        modelEntity.attackCount = data.getAttackCount();
        System.arraycopy(data.getAttackAnimNames(), 0, modelEntity.attackAnimNames, 0, CustomModelData.MAX_ATTACKS);
        System.arraycopy(data.getAttackWeights(), 0, modelEntity.attackWeights, 0, CustomModelData.MAX_ATTACKS);
        System.arraycopy(data.getAttackFrames(), 0, modelEntity.attackFrames, 0, CustomModelData.MAX_ATTACKS);
        System.arraycopy(data.getAttackSoundNames(), 0, modelEntity.attackSoundNames, 0, CustomModelData.MAX_ATTACKS);

        modelEntity.hurtAnimCount = data.getHurtAnimCount();
        System.arraycopy(data.getHurtAnimNames(), 0, modelEntity.hurtAnimNames, 0, CustomModelData.MAX_HURTS);
        System.arraycopy(data.getHurtWeights(), 0, modelEntity.hurtWeights, 0, CustomModelData.MAX_HURTS);
        System.arraycopy(data.getHurtSoundNames(), 0, modelEntity.hurtSoundNames, 0, CustomModelData.MAX_HURTS);

        modelEntity.deathAnimCount = data.getDeathAnimCount();
        System.arraycopy(data.getDeathAnimNames(), 0, modelEntity.deathAnimNames, 0, CustomModelData.MAX_DEATHS);
        System.arraycopy(data.getDeathWeights(), 0, modelEntity.deathWeights, 0, CustomModelData.MAX_DEATHS);
        System.arraycopy(data.getDeathHealthThresholds(), 0, modelEntity.deathHealthThresholds, 0, CustomModelData.MAX_DEATHS);
        System.arraycopy(data.getDeathAnimDurations(), 0, modelEntity.deathAnimDurations, 0, CustomModelData.MAX_DEATHS);

        if (data.getHeight() != modelEntity.getBbHeight() || data.getWidth() != modelEntity.getBbWidth()) {
            modelEntity.setSize(data.getWidth(), data.getHeight());
            this.refreshDimensions();
        }

        // Hurt animation
        if (this.hurtTime > 0 && lastHurtTime == 0 && !hurtAnimPlayedForThisHit) {
            hurtAnimPlayedForThisHit = true;
            if (modelEntity.currentAttackAnim == null) {
                String hurt = modelEntity.pickWeightedHurtAnim();
                if (hurt != null) {
                    currentHurtSound = null;
                    for (int i = 0; i < modelEntity.hurtAnimCount; i++) {
                        if (hurt.equals(modelEntity.hurtAnimNames[i])) {
                            currentHurtSound = modelEntity.hurtSoundNames[i];
                            break;
                        }
                    }
                    modelEntity.playHurtAnimation(hurt);
                    playSound(currentHurtSound);
                }
            }
        }
        if (this.hurtTime <= 0) {
            hurtAnimPlayedForThisHit = false;
        }
        lastHurtTime = this.hurtTime;

        // Death animation (both sides, client has correct data from GUI)
        if (this.deathTime > 0 && lastDeathTime == 0 && !modelEntity.deathAnimationPlaying) {
            String deathAnim = modelEntity.pickWeightedDeathAnim();
            if (deathAnim == null || deathAnim.isEmpty()) {
                deathAnim = display.getCustomModelData().getHurtAnim();
            }
            if (deathAnim != null && !deathAnim.isEmpty()) {
                modelEntity.playDeathAnimation(deathAnim);
            }
        }
        lastDeathTime = this.deathTime;

        // Attack frame-based damage (server only, matches 1.20.1 logic)
        if (!level().isClientSide) {
            if (modelEntity.currentAttackAnim != null && modelEntity.attackingTarget != null && !modelEntity.attackDamageDealt && modelEntity.currentAttackFrame > 0) {
                int elapsedTicks = tickCount - modelEntity.attackAnimStartTick;
                int targetTicks = (int)(modelEntity.currentAttackFrame * 20.0f);
                if (elapsedTicks >= targetTicks && targetTicks > 0) {
                    modelEntity.frameAttackInProgress = true;
                    this.doHurtTarget(modelEntity.attackingTarget);
                    modelEntity.frameAttackInProgress = false;
                    modelEntity.attackDamageDealt = true;
                    playSound(modelEntity.currentAttackSound);
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

    @Inject(method = "die", at = @At("HEAD"))
    public void onDie(DamageSource source, CallbackInfo ci) {
        if (level().isClientSide) return;
        Entity entity = this.modelData.getEntity(this);
        if (!(entity instanceof EntityCustomModel modelEntity)) return;
        if (modelEntity.deathAnimationPlaying) return;
        IDataDisplay display = (IDataDisplay) this.display;
        CustomModelData data = display.getCustomModelData();
        String deathAnim = (data.getDeathAnimCount() > 0) ? modelEntity.pickWeightedDeathAnim() : "";
        modelEntity.playDeathAnimation(deathAnim);
    }

    @Unique
    private void playSound(String soundId) {
        if (soundId == null || soundId.isEmpty()) return;
        if (level().isClientSide) return;
        ResourceLocation sndLoc = ResourceLocation.tryParse(soundId);
        if (sndLoc == null) return;
        this.level().playSound(null, this.blockPosition(), SoundEvent.createVariableRangeEvent(sndLoc), SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
