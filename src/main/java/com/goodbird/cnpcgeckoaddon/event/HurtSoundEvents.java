package com.goodbird.cnpcgeckoaddon.event;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.entity.EntityCustomNpc;

/**
 * Hurt sound is independent of animation state.
 * Fires once per successful damage application on a gecko NPC.
 */
public class HurtSoundEvents {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof EntityCustomNpc npc)) return;
        if (event.getAmount() <= 0) return;

        Entity model = npc.modelData.getEntity(npc);
        if (!(model instanceof EntityCustomModel em)) return;

        String sound = pickWeightedHurtSound(em, npc);
        if (sound == null || sound.isEmpty()) return;

        ResourceLocation loc;
        try {
            loc = new ResourceLocation(sound);
        } catch (Exception e) {
            return;
        }
        npc.level().playSound(null, npc.blockPosition(),
                SoundEvent.createVariableRangeEvent(loc),
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static String pickWeightedHurtSound(EntityCustomModel em, EntityCustomNpc npc) {
        int totalWeight = 0;
        int validCount = 0;
        for (int i = 0; i < em.hurtAnimCount; i++) {
            String snd = em.hurtSoundNames[i];
            if (snd != null && !snd.isEmpty()) {
                totalWeight += Math.max(em.hurtWeights[i], 0);
                validCount++;
            }
        }
        if (validCount == 0) return null;
        if (totalWeight <= 0) {
            int idx = npc.getRandom().nextInt(validCount);
            int n = 0;
            for (int i = 0; i < em.hurtAnimCount; i++) {
                String snd = em.hurtSoundNames[i];
                if (snd != null && !snd.isEmpty()) {
                    if (n++ == idx) return snd;
                }
            }
            return null;
        }
        int rand = npc.getRandom().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < em.hurtAnimCount; i++) {
            String snd = em.hurtSoundNames[i];
            if (snd != null && !snd.isEmpty()) {
                cumulative += Math.max(em.hurtWeights[i], 0);
                if (rand < cumulative) return snd;
            }
        }
        return null;
    }
}
