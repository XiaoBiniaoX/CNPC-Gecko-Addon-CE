package com.goodbird.cnpcgeckoaddon.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.Random;

public class EntityCustomModel extends EntityCreature implements IAnimatable, IAnimationTickable {
    private final AnimationFactory factory = new AnimationFactory(this);
    public ResourceLocation modelResLoc = new ResourceLocation("geckolib3", "geo/bike.geo.json");
    public ResourceLocation animResLoc = new ResourceLocation("geckolib3", "animations/bike.animation.json");
    public ResourceLocation textureResLoc = new ResourceLocation("geckolib3", "textures/model/entity/bike.png");
    public String idleAnimName = "";
    public String walkAnimName = "";
    public String hurtAnimName = "";
    public String meleeAttackAnimName = "";
    public String rangedAttackAnimName = "";
    public AnimationBuilder dialogAnim = null;
    public AnimationBuilder manualAnim = null;
    public AnimationBuilder attackAnim = null;
    public AnimationBuilder hurtAnim = null;
    public AnimationBuilder deathAnim = null;
    public String headBoneName = "head";
    public ItemStack leftHeldItem;

    public String[] attackAnimNames = new String[0];
    public int[] attackWeights = new int[0];
    public float[] attackFrames = new float[0];
    public int attackCount = 0;

    public String[] hurtAnimNames = new String[0];
    public int[] hurtWeights = new int[0];
    public int hurtAnimCount = 0;

    public String[] deathAnimNames = new String[0];
    public int[] deathWeights = new int[0];
    public float[] deathHealthThresholds = new float[0];
    public float[] deathAnimDurations = new float[0];
    public int deathAnimCount = 0;

    public EntityLivingBase npcRef = null;
    private int prevHurtTime = 0;
    private boolean wasSwinging = false;
    private boolean deathTriggered = false;

    // Attack offset state (server-side, independent of render predicate)
    public Entity attackingTarget = null;
    public String currentAttackAnim = null;
    public float currentAttackFrame = 0;
    public int attackAnimStartTick = 0;
    public boolean attackDamageDealt = false;
    public boolean frameAttackInProgress = false;
    public boolean delayedAttackPending = false;
    public float cachedDamage = 0;
    public float preAttackHealth = 0;

    private static final Random RAND = new Random();

    private <E extends IAnimatable> PlayState predicateMovement(AnimationEvent<E> event) {
        if (deathAnim != null) {
            if (event.getController().currentAnimationBuilder == deathAnim && event.getController().getAnimationState() == AnimationState.Stopped) {
                return PlayState.STOP;
            }
            if (event.getController().currentAnimationBuilder != deathAnim) {
                event.getController().markNeedsReload();
            }
            event.getController().setAnimation(deathAnim);
            return PlayState.CONTINUE;
        }
        if (hurtAnim != null) {
            if (event.getController().currentAnimationBuilder == hurtAnim && event.getController().getAnimationState() == AnimationState.Stopped) {
                hurtAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != hurtAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(hurtAnim);
                return PlayState.CONTINUE;
            }
        }
        if (attackAnim != null) {
            if (event.getController().currentAnimationBuilder == attackAnim && event.getController().getAnimationState() == AnimationState.Stopped) {
                attackAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != attackAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(attackAnim);
                return PlayState.CONTINUE;
            }
        }
        if (dialogAnim != null) {
            if (event.getController().currentAnimationBuilder == dialogAnim && event.getController().getAnimationState() == AnimationState.Stopped) {
                dialogAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != dialogAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(dialogAnim);
                return PlayState.CONTINUE;
            }
        }
        if (manualAnim != null) {
            if (event.getController().currentAnimationBuilder == manualAnim && event.getController().getAnimationState() == AnimationState.Stopped) {
                manualAnim = null;
            } else {
                if (event.getController().currentAnimationBuilder != manualAnim) {
                    event.getController().markNeedsReload();
                }
                event.getController().setAnimation(manualAnim);
                return PlayState.CONTINUE;
            }
        }
        if (!event.isMoving() || walkAnimName.isEmpty()) {
            if (!idleAnimName.isEmpty()) {
                event.getController().setAnimation(new AnimationBuilder().loop(idleAnimName));
            } else {
                return PlayState.STOP;
            }
        } else {
            event.getController().setAnimation(new AnimationBuilder().loop(walkAnimName));
        }
        return PlayState.CONTINUE;
    }

    public void setDialogAnim(String name) {
        dialogAnim = new AnimationBuilder().playOnce(name);
    }

    private static int weightedRandomSelect(int[] weights, int count) {
        if (count <= 0) return -1;
        int total = 0;
        for (int i = 0; i < count; i++) total += Math.max(0, weights[i]);
        if (total <= 0) return -1;
        int r = RAND.nextInt(total);
        int cumsum = 0;
        for (int i = 0; i < count; i++) {
            cumsum += Math.max(0, weights[i]);
            if (r < cumsum) return i;
        }
        return count - 1;
    }

    public void resetAttackState() {
        currentAttackAnim = null;
        attackingTarget = null;
        currentAttackFrame = 0;
        attackAnimStartTick = 0;
        attackDamageDealt = false;
        frameAttackInProgress = false;
        delayedAttackPending = false;
        cachedDamage = 0;
        preAttackHealth = 0;
    }

    public void detectAnimationEvents() {
        if (npcRef == null || !world.isRemote) return;

        if (npcRef.deathTime <= 0 && npcRef.getHealth() > 0f && deathTriggered) {
            deathTriggered = false;
            deathAnim = null;
        }

        if (npcRef.deathTime > 0 && !deathTriggered && deathAnimCount > 0) {
            float hpPercent = npcRef.getHealth() / Math.max(npcRef.getMaxHealth(), 1f) * 100f;
            int[] eligibleWeights = new int[deathAnimCount];
            String[] eligibleNames = new String[deathAnimCount];
            int eligibleCount = 0;
            for (int i = 0; i < deathAnimCount; i++) {
                if (hpPercent <= deathHealthThresholds[i]) {
                    eligibleWeights[eligibleCount] = deathWeights[i];
                    eligibleNames[eligibleCount] = deathAnimNames[i];
                    eligibleCount++;
                }
            }
            if (eligibleCount > 0) {
                int idx = weightedRandomSelect(eligibleWeights, eligibleCount);
                if (idx >= 0 && !eligibleNames[idx].isEmpty()) {
                    deathAnim = new AnimationBuilder().playOnce(eligibleNames[idx]);
                    deathTriggered = true;
                }
            }
            wasSwinging = false;
            prevHurtTime = 0;
            return;
        }

        if (npcRef.hurtTime > 0 && prevHurtTime <= 0 && hurtAnim == null && hurtAnimCount > 0) {
            int idx = weightedRandomSelect(hurtWeights, hurtAnimCount);
            if (idx >= 0 && !hurtAnimNames[idx].isEmpty()) {
                hurtAnim = new AnimationBuilder().playOnce(hurtAnimNames[idx]);
            }
        }
        prevHurtTime = npcRef.hurtTime;

        boolean isSwinging = npcRef.swingProgress > 0.1f;
        if (isSwinging && !wasSwinging && attackAnim == null && manualAnim == null && attackCount > 0) {
            int idx = weightedRandomSelect(attackWeights, attackCount);
            if (idx >= 0 && !attackAnimNames[idx].isEmpty()) {
                attackAnim = new AnimationBuilder().playOnce(attackAnimNames[idx]);
            }
        }
        wasSwinging = isSwinging;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        detectAnimationEvents();
    }

    @Override
    public void tick() {}

    public EntityCustomModel(World worldIn) {
        super(worldIn);
        this.ignoreFrustumCheck = true;
        this.setSize(0.7F, 2F);
    }

    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "movement", 10, this::predicateMovement));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    public int tickTimer() {
        return ticksExisted;
    }

}
