package com.goodbird.cnpcgeckoaddon.event;

import com.goodbird.cnpcgeckoaddon.data.CustomModelDataProvider;
import com.goodbird.cnpcgeckoaddon.data.ICustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.hooklib.cnpchooks.CommonHooks;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import net.minecraft.entity.Entity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noppes.npcs.entity.EntityCustomNpc;
import software.bernie.geckolib3.core.builder.AnimationBuilder;

public class HurtSoundEvents {

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().world == null || event.getEntity().world.isRemote) return;
        if (!(event.getEntity() instanceof EntityCustomNpc)) return;
        if (event.getAmount() <= 0) return;

        EntityCustomNpc npc = (EntityCustomNpc) event.getEntity();
        ICustomModelData data = npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
        if (data == null) return;

        Entity model = npc.modelData != null ? npc.modelData.getEntity(npc) : null;
        EntityCustomModel em = model instanceof EntityCustomModel ? (EntityCustomModel) model : null;

        String[] soundNames;
        int[] weights;
        int count;
        String[] animNames;
        if (em != null && em.hurtSoundNames != null && em.hurtSoundNames.length > 0 && em.hurtAnimCount > 0) {
            soundNames = em.hurtSoundNames;
            weights = em.hurtWeights;
            count = em.hurtAnimCount;
            animNames = em.hurtAnimNames;
        } else if (data.getHurtAnimCount() > 0) {
            soundNames = data.getHurtSoundNames();
            weights = data.getHurtWeights();
            count = data.getHurtAnimCount();
            animNames = data.getHurtAnimNames();
        } else {
            return;
        }

        String sound = pickWeighted(soundNames, weights, count, npc);
        if (sound != null && !sound.isEmpty()) {
            CommonHooks.playGeckoSound(npc, sound);
        }

        // Server-driven hurt anim sync (avoids waiting for client hurtTime)
        if (em != null && em.currentAttackAnim != null) return;
        String hurtAnim = pickWeighted(animNames, weights, count, npc);
        if (hurtAnim != null && !hurtAnim.isEmpty()) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(npc, new AnimationBuilder().playOnce(hurtAnim), true));
        }
    }

    private static String pickWeighted(String[] values, int[] weights, int count, EntityCustomNpc npc) {
        if (values == null || count <= 0) return null;
        int totalWeight = 0;
        int validCount = 0;
        for (int i = 0; i < count && i < values.length; i++) {
            String v = values[i];
            if (v != null && !v.isEmpty()) {
                totalWeight += weights != null && i < weights.length ? Math.max(weights[i], 0) : 1;
                validCount++;
            }
        }
        if (validCount == 0) return null;
        if (totalWeight <= 0) {
            int idx = npc.getRNG().nextInt(validCount);
            int n = 0;
            for (int i = 0; i < count && i < values.length; i++) {
                String v = values[i];
                if (v != null && !v.isEmpty()) {
                    if (n++ == idx) return v;
                }
            }
            return null;
        }
        int rand = npc.getRNG().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < count && i < values.length; i++) {
            String v = values[i];
            if (v != null && !v.isEmpty()) {
                cumulative += weights != null && i < weights.length ? Math.max(weights[i], 0) : 1;
                if (rand < cumulative) return v;
            }
        }
        return null;
    }
}
