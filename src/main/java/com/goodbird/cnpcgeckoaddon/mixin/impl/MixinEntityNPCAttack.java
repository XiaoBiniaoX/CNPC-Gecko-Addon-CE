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
        if (!(self instanceof EntityCustomNpc customNpc)) return;
        if (!(customNpc.modelData.getEntity(self) instanceof EntityCustomModel em)) return;
        if (em.frameAttackInProgress) return;
        if (em.attackAnimNames == null) return;

        String selectedAnim = weightedRandomPick(self, em.attackAnimNames, em.attackWeights, em.attackCount);
        if (selectedAnim == null || selectedAnim.isEmpty()) return;

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
            em.attackDamageDealt = true;
        }
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
        if (validCount == 0) return null;
        if (totalWeight <= 0) {
            int idx = npc.getRandom().nextInt(validCount);
            int n = 0;
            for (int i = 0; i < count; i++) {
                if (anims[i] != null && !anims[i].isEmpty()) {
                    if (n++ == idx) return anims[i];
                }
            }
        }
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