package com.goodbird.cnpcgeckoaddon.data;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class CustomModelData {
    public static final int MAX_ATTACKS = 20;
    public static final int MAX_HURTS = 20;
    public static final int MAX_DEATHS = 20;

    private String model = CNPCGeckoAddon.MODID + ":geo/geo_npc.geo.json";
    private String animFile = CNPCGeckoAddon.MODID + ":animations/geo_npc.animation.json";
    private String idleAnim = "idle";
    private String walkAnim = "walk";
    private String headBoneName = "head";
    private int transitionLengthTicks = 10;
    private float width = 0.7f;
    private float height = 2f;
    private boolean hurtTintEnabled = false;

    private String[] attackAnimNames = new String[MAX_ATTACKS];
    private int[] attackWeights = new int[MAX_ATTACKS];
    private float[] attackFrames = new float[MAX_ATTACKS];
    private String[] attackSoundNames = new String[MAX_ATTACKS];
    private int attackCount = 0;

    private String[] hurtAnimNames = new String[MAX_HURTS];
    private int[] hurtWeights = new int[MAX_HURTS];
    private String[] hurtSoundNames = new String[MAX_HURTS];
    private int hurtAnimCount = 0;

    private String[] deathAnimNames = new String[MAX_DEATHS];
    private int[] deathWeights = new int[MAX_DEATHS];
    private float[] deathHealthThresholds = new float[MAX_DEATHS];
    private float[] deathAnimDurations = new float[MAX_DEATHS];
    private int deathAnimCount = 0;

    public CompoundTag writeToNBT(CompoundTag nbttagcompound) {
        nbttagcompound.putString("Model", model);
        nbttagcompound.putString("AnimFile", animFile);
        nbttagcompound.putString("IdleAnim", idleAnim);
        nbttagcompound.putString("WalkAnim", walkAnim);
        nbttagcompound.putString("HeadBoneName", headBoneName);
        nbttagcompound.putInt("TransitionLengthTicks", transitionLengthTicks);
        nbttagcompound.putFloat("Width", width);
        nbttagcompound.putFloat("Height", height);
        nbttagcompound.putBoolean("HurtTintEnabled", hurtTintEnabled);

        nbttagcompound.putInt("AttackCount", attackCount);
        ListTag attackList = new ListTag();
        for (int i = 0; i < attackCount; i++) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", attackAnimNames[i]);
            tag.putInt("weight", attackWeights[i]);
            tag.putFloat("frame", attackFrames[i]);
            String snd = attackSoundNames[i];
            if (snd != null && !snd.isEmpty()) tag.putString("sound", snd);
            attackList.add(tag);
        }
        nbttagcompound.put("AttackList", attackList);

        nbttagcompound.putInt("HurtAnimCount", hurtAnimCount);
        ListTag hurtList = new ListTag();
        for (int i = 0; i < hurtAnimCount; i++) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", hurtAnimNames[i]);
            tag.putInt("weight", hurtWeights[i]);
            String snd = hurtSoundNames[i];
            if (snd != null && !snd.isEmpty()) tag.putString("sound", snd);
            hurtList.add(tag);
        }
        nbttagcompound.put("HurtList", hurtList);

        nbttagcompound.putInt("DeathAnimCount", deathAnimCount);
        ListTag deathList = new ListTag();
        for (int i = 0; i < deathAnimCount; i++) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", deathAnimNames[i]);
            tag.putInt("weight", deathWeights[i]);
            tag.putFloat("hpThreshold", deathHealthThresholds[i]);
            tag.putFloat("duration", deathAnimDurations[i]);
            deathList.add(tag);
        }
        nbttagcompound.put("DeathList", deathList);
        return nbttagcompound;
    }

    public void readFromNBT(CompoundTag nbttagcompound) {
        if (!nbttagcompound.contains("Model")) return;
        model = nbttagcompound.getString("Model");
        animFile = nbttagcompound.getString("AnimFile");
        idleAnim = nbttagcompound.getString("IdleAnim");
        walkAnim = nbttagcompound.getString("WalkAnim");
        if (nbttagcompound.contains("HeadBoneName"))
            headBoneName = nbttagcompound.getString("HeadBoneName");
        if (nbttagcompound.contains("Width"))
            width = nbttagcompound.getFloat("Width");
        if (nbttagcompound.contains("Height"))
            height = nbttagcompound.getFloat("Height");
        if (nbttagcompound.contains("TransitionLengthTicks"))
            transitionLengthTicks = nbttagcompound.getInt("TransitionLengthTicks");
        if (nbttagcompound.contains("HurtTintEnabled"))
            hurtTintEnabled = nbttagcompound.getBoolean("HurtTintEnabled");

        if (nbttagcompound.contains("AttackCount")) {
            attackCount = nbttagcompound.getInt("AttackCount");
            ListTag attackList = nbttagcompound.getList("AttackList", 10);
            for (int i = 0; i < attackCount && i < attackList.size(); i++) {
                CompoundTag tag = attackList.getCompound(i);
                attackAnimNames[i] = tag.getString("name");
                attackWeights[i] = tag.getInt("weight");
                attackFrames[i] = tag.getFloat("frame");
                attackSoundNames[i] = tag.contains("sound") ? tag.getString("sound") : "";
            }
        }
        if (nbttagcompound.contains("HurtAnimCount")) {
            hurtAnimCount = nbttagcompound.getInt("HurtAnimCount");
            ListTag hurtList = nbttagcompound.getList("HurtList", 10);
            for (int i = 0; i < hurtAnimCount && i < hurtList.size(); i++) {
                CompoundTag tag = hurtList.getCompound(i);
                hurtAnimNames[i] = tag.getString("name");
                hurtWeights[i] = tag.getInt("weight");
                hurtSoundNames[i] = tag.contains("sound") ? tag.getString("sound") : "";
            }
        }
        if (nbttagcompound.contains("DeathAnimCount")) {
            deathAnimCount = nbttagcompound.getInt("DeathAnimCount");
            ListTag deathList = nbttagcompound.getList("DeathList", 10);
            for (int i = 0; i < deathAnimCount && i < deathList.size(); i++) {
                CompoundTag tag = deathList.getCompound(i);
                deathAnimNames[i] = tag.getString("name");
                deathWeights[i] = tag.getInt("weight");
                deathHealthThresholds[i] = tag.getFloat("hpThreshold");
                deathAnimDurations[i] = tag.getFloat("duration");
            }
        }
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getAnimFile() { return animFile; }
    public void setAnimFile(String animFile) { this.animFile = animFile; }
    public String getIdleAnim() { return idleAnim; }
    public void setIdleAnim(String idleAnim) { this.idleAnim = idleAnim; }
    public String getWalkAnim() { return walkAnim; }
    public void setWalkAnim(String walkAnim) { this.walkAnim = walkAnim; }
    public String getHeadBoneName() { return headBoneName; }
    public void setHeadBoneName(String headBoneName) { this.headBoneName = headBoneName; }
    public int getTransitionLengthTicks() { return transitionLengthTicks; }
    public void setTransitionLengthTicks(int transitionLengthTicks) { this.transitionLengthTicks = transitionLengthTicks; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public boolean isHurtTintEnabled() { return hurtTintEnabled; }
    public void setHurtTintEnabled(boolean value) { this.hurtTintEnabled = value; }
    public String getAttackAnim() { return attackAnimNames[0]; }
    public String getHurtAnim() { return hurtAnimNames[0]; }

    public String[] getAttackAnimNames() { return attackAnimNames; }
    public int[] getAttackWeights() { return attackWeights; }
    public float[] getAttackFrames() { return attackFrames; }
    public String[] getAttackSoundNames() { return attackSoundNames; }
    public int getAttackCount() { return attackCount; }
    public void addAttack() {
        if (attackCount < MAX_ATTACKS) {
            attackSoundNames[attackCount] = "";
            attackCount++;
        }
    }
    public void removeAttack(int idx) {
        if (idx < 0 || idx >= attackCount) return;
        System.arraycopy(attackAnimNames, idx + 1, attackAnimNames, idx, attackCount - idx - 1);
        System.arraycopy(attackWeights, idx + 1, attackWeights, idx, attackCount - idx - 1);
        System.arraycopy(attackFrames, idx + 1, attackFrames, idx, attackCount - idx - 1);
        System.arraycopy(attackSoundNames, idx + 1, attackSoundNames, idx, attackCount - idx - 1);
        attackAnimNames[attackCount - 1] = null;
        attackWeights[attackCount - 1] = 0;
        attackFrames[attackCount - 1] = 0;
        attackSoundNames[attackCount - 1] = "";
        attackCount--;
    }

    public String[] getHurtAnimNames() { return hurtAnimNames; }
    public int[] getHurtWeights() { return hurtWeights; }
    public String[] getHurtSoundNames() { return hurtSoundNames; }
    public int getHurtAnimCount() { return hurtAnimCount; }
    public void addHurtAnim() {
        if (hurtAnimCount < MAX_HURTS) {
            hurtSoundNames[hurtAnimCount] = "";
            hurtAnimCount++;
        }
    }
    public void removeHurtAnim(int idx) {
        if (idx < 0 || idx >= hurtAnimCount) return;
        System.arraycopy(hurtAnimNames, idx + 1, hurtAnimNames, idx, hurtAnimCount - idx - 1);
        System.arraycopy(hurtWeights, idx + 1, hurtWeights, idx, hurtAnimCount - idx - 1);
        System.arraycopy(hurtSoundNames, idx + 1, hurtSoundNames, idx, hurtAnimCount - idx - 1);
        hurtAnimNames[hurtAnimCount - 1] = null;
        hurtWeights[hurtAnimCount - 1] = 0;
        hurtSoundNames[hurtAnimCount - 1] = "";
        hurtAnimCount--;
    }

    public String[] getDeathAnimNames() { return deathAnimNames; }
    public int[] getDeathWeights() { return deathWeights; }
    public float[] getDeathHealthThresholds() { return deathHealthThresholds; }
    public float[] getDeathAnimDurations() { return deathAnimDurations; }
    public int getDeathAnimCount() { return deathAnimCount; }
    public void addDeathAnim() {
        if (deathAnimCount < MAX_DEATHS) deathAnimCount++;
    }
    public void removeDeathAnim(int idx) {
        if (idx < 0 || idx >= deathAnimCount) return;
        System.arraycopy(deathAnimNames, idx + 1, deathAnimNames, idx, deathAnimCount - idx - 1);
        System.arraycopy(deathWeights, idx + 1, deathWeights, idx, deathAnimCount - idx - 1);
        System.arraycopy(deathHealthThresholds, idx + 1, deathHealthThresholds, idx, deathAnimCount - idx - 1);
        System.arraycopy(deathAnimDurations, idx + 1, deathAnimDurations, idx, deathAnimCount - idx - 1);
        deathAnimNames[deathAnimCount - 1] = null;
        deathWeights[deathAnimCount - 1] = 0;
        deathHealthThresholds[deathAnimCount - 1] = 0;
        deathAnimDurations[deathAnimCount - 1] = 0;
        deathAnimCount--;
    }
}