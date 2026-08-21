package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if (level().isClientSide) {
            // 补放：脚本动画包到达时实体还没加载/模型实体还没创建时被缓存，
            // 这里在客户端 tick 里尝试补放，直到成功或超时放弃。
            PacketSyncAnimation.PendingAnim pending = PacketSyncAnimation.PENDING.remove(this.getId());
            if (pending != null) {
                Entity pendingEntity = this.modelData.getEntity(this);
                if (pendingEntity instanceof EntityCustomModel pendingModel) {
                    pendingModel.requestManualAnim(pending.animName, pending.instant, pending.priority);
                } else {
                    if (++pending.tries < 100) {
                        PacketSyncAnimation.PENDING.put(this.getId(), pending);
                    }
                }
            }
        }
        IDataDisplay display = (IDataDisplay) this.display;
        Entity entity = this.modelData.getEntity(this);
        if (!(entity instanceof EntityCustomModel)) {
            // 不打印日志：未配置 Geo 模型的普通 NPC 每 tick 都会走到这里，
            // 打印会造成日志每秒数百行的膨胀（曾 3 分钟写满 1GB）。
            return;
        }
        EntityCustomModel modelEntity = (EntityCustomModel) entity;
        CustomModelData data = display.getCustomModelData();

        // Keep the model entity in sync with the config every tick.
        // Previously this only happened in EntityUtil.Copy, so after editing the GUI the
        // NPC kept using stale animation/sound tables until it was respawned.
        modelEntity.owner = this;
        gecko$syncConfig(modelEntity, data);

        if (data.getHeight() != modelEntity.getBbHeight() || data.getWidth() != modelEntity.getBbWidth()) {
            modelEntity.setSize(data.getWidth(), data.getHeight());
            // Refresh the model entity too, otherwise its bounding box never matches the
            // config and this branch runs every tick, re-resetting the NPC hitbox
            // continuously (which breaks collision with other NPCs).
            modelEntity.refreshDimensions();
            this.refreshDimensions();
        }

        modelEntity.deathTime = this.deathTime;
        if (data.isHurtTintEnabled()) {
            modelEntity.hurtTime = this.hurtTime;
        } else {
            modelEntity.hurtTime = 0;
        }

        // Hurt animation only (sound is handled by HurtSoundEvents, independent of anim)
        // 原先此处要求 currentAttackAnim == null（攻击中不播受伤动画），与约定的
        // 「受伤 > 攻击」相反，改由 EntityCustomModel 的优先级仲裁决定是否覆盖。
        //
        // 必须限定服务端触发：hurtTime / deathTime 两端都会同步，客户端原先也会跑到这里，
        // 于是同一次受伤被「掷两次骰子」——服务端 pickWeightedHurtAnim 抽中 A 发包过来，
        // 客户端本地又抽中 B 覆盖掉刚收到的 A（实测日志里 skill5 被 attack 顶掉、
        // attack 被 skill5 顶掉，同一毫秒内两次 START）。表现就是受伤/攻击动画
        // 播一半被替换、随机抽搐。动画一律由服务端定夺、发包同步给所有客户端。
        if (!level().isClientSide) {
            if (this.hurtTime > 0 && !modelEntity.hurtAnimationPlaying) {
                String hurtAnim = modelEntity.pickWeightedHurtAnim();
                if (hurtAnim == null) hurtAnim = data.getHurtAnim();
                if (hurtAnim != null && !hurtAnim.isEmpty()) {
                    modelEntity.playHurtAnimation(hurtAnim);
                    modelEntity.hurtAnimationPlaying = true;
                }
            }
            if (this.hurtTime <= 0) {
                modelEntity.hurtAnimationPlaying = false;
            }

            if (this.deathTime == 1 && !modelEntity.deathAnimationPlaying) {
                String deathAnim = modelEntity.pickWeightedDeathAnim();
                if (deathAnim == null || deathAnim.isEmpty()) {
                    deathAnim = data.getHurtAnim();
                }
                if (deathAnim != null && !deathAnim.isEmpty()) {
                    modelEntity.playDeathAnimation(deathAnim);
                }
            }
            if (this.deathTime <= 0) {
                modelEntity.deathAnimationPlaying = false;
            }
        }

        if (!level().isClientSide) {
            // Attack sound only tied to damage frame delay
            if (modelEntity.currentAttackAnim != null && modelEntity.attackingTarget != null && !modelEntity.attackDamageDealt && modelEntity.currentAttackFrame > 0) {
                int elapsedTicks = tickCount - modelEntity.attackAnimStartTick;
                int targetTicks = (int)(modelEntity.currentAttackFrame * 20.0f);
                if (elapsedTicks >= targetTicks && targetTicks > 0) {
                    modelEntity.frameAttackInProgress = true;
                    this.doHurtTarget(modelEntity.attackingTarget);
                    modelEntity.frameAttackInProgress = false;
                    modelEntity.attackDamageDealt = true;
                    playGeckoSound(modelEntity.currentAttackSound);
                }
            }
            // 兜底收尾：帧结算被异常跳过（如目标失效）时，在 target 之后再等 60 tick 强制收尾。
            // 之前固定 200 tick（10 秒），在配置 10s/12s 延时时会抢先于帧结算触发，
            // 且兜底补刀未置 frameAttackInProgress 被守卫吞掉 → 无伤害 + 状态被清（延时伤害失效）。
            int animTimeout = Math.max((int)(modelEntity.currentAttackFrame * 20.0f), 200) + 60;
            if (modelEntity.currentAttackAnim != null && (tickCount - modelEntity.attackAnimStartTick > animTimeout)) {
                if (!modelEntity.attackDamageDealt && modelEntity.attackingTarget != null && modelEntity.attackingTarget.isAlive()) {
                    modelEntity.frameAttackInProgress = true;
                    this.doHurtTarget(modelEntity.attackingTarget);
                    modelEntity.frameAttackInProgress = false;
                    playGeckoSound(modelEntity.currentAttackSound);
                }
                modelEntity.resetAttackState();
            }
        }
    }

    @Unique
    private void gecko$syncConfig(EntityCustomModel modelEntity, CustomModelData data) {
        ResourceLocation model = gecko$parse(data.getModel());
        if (model != null) modelEntity.modelResLoc = model;
        ResourceLocation animFile = gecko$parse(data.getAnimFile());
        if (animFile != null) modelEntity.animResLoc = animFile;

        modelEntity.idleAnim = gecko$orEmpty(data.getIdleAnim());
        modelEntity.walkAnim = gecko$orEmpty(data.getWalkAnim());
        modelEntity.attackAnim = gecko$orEmpty(data.getAttackAnim());
        modelEntity.hurtAnim = gecko$orEmpty(data.getHurtAnim());
        modelEntity.headBoneName = gecko$orEmpty(data.getHeadBoneName());
        modelEntity.size = this.display.getSize();

        modelEntity.attackCount = Math.min(data.getAttackCount(), CustomModelData.MAX_ATTACKS);
        System.arraycopy(data.getAttackAnimNames(), 0, modelEntity.attackAnimNames, 0, CustomModelData.MAX_ATTACKS);
        System.arraycopy(data.getAttackWeights(), 0, modelEntity.attackWeights, 0, CustomModelData.MAX_ATTACKS);
        System.arraycopy(data.getAttackFrames(), 0, modelEntity.attackFrames, 0, CustomModelData.MAX_ATTACKS);
        System.arraycopy(data.getAttackSoundNames(), 0, modelEntity.attackSoundNames, 0, CustomModelData.MAX_ATTACKS);

        modelEntity.hurtAnimCount = Math.min(data.getHurtAnimCount(), CustomModelData.MAX_HURTS);
        System.arraycopy(data.getHurtAnimNames(), 0, modelEntity.hurtAnimNames, 0, CustomModelData.MAX_HURTS);
        System.arraycopy(data.getHurtWeights(), 0, modelEntity.hurtWeights, 0, CustomModelData.MAX_HURTS);
        System.arraycopy(data.getHurtSoundNames(), 0, modelEntity.hurtSoundNames, 0, CustomModelData.MAX_HURTS);

        modelEntity.deathAnimCount = Math.min(data.getDeathAnimCount(), CustomModelData.MAX_DEATHS);
        System.arraycopy(data.getDeathAnimNames(), 0, modelEntity.deathAnimNames, 0, CustomModelData.MAX_DEATHS);
        System.arraycopy(data.getDeathWeights(), 0, modelEntity.deathWeights, 0, CustomModelData.MAX_DEATHS);
        System.arraycopy(data.getDeathHealthThresholds(), 0, modelEntity.deathHealthThresholds, 0, CustomModelData.MAX_DEATHS);
        System.arraycopy(data.getDeathAnimDurations(), 0, modelEntity.deathAnimDurations, 0, CustomModelData.MAX_DEATHS);

        // Only ask for an animation resync when the animation config really changed.
        // Doing it unconditionally would restart the base loop and flicker.
        String signature = modelEntity.animResLoc + "|" + modelEntity.idleAnim + "|" + modelEntity.walkAnim
                + "|" + modelEntity.modelResLoc;
        if (!signature.equals(modelEntity.animConfigSignature)) {
            boolean first = modelEntity.animConfigSignature == null;
            modelEntity.animConfigSignature = signature;
            if (!first) modelEntity.requestAnimResync();
        }
    }

    @Unique
    private static String gecko$orEmpty(String value) {
        return value == null ? "" : value;
    }

    @Unique
    private static ResourceLocation gecko$parse(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return new ResourceLocation(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private void playGeckoSound(String soundId) {
        if (soundId == null || soundId.isEmpty()) return;
        if (level().isClientSide) return;
        ResourceLocation sndLoc = gecko$parse(soundId);
        if (sndLoc == null) return;
        SoundEvent event = SoundEvent.createVariableRangeEvent(sndLoc);
        this.level().playSound(null, this.blockPosition(), event, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
