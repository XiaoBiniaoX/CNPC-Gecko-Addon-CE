package com.goodbird.cnpcgeckoaddon.data;

import net.minecraft.nbt.NBTTagCompound;

public interface ICustomModelData {
    String getModel();
    void setModel(String model);

    String getAnimFile();
    void setAnimFile(String animFile);

    String getIdleAnim();
    void setIdleAnim(String idleAnim);

    String getWalkAnim();
    void setWalkAnim(String walkAnim);

    String getMeleeAttackAnim();
    void setMeleeAttackAnim(String meleeAttackAnim);

    String getHurtAnim();
    void setHurtAnim(String hurtAnim);

    String getRangedAttackAnim();
    void setRangedAttackAnim(String rangedAttackAnim);

    NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound);
    void readFromNBT(NBTTagCompound nbttagcompound);

    String getHeadBoneName();
    void setHeadBoneName(String headBoneName);

    int getTransitionLengthTicks();
    void setTransitionLengthTicks(int transitionLengthTicks);

    float getWidth();
    void setWidth(float width);

    float getHeight();
    void setHeight(float height);

    boolean isHurtTintEnabled();
    void setEnableHurtTint(boolean value);

    // Attack animation arrays
    int getAttackCount();
    String[] getAttackAnimNames();
    int[] getAttackWeights();
    float[] getAttackFrames();
    void addAttack();
    void removeAttack(int index);

    // Hurt animation arrays
    int getHurtAnimCount();
    String[] getHurtAnimNames();
    int[] getHurtWeights();
    void addHurtAnim();
    void removeHurtAnim(int index);

    // Death animation arrays
    int getDeathAnimCount();
    String[] getDeathAnimNames();
    int[] getDeathWeights();
    float[] getDeathHealthThresholds();
    float[] getDeathAnimDurations();
    void addDeathAnim();
    void removeDeathAnim(int index);
}
