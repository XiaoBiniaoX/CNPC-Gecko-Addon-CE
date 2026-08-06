package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.world.entity.Entity;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityNPCInterface.class)
public class MixinEntityNPCAttack {

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    public void onDoHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;
        System.out.println("[GeckoDBG] doHurtTarget: " + self.getName().getString() + " client=" + self.level().isClientSide);
        if (!(self instanceof EntityCustomNpc customNpc)) { System.out.println("[GeckoDBG]   -> not EntityCustomNpc"); return; }
        if (!(customNpc.modelData.getEntity(self) instanceof EntityCustomModel em)) {
            System.out.println("[GeckoDBG]   -> getEntity()=" + customNpc.modelData.getEntity(self) + " not EntityCustomModel");
            return;
        }
        if (em.frameAttackInProgress) { System.out.println("[GeckoDBG]   -> frameAttackInProgress"); return; }
        // 动画等待帧伤害结算期间，AI 冷却到期的重复攻击调用直接吞掉：
        // 否则每次调用都会无条件重置 attackAnimStartTick，伤害延迟永远累计不到，
        // 帧伤害分支永不触发，造成「设置了伤害延迟后连伤害都不造成」。
        if (em.currentAttackAnim != null && !em.attackDamageDealt && em.currentAttackFrame > 0) {
            System.out.println("[GeckoDBG]   -> attack anim pending, swallow duplicate attack");
            cir.setReturnValue(false);
            cir.cancel();
            return;
        }
        if (em.attackAnimNames == null) { System.out.println("[GeckoDBG]   -> attackAnimNames null"); return; }

        String selectedAnim = weightedRandomPick(self, em.attackAnimNames, em.attackWeights, em.attackCount);
        System.out.println("[GeckoDBG]   -> selectedAnim=" + selectedAnim + " count=" + em.attackCount + " names[0]=" + (em.attackAnimNames[0] == null ? "null" : em.attackAnimNames[0]));
        if (selectedAnim == null || selectedAnim.isEmpty()) { System.out.println("[GeckoDBG]   -> empty anim, skip"); return; }

        float frame = 0;
        for (int i = 0; i < em.attackCount; i++) {
            if (selectedAnim.equals(em.attackAnimNames[i])) {
                frame = em.attackFrames[i];
                break;
            }
        }

        em.startAttackAnimation(selectedAnim, frame, target);
        em.attackAnimStartTick = self.tickCount;

        if (frame > 0) {
            cir.setReturnValue(false);
            cir.cancel();
        } else {
            // frame==0: damage applies immediately; sound plays with the hit (not with anim)
            em.attackDamageDealt = true;
            playAttackSound(self, em.currentAttackSound);
        }
    }

    @Unique
    private void playAttackSound(EntityNPCInterface self, String soundId) {
        if (soundId == null || soundId.isEmpty()) return;
        if (self.level().isClientSide) return;
        net.minecraft.resources.ResourceLocation loc;
        try {
            loc = new net.minecraft.resources.ResourceLocation(soundId);
        } catch (Exception e) {
            return;
        }
        self.level().playSound(null, self.blockPosition(),
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(loc),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @Unique
    private String weightedRandomPick(EntityNPCInterface npc, String[] anims, int[] weights, int count) {
        int totalWeight = 0;
        int validCount = 0;
        for (int i = 0; i < count; i++) {
            if (anims[i] != null && !anims[i].isEmpty()) {
                totalWeight += Math.max(weights[i], 0);
                validCount++;
            }
        }
        if (validCount == 0 || totalWeight <= 0) return null;
        int rand = npc.getRandom().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < count; i++) {
            if (anims[i] != null && !anims[i].isEmpty()) {
                cumulative += Math.max(weights[i], 0);
                if (rand < cumulative) return anims[i];
            }
        }
        return null;
    }
}
