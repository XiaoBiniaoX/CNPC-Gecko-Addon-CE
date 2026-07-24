package com.goodbird.cnpcgeckoaddon.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagFloat;

public class CustomModelData implements ICustomModelData {
    public static final int MAX_ATTACKS = 20;
    public static final int MAX_HURTS = 20;
    public static final int MAX_DEATHS = 20;

    private String model = "geckolib3:geo/bike.geo.json";
    private String animFile = "geckolib3:animations/bike.animation.json";
    private String idleAnim = "animation.bike.idle";
    private String walkAnim = "";
    private String meleeAttackAnim = "";
    private String hurtAnim = "";
    private String rangedAttackAnim = "";
    private String headBoneName = "head";
    private int transitionLengthTicks = 10;
    private float width = 0.7f;
    private float height = 2f;
    private boolean enableHurtTint = true;

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

    public CustomModelData() {
        for (int i = 0; i < MAX_ATTACKS; i++) {
            attackAnimNames[i] = "";
            attackWeights[i] = 1;
            attackFrames[i] = 0f;
            attackSoundNames[i] = "";
        }
        for (int i = 0; i < MAX_HURTS; i++) {
            hurtAnimNames[i] = "";
            hurtWeights[i] = 1;
            hurtSoundNames[i] = "";
        }
        for (int i = 0; i < MAX_DEATHS; i++) {
            deathAnimNames[i] = "";
            deathWeights[i] = 1;
            deathHealthThresholds[i] = 100f;
            deathAnimDurations[i] = 2f;
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
        nbttagcompound.setString("Model", model);
        nbttagcompound.setString("AnimFile", animFile);
        nbttagcompound.setString("IdleAnim", idleAnim);
        nbttagcompound.setString("WalkAnim", walkAnim);
        nbttagcompound.setString("MeleeAttackAnim", meleeAttackAnim);
        nbttagcompound.setString("RangedAttackAnim", rangedAttackAnim);
        nbttagcompound.setString("HurtAnim", hurtAnim);
        nbttagcompound.setString("HeadBoneName", headBoneName);
        nbttagcompound.setInteger("TransitionLengthTicks", transitionLengthTicks);
        nbttagcompound.setFloat("Width",width);
        nbttagcompound.setFloat("Height",height);
        nbttagcompound.setBoolean("EnableHurtTint",enableHurtTint);

        nbttagcompound.setInteger("AttackCount", Math.min(attackCount, MAX_ATTACKS));
        NBTTagList attackNamesList = new NBTTagList();
        int writeAttackCount = Math.min(attackCount, MAX_ATTACKS);
        for (int i = 0; i < writeAttackCount; i++) attackNamesList.appendTag(new NBTTagString(attackAnimNames[i]));
        nbttagcompound.setTag("AttackAnimNames", attackNamesList);
        NBTTagList attackWeightsList = new NBTTagList();
        for (int i = 0; i < writeAttackCount; i++) attackWeightsList.appendTag(new NBTTagInt(attackWeights[i]));
        nbttagcompound.setTag("AttackWeights", attackWeightsList);
        NBTTagList attackFramesList = new NBTTagList();
        for (int i = 0; i < writeAttackCount; i++) attackFramesList.appendTag(new NBTTagFloat(attackFrames[i]));
        nbttagcompound.setTag("AttackFrames", attackFramesList);
        NBTTagList attackSoundsList = new NBTTagList();
        for (int i = 0; i < writeAttackCount; i++) {
            String snd = attackSoundNames[i];
            attackSoundsList.appendTag(new NBTTagString(snd != null ? snd : ""));
        }
        nbttagcompound.setTag("AttackSoundNames", attackSoundsList);

        int writeHurtCount = Math.min(hurtAnimCount, MAX_HURTS);
        nbttagcompound.setInteger("HurtAnimCount", writeHurtCount);
        NBTTagList hurtNamesList = new NBTTagList();
        for (int i = 0; i < writeHurtCount; i++) hurtNamesList.appendTag(new NBTTagString(hurtAnimNames[i]));
        nbttagcompound.setTag("HurtAnimNames", hurtNamesList);
        NBTTagList hurtWeightsList = new NBTTagList();
        for (int i = 0; i < writeHurtCount; i++) hurtWeightsList.appendTag(new NBTTagInt(hurtWeights[i]));
        nbttagcompound.setTag("HurtWeights", hurtWeightsList);
        NBTTagList hurtSoundsList = new NBTTagList();
        for (int i = 0; i < writeHurtCount; i++) {
            String snd = hurtSoundNames[i];
            hurtSoundsList.appendTag(new NBTTagString(snd != null ? snd : ""));
        }
        nbttagcompound.setTag("HurtSoundNames", hurtSoundsList);

        int writeDeathCount = Math.min(deathAnimCount, MAX_DEATHS);
        nbttagcompound.setInteger("DeathAnimCount", writeDeathCount);
        NBTTagList deathNamesList = new NBTTagList();
        for (int i = 0; i < writeDeathCount; i++) deathNamesList.appendTag(new NBTTagString(deathAnimNames[i]));
        nbttagcompound.setTag("DeathAnimNames", deathNamesList);
        NBTTagList deathWeightsList = new NBTTagList();
        for (int i = 0; i < writeDeathCount; i++) deathWeightsList.appendTag(new NBTTagInt(deathWeights[i]));
        nbttagcompound.setTag("DeathWeights", deathWeightsList);
        NBTTagList deathThresholdsList = new NBTTagList();
        for (int i = 0; i < writeDeathCount; i++) deathThresholdsList.appendTag(new NBTTagFloat(deathHealthThresholds[i]));
        nbttagcompound.setTag("DeathHealthThresholds", deathThresholdsList);
        NBTTagList deathDurationsList = new NBTTagList();
        for (int i = 0; i < writeDeathCount; i++) deathDurationsList.appendTag(new NBTTagFloat(deathAnimDurations[i]));
        nbttagcompound.setTag("DeathAnimDurations", deathDurationsList);

        return nbttagcompound;
    }

    public void readFromNBT(NBTTagCompound nbttagcompound) {
        if(nbttagcompound.hasKey("Model")) {
            model = nbttagcompound.getString("Model");
            animFile = nbttagcompound.getString("AnimFile");
            idleAnim = nbttagcompound.getString("IdleAnim");
            walkAnim = nbttagcompound.getString("WalkAnim");
            hurtAnim = nbttagcompound.getString("HurtAnim");
            meleeAttackAnim = nbttagcompound.getString("MeleeAttackAnim");
            rangedAttackAnim = nbttagcompound.getString("RangedAttackAnim");

            if(nbttagcompound.hasKey("HeadBoneName"))
                headBoneName = nbttagcompound.getString("HeadBoneName");

            if(nbttagcompound.hasKey("Width"))
                width = nbttagcompound.getFloat("Width");

            if(nbttagcompound.hasKey("Height"))
                height = nbttagcompound.getFloat("Height");

            if(nbttagcompound.hasKey("TransitionLengthTicks"))
                transitionLengthTicks = nbttagcompound.getInteger("TransitionLengthTicks");

            if(nbttagcompound.hasKey("EnableHurtTint"))
                enableHurtTint = nbttagcompound.getBoolean("EnableHurtTint");

            if (nbttagcompound.hasKey("AttackCount")) {
                attackCount = Math.min(nbttagcompound.getInteger("AttackCount"), MAX_ATTACKS);
                if (nbttagcompound.hasKey("AttackAnimNames")) {
                    NBTTagList list = nbttagcompound.getTagList("AttackAnimNames", 8);
                    for (int i = 0; i < Math.min(list.tagCount(), attackCount); i++)
                        attackAnimNames[i] = list.getStringTagAt(i);
                }
                if (nbttagcompound.hasKey("AttackWeights")) {
                    NBTTagList list = nbttagcompound.getTagList("AttackWeights", 3);
                    for (int i = 0; i < Math.min(list.tagCount(), attackCount); i++)
                        attackWeights[i] = ((NBTTagInt) list.get(i)).getInt();
                }
                if (nbttagcompound.hasKey("AttackFrames")) {
                    NBTTagList list = nbttagcompound.getTagList("AttackFrames", 5);
                    for (int i = 0; i < Math.min(list.tagCount(), attackCount); i++)
                        attackFrames[i] = ((NBTTagFloat) list.get(i)).getFloat();
                }
                if (nbttagcompound.hasKey("AttackSoundNames")) {
                    NBTTagList list = nbttagcompound.getTagList("AttackSoundNames", 8);
                    for (int i = 0; i < Math.min(list.tagCount(), attackCount); i++)
                        attackSoundNames[i] = list.getStringTagAt(i);
                }
            }

            if (nbttagcompound.hasKey("HurtAnimCount")) {
                hurtAnimCount = Math.min(nbttagcompound.getInteger("HurtAnimCount"), MAX_HURTS);
                if (nbttagcompound.hasKey("HurtAnimNames")) {
                    NBTTagList list = nbttagcompound.getTagList("HurtAnimNames", 8);
                    for (int i = 0; i < Math.min(list.tagCount(), hurtAnimCount); i++)
                        hurtAnimNames[i] = list.getStringTagAt(i);
                }
                if (nbttagcompound.hasKey("HurtWeights")) {
                    NBTTagList list = nbttagcompound.getTagList("HurtWeights", 3);
                    for (int i = 0; i < Math.min(list.tagCount(), hurtAnimCount); i++)
                        hurtWeights[i] = ((NBTTagInt) list.get(i)).getInt();
                }
                if (nbttagcompound.hasKey("HurtSoundNames")) {
                    NBTTagList list = nbttagcompound.getTagList("HurtSoundNames", 8);
                    for (int i = 0; i < Math.min(list.tagCount(), hurtAnimCount); i++)
                        hurtSoundNames[i] = list.getStringTagAt(i);
                }
            }

            if (nbttagcompound.hasKey("DeathAnimCount")) {
                deathAnimCount = Math.min(nbttagcompound.getInteger("DeathAnimCount"), MAX_DEATHS);
                if (nbttagcompound.hasKey("DeathAnimNames")) {
                    NBTTagList list = nbttagcompound.getTagList("DeathAnimNames", 8);
                    for (int i = 0; i < Math.min(list.tagCount(), deathAnimCount); i++)
                        deathAnimNames[i] = list.getStringTagAt(i);
                }
                if (nbttagcompound.hasKey("DeathWeights")) {
                    NBTTagList list = nbttagcompound.getTagList("DeathWeights", 3);
                    for (int i = 0; i < Math.min(list.tagCount(), deathAnimCount); i++)
                        deathWeights[i] = ((NBTTagInt) list.get(i)).getInt();
                }
                if (nbttagcompound.hasKey("DeathHealthThresholds")) {
                    NBTTagList list = nbttagcompound.getTagList("DeathHealthThresholds", 5);
                    for (int i = 0; i < Math.min(list.tagCount(), deathAnimCount); i++)
                        deathHealthThresholds[i] = ((NBTTagFloat) list.get(i)).getFloat();
                }
                if (nbttagcompound.hasKey("DeathAnimDurations")) {
                    NBTTagList list = nbttagcompound.getTagList("DeathAnimDurations", 5);
                    for (int i = 0; i < Math.min(list.tagCount(), deathAnimCount); i++)
                        deathAnimDurations[i] = ((NBTTagFloat) list.get(i)).getFloat();
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

    public String getMeleeAttackAnim() { return meleeAttackAnim; }
    public void setMeleeAttackAnim(String meleeAttackAnim) { this.meleeAttackAnim = meleeAttackAnim; }

    public String getHurtAnim() { return hurtAnim; }
    public void setHurtAnim(String hurtAnim) { this.hurtAnim = hurtAnim; }

    public String getRangedAttackAnim() { return rangedAttackAnim; }
    public void setRangedAttackAnim(String rangedAttackAnim) { this.rangedAttackAnim = rangedAttackAnim; }

    public String getHeadBoneName() { return headBoneName; }
    public void setHeadBoneName(String headBoneName) { this.headBoneName = headBoneName; }

    public int getTransitionLengthTicks() { return transitionLengthTicks; }
    public void setTransitionLengthTicks(int transitionLengthTicks) { this.transitionLengthTicks = transitionLengthTicks; }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public boolean isHurtTintEnabled(){ return enableHurtTint; }
    public void setEnableHurtTint(boolean value){ this.enableHurtTint = value; }

    public int getAttackCount() { return attackCount; }
    public String[] getAttackAnimNames() { return attackAnimNames; }
    public int[] getAttackWeights() { return attackWeights; }
    public float[] getAttackFrames() { return attackFrames; }
    public String[] getAttackSoundNames() { return attackSoundNames; }
    public void addAttack() {
        if (attackCount < MAX_ATTACKS) {
            attackAnimNames[attackCount] = "";
            attackWeights[attackCount] = 1;
            attackFrames[attackCount] = 0f;
            attackSoundNames[attackCount] = "";
            attackCount++;
        }
    }
    public void removeAttack(int index) {
        if (index < 0 || index >= attackCount || attackCount <= 0) return;
        for (int i = index; i < attackCount - 1; i++) {
            attackAnimNames[i] = attackAnimNames[i + 1];
            attackWeights[i] = attackWeights[i + 1];
            attackFrames[i] = attackFrames[i + 1];
            attackSoundNames[i] = attackSoundNames[i + 1];
        }
        attackCount--;
        attackAnimNames[attackCount] = "";
        attackWeights[attackCount] = 1;
        attackFrames[attackCount] = 0f;
        attackSoundNames[attackCount] = "";
    }

    public int getHurtAnimCount() { return hurtAnimCount; }
    public String[] getHurtAnimNames() { return hurtAnimNames; }
    public int[] getHurtWeights() { return hurtWeights; }
    public String[] getHurtSoundNames() { return hurtSoundNames; }
    public void addHurtAnim() {
        if (hurtAnimCount < MAX_HURTS) {
            hurtAnimNames[hurtAnimCount] = "";
            hurtWeights[hurtAnimCount] = 1;
            hurtSoundNames[hurtAnimCount] = "";
            hurtAnimCount++;
        }
    }
    public void removeHurtAnim(int index) {
        if (index < 0 || index >= hurtAnimCount || hurtAnimCount <= 0) return;
        for (int i = index; i < hurtAnimCount - 1; i++) {
            hurtAnimNames[i] = hurtAnimNames[i + 1];
            hurtWeights[i] = hurtWeights[i + 1];
            hurtSoundNames[i] = hurtSoundNames[i + 1];
        }
        hurtAnimCount--;
        hurtAnimNames[hurtAnimCount] = "";
        hurtWeights[hurtAnimCount] = 1;
        hurtSoundNames[hurtAnimCount] = "";
    }

    public int getDeathAnimCount() { return deathAnimCount; }
    public String[] getDeathAnimNames() { return deathAnimNames; }
    public int[] getDeathWeights() { return deathWeights; }
    public float[] getDeathHealthThresholds() { return deathHealthThresholds; }
    public float[] getDeathAnimDurations() { return deathAnimDurations; }
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
