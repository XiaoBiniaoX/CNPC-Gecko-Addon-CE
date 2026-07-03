package com.goodbird.cnpcgeckoaddon.data;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;

public class CustomModelData {
    public static final int MAX_ATTACKS = 20;
    public static final int MAX_HURTS = 20;
    public static final int MAX_DEATHS = 20;

    private String model = CNPCGeckoAddon.MODID + ":geo/geo_npc.geo.json";
    private String animFile = CNPCGeckoAddon.MODID + ":animations/geo_npc.animation.json";
    private String idleAnim = "idle";
    private String walkAnim = "walk";
    private String attackAnim = "";
    private String hurtAnim = "";
    private String headBoneName = "head";
    private int transitionLengthTicks = 10;
    private float width = 0.7f;
    private float height = 2f;
    private boolean hurtTintEnabled = false;
    private String[] attackAnimNames = new String[MAX_ATTACKS];
    private int[] attackWeights = new int[MAX_ATTACKS];
    private float[] attackFrames = new float[MAX_ATTACKS];
    private int attackCount = 5;

    private String[] hurtAnimNames = new String[MAX_HURTS];
    private int[] hurtWeights = new int[MAX_HURTS];
    private int hurtAnimCount = 5;

    private String[] deathAnimNames = new String[MAX_DEATHS];
    private int[] deathWeights = new int[MAX_DEATHS];
    private float[] deathHealthThresholds = new float[MAX_DEATHS];
    private float[] deathAnimDurations = new float[MAX_DEATHS];
    private int deathAnimCount = 5;

    public CustomModelData() {
        for (int i = 0; i < MAX_ATTACKS; i++) {
            attackAnimNames[i] = "";
            attackWeights[i] = 1;
            attackFrames[i] = 0f;
        }
        for (int i = 0; i < MAX_HURTS; i++) {
            hurtAnimNames[i] = "";
            hurtWeights[i] = 1;
        }
        for (int i = 0; i < MAX_DEATHS; i++) {
            deathAnimNames[i] = "";
            deathWeights[i] = 1;
            deathHealthThresholds[i] = 100f;
            deathAnimDurations[i] = 2f;
        }
    }

    public CompoundTag writeToNBT(CompoundTag nbttagcompound) {
        nbttagcompound.putString("Model", model);
        nbttagcompound.putString("AnimFile", animFile);
        nbttagcompound.putString("IdleAnim", idleAnim);
        nbttagcompound.putString("WalkAnim", walkAnim);
        nbttagcompound.putString("AttackAnim", attackAnim);
        nbttagcompound.putString("HurtAnim", hurtAnim);
        nbttagcompound.putString("HeadBoneName", headBoneName);
        nbttagcompound.putInt("TransitionLengthTicks", transitionLengthTicks);
        nbttagcompound.putFloat("Width",width);
        nbttagcompound.putFloat("Height",height);
        nbttagcompound.putBoolean("HurtTintEnabled",hurtTintEnabled);
        nbttagcompound.putInt("AttackCount", attackCount);
        ListTag namesList = new ListTag();
        for (int i = 0; i < attackCount; i++) namesList.add(StringTag.valueOf(attackAnimNames[i]));
        nbttagcompound.put("AttackAnimNames", namesList);
        ListTag weightsList = new ListTag();
        for (int i = 0; i < attackCount; i++) weightsList.add(IntTag.valueOf(attackWeights[i]));
        nbttagcompound.put("AttackWeights", weightsList);
        ListTag framesList = new ListTag();
        for (int i = 0; i < attackCount; i++) framesList.add(FloatTag.valueOf(attackFrames[i]));
        nbttagcompound.put("AttackFrames", framesList);

        nbttagcompound.putInt("HurtAnimCount", hurtAnimCount);
        ListTag hurtNamesList = new ListTag();
        for (int i = 0; i < hurtAnimCount; i++) hurtNamesList.add(StringTag.valueOf(hurtAnimNames[i]));
        nbttagcompound.put("HurtAnimNames", hurtNamesList);
        ListTag hurtWeightsList = new ListTag();
        for (int i = 0; i < hurtAnimCount; i++) hurtWeightsList.add(IntTag.valueOf(hurtWeights[i]));
        nbttagcompound.put("HurtWeights", hurtWeightsList);

        nbttagcompound.putInt("DeathAnimCount", deathAnimCount);
        ListTag deathNamesList = new ListTag();
        for (int i = 0; i < deathAnimCount; i++) deathNamesList.add(StringTag.valueOf(deathAnimNames[i]));
        nbttagcompound.put("DeathAnimNames", deathNamesList);
        ListTag deathWeightsList = new ListTag();
        for (int i = 0; i < deathAnimCount; i++) deathWeightsList.add(IntTag.valueOf(deathWeights[i]));
        nbttagcompound.put("DeathWeights", deathWeightsList);
        ListTag deathThresholdsList = new ListTag();
        for (int i = 0; i < deathAnimCount; i++) deathThresholdsList.add(FloatTag.valueOf(deathHealthThresholds[i]));
        nbttagcompound.put("DeathHealthThresholds", deathThresholdsList);
        ListTag deathDurationsList = new ListTag();
        for (int i = 0; i < deathAnimCount; i++) deathDurationsList.add(FloatTag.valueOf(deathAnimDurations[i]));
        nbttagcompound.put("DeathAnimDurations", deathDurationsList);

        return nbttagcompound;
    }

    public void readFromNBT(CompoundTag nbttagcompound) {
        if (nbttagcompound.contains("Model")) {
            model = nbttagcompound.getString("Model");
            animFile = nbttagcompound.getString("AnimFile");
            idleAnim = nbttagcompound.getString("IdleAnim");
            walkAnim = nbttagcompound.getString("WalkAnim");
            hurtAnim = nbttagcompound.getString("HurtAnim");
            attackAnim = nbttagcompound.getString("AttackAnim");
            headBoneName = nbttagcompound.getString("HeadBoneName");
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
            } else {
                attackCount = 5;
            }
            attackCount = Math.min(attackCount, MAX_ATTACKS);

            if (nbttagcompound.contains("AttackAnimNames")) {
                ListTag namesList = nbttagcompound.getList("AttackAnimNames", 8);
                for (int i = 0; i < Math.min(namesList.size(), attackCount); i++)
                    attackAnimNames[i] = namesList.getString(i);
            }
            if (nbttagcompound.contains("AttackWeights")) {
                ListTag weightsList = nbttagcompound.getList("AttackWeights", 3);
                for (int i = 0; i < Math.min(weightsList.size(), attackCount); i++)
                    attackWeights[i] = weightsList.getInt(i);
            }
            if (nbttagcompound.contains("AttackFrames")) {
                ListTag framesList = nbttagcompound.getList("AttackFrames", 5);
                for (int i = 0; i < Math.min(framesList.size(), attackCount); i++)
                    attackFrames[i] = framesList.getFloat(i);
            }

            if (nbttagcompound.contains("HurtAnimCount")) {
                hurtAnimCount = Math.min(nbttagcompound.getInt("HurtAnimCount"), MAX_HURTS);
                if (hurtAnimCount <= 0) hurtAnimCount = 5;
                if (nbttagcompound.contains("HurtAnimNames")) {
                    ListTag list = nbttagcompound.getList("HurtAnimNames", 8);
                    for (int i = 0; i < Math.min(list.size(), hurtAnimCount); i++)
                        hurtAnimNames[i] = list.getString(i);
                }
                if (nbttagcompound.contains("HurtWeights")) {
                    ListTag list = nbttagcompound.getList("HurtWeights", 3);
                    for (int i = 0; i < Math.min(list.size(), hurtAnimCount); i++)
                        hurtWeights[i] = list.getInt(i);
                }
            }

            if (nbttagcompound.contains("DeathAnimCount")) {
                deathAnimCount = Math.min(nbttagcompound.getInt("DeathAnimCount"), MAX_DEATHS);
                if (deathAnimCount <= 0) deathAnimCount = 5;
                if (nbttagcompound.contains("DeathAnimNames")) {
                    ListTag list = nbttagcompound.getList("DeathAnimNames", 8);
                    for (int i = 0; i < Math.min(list.size(), deathAnimCount); i++)
                        deathAnimNames[i] = list.getString(i);
                }
                if (nbttagcompound.contains("DeathWeights")) {
                    ListTag list = nbttagcompound.getList("DeathWeights", 3);
                    for (int i = 0; i < Math.min(list.size(), deathAnimCount); i++)
                        deathWeights[i] = list.getInt(i);
                }
                if (nbttagcompound.contains("DeathHealthThresholds")) {
                    ListTag list = nbttagcompound.getList("DeathHealthThresholds", 5);
                    for (int i = 0; i < Math.min(list.size(), deathAnimCount); i++)
                        deathHealthThresholds[i] = list.getFloat(i);
                }
                if (nbttagcompound.contains("DeathAnimDurations")) {
                    ListTag list = nbttagcompound.getList("DeathAnimDurations", 5);
                    for (int i = 0; i < Math.min(list.size(), deathAnimCount); i++)
                        deathAnimDurations[i] = list.getFloat(i);
                }
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
    public String getAttackAnim() { return attackAnim; }
    public void setAttackAnim(String attackAnim) { this.attackAnim = attackAnim; }
    public String getHurtAnim() { return hurtAnim; }
    public void setHurtAnim(String hurtAnim) { this.hurtAnim = hurtAnim; }
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

    public String[] getAttackAnimNames() { return attackAnimNames; }
    public void setAttackAnimNames(String[] names) { this.attackAnimNames = names; }
    public int[] getAttackWeights() { return attackWeights; }
    public void setAttackWeights(int[] weights) { this.attackWeights = weights; }
    public float[] getAttackFrames() { return attackFrames; }
    public void setAttackFrames(float[] frames) { this.attackFrames = frames; }
    public int getAttackCount() { return attackCount; }
    public void setAttackCount(int count) { this.attackCount = Math.min(count, MAX_ATTACKS); }
    public void addAttack() {
        if (attackCount < MAX_ATTACKS) {
            attackAnimNames[attackCount] = "";
            attackWeights[attackCount] = 1;
            attackFrames[attackCount] = 0f;
            attackCount++;
        }
    }
    public void removeAttack(int index) {
        if (index < 0 || index >= attackCount || attackCount <= 0) return;
        for (int i = index; i < attackCount - 1; i++) {
            attackAnimNames[i] = attackAnimNames[i + 1];
            attackWeights[i] = attackWeights[i + 1];
            attackFrames[i] = attackFrames[i + 1];
        }
        attackCount--;
        attackAnimNames[attackCount] = "";
        attackWeights[attackCount] = 1;
        attackFrames[attackCount] = 0f;
    }

    public String[] getHurtAnimNames() { return hurtAnimNames; }
    public int[] getHurtWeights() { return hurtWeights; }
    public int getHurtAnimCount() { return hurtAnimCount; }
    public void setHurtAnimCount(int count) { this.hurtAnimCount = Math.min(count, MAX_HURTS); }
    public void addHurtAnim() {
        if (hurtAnimCount < MAX_HURTS) {
            hurtAnimNames[hurtAnimCount] = "";
            hurtWeights[hurtAnimCount] = 1;
            hurtAnimCount++;
        }
    }
    public void removeHurtAnim(int index) {
        if (index < 0 || index >= hurtAnimCount || hurtAnimCount <= 0) return;
        for (int i = index; i < hurtAnimCount - 1; i++) {
            hurtAnimNames[i] = hurtAnimNames[i + 1];
            hurtWeights[i] = hurtWeights[i + 1];
        }
        hurtAnimCount--;
        hurtAnimNames[hurtAnimCount] = "";
        hurtWeights[hurtAnimCount] = 1;
    }

    public String[] getDeathAnimNames() { return deathAnimNames; }
    public int[] getDeathWeights() { return deathWeights; }
    public float[] getDeathHealthThresholds() { return deathHealthThresholds; }
    public float[] getDeathAnimDurations() { return deathAnimDurations; }
    public int getDeathAnimCount() { return deathAnimCount; }
    public void setDeathAnimCount(int count) { this.deathAnimCount = Math.min(count, MAX_DEATHS); }
    public void addDeathAnim() {
        if (deathAnimCount < MAX_DEATHS) {
            deathAnimNames[deathAnimCount] = "";
            deathWeights[deathAnimCount] = 1;
            deathHealthThresholds[deathAnimCount] = 100f;
            deathAnimDurations[deathAnimCount] = 2f;
            deathAnimCount++;
        }
    }
    public void removeDeathAnim(int index) {
        if (index < 0 || index >= deathAnimCount || deathAnimCount <= 0) return;
        for (int i = index; i < deathAnimCount - 1; i++) {
            deathAnimNames[i] = deathAnimNames[i + 1];
            deathWeights[i] = deathWeights[i + 1];
            deathHealthThresholds[i] = deathHealthThresholds[i + 1];
            deathAnimDurations[i] = deathAnimDurations[i + 1];
        }
        deathAnimCount--;
        deathAnimNames[deathAnimCount] = "";
        deathWeights[deathAnimCount] = 1;
        deathHealthThresholds[deathAnimCount] = 100f;
        deathAnimDurations[deathAnimCount] = 2f;
    }

}
