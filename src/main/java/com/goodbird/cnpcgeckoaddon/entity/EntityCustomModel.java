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
    public int transitionLengthTicks = 10;
    public ItemStack leftHeldItem;

    public String[] attackAnimNames = new String[0];
    public int[] attackWeights = new int[0];
    public float[] attackFrames = new float[0];
    public String[] attackSoundNames = new String[0];
    public int attackCount = 0;

    public String[] hurtAnimNames = new String[0];
    public int[] hurtWeights = new int[0];
    public String[] hurtSoundNames = new String[0];
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
    public String currentAttackSound = null;

    private static final Random RAND = new Random();

    private void applyOneShot(AnimationController<?> controller, AnimationBuilder anim) {
        // Instant start for combat anims (no transition lag)
        controller.transitionLengthTicks = 0;
        if (controller.currentAnimationBuilder != anim) {
            controller.markNeedsReload();
        }
        controller.setAnimation(anim);
    }

    private void applyLoop(AnimationController<?> controller, AnimationBuilder anim, int transitionTicks) {
        controller.transitionLengthTicks = transitionTicks;
        controller.setAnimation(anim);
    }

    private <E extends IAnimatable> PlayState predicateMovement(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        if (deathAnim != null) {
            if (controller.currentAnimationBuilder == deathAnim && controller.getAnimationState() == AnimationState.Stopped) {
                return PlayState.STOP;
            }
            applyOneShot(controller, deathAnim);
            return PlayState.CONTINUE;
        }
        if (hurtAnim != null) {
            if (controller.currentAnimationBuilder == hurtAnim && controller.getAnimationState() == AnimationState.Stopped) {
                hurtAnim = null;
            } else {
                applyOneShot(controller, hurtAnim);
                return PlayState.CONTINUE;
            }
        }
        if (attackAnim != null) {
            if (controller.currentAnimationBuilder == attackAnim && controller.getAnimationState() == AnimationState.Stopped) {
                attackAnim = null;
            } else {
                applyOneShot(controller, attackAnim);
                return PlayState.CONTINUE;
            }
        }
        if (dialogAnim != null) {
            if (controller.currentAnimationBuilder == dialogAnim && controller.getAnimationState() == AnimationState.Stopped) {
                dialogAnim = null;
            } else {
                applyOneShot(controller, dialogAnim);
                return PlayState.CONTINUE;
            }
        }
        if (manualAnim != null) {
            if (controller.currentAnimationBuilder == manualAnim && controller.getAnimationState() == AnimationState.Stopped) {
                manualAnim = null;
            } else {
                applyOneShot(controller, manualAnim);
                return PlayState.CONTINUE;
            }
        }
        int transition = Math.max(0, transitionLengthTicks);
        if (!event.isMoving() || walkAnimName.isEmpty()) {
            if (!idleAnimName.isEmpty()) {
                applyLoop(controller, new AnimationBuilder().loop(idleAnimName), transition);
            } else {
                return PlayState.STOP;
            }
        } else {
            applyLoop(controller, new AnimationBuilder().loop(walkAnimName), transition);
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
        currentAttackSound = null;
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

        // Fallback client edge detect (server also syncs via PacketSyncAnimation asHurt)
        if (npcRef.hurtTime > prevHurtTime && hurtAnim == null && currentAttackAnim == null && hurtAnimCount > 0) {
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
