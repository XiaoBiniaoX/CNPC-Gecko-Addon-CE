package com.goodbird.cnpcgeckoaddon.entity;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.mixin.impl.AnimControllerAccessor;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketInstructionKeyframe;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import noppes.npcs.entity.EntityNPCInterface;
import software.bernie.geckolib.GeckoLib;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EntityCustomModel extends Animal implements GeoAnimatable, GeoEntity {
    private AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    public ResourceLocation modelResLoc=new ResourceLocation(CNPCGeckoAddon.MODID, "geo/geo_npc.geo.json");
    public ResourceLocation animResLoc=new ResourceLocation(CNPCGeckoAddon.MODID , "animations/geo_npc.animation.json");
    public ResourceLocation textureResLoc = new ResourceLocation("customnpcs","textures/entity/humanmale/steve.png");
    public String idleAnim = "";
    public String walkAnim = "";
    public String hurtAnim = "";
    public String attackAnim = "";
    public String manualAnimName = null;
    public String dialogAnimName = null;
    public boolean manualAnimInstant = false;
    private RawAnimation manualAnimRaw = null;
    private RawAnimation dialogAnimRaw = null;
    private String currentOverrideAnim = "";
    public ItemStack leftHeldItem;
    public String headBoneName = "head";
    private EntityDimensions dims;
    public float size = 5.0f;
    public EntityNPCInterface owner;
    public static final int MAX_ATTACKS = 20;
    public String[] attackAnimNames = new String[MAX_ATTACKS];
    public int[] attackWeights = new int[MAX_ATTACKS];
    public float[] attackFrames = new float[MAX_ATTACKS];
    public String[] attackSoundNames = new String[MAX_ATTACKS];
    public int attackCount = 5;
    public Entity attackingTarget = null;
    public String currentAttackAnim = null;
    public float currentAttackFrame = 0;
    public int attackAnimStartTick = 0;
    public boolean attackDamageDealt = false;
    public boolean frameAttackInProgress = false;
    public boolean hurtAnimationPlaying = false;
    public String currentAttackSound = null;

    public static final int MAX_HURTS = 20;
    public String[] hurtAnimNames = new String[MAX_HURTS];
    public int[] hurtWeights = new int[MAX_HURTS];
    public String[] hurtSoundNames = new String[MAX_HURTS];
    public int hurtAnimCount = 5;
    public static final int MAX_DEATHS = 20;
    public String[] deathAnimNames = new String[MAX_DEATHS];
    public int[] deathWeights = new int[MAX_DEATHS];
    public float[] deathHealthThresholds = new float[MAX_DEATHS];
    public float[] deathAnimDurations = new float[MAX_DEATHS];
    public int deathAnimCount = 5;
    public boolean deathAnimationPlaying = false;

    // B fix: track current base animation to avoid unnecessary setAnimation calls
    private String currentBaseAnimName = "";
    // C fix: hysteresis for idle/walk switching
    private int stableTicks = 0;
    private boolean wasMoving = false;
    private static final int STABLE_THRESHOLD = 3;

    private PlayState predicateMovement(AnimationState<EntityCustomModel> event) {
        AnimationController<?> controller = event.getController();
        if (manualAnimName != null) {
            if (controller.getAnimationState() == AnimationController.State.STOPPED) {
                manualAnimName = null;
            } else {
                if (manualAnimRaw == null || !manualAnimName.equals(currentOverrideAnim)) {
                    manualAnimRaw = RawAnimation.begin().thenPlay(manualAnimName);
                    currentOverrideAnim = manualAnimName;
                    controller.forceAnimationReset();
                }
                controller.setAnimation(manualAnimRaw);
                if (controller.getAnimationState() == AnimationController.State.TRANSITIONING &&
                        !((AnimControllerAccessor) controller).getJustStartedTransition() && manualAnimInstant) {
                    ((AnimControllerAccessor) controller).setAnimationState(AnimationController.State.RUNNING);
                }
                return PlayState.CONTINUE;
            }
        }
        if (dialogAnimName != null) {
            if (controller.getAnimationState() == AnimationController.State.STOPPED) {
                dialogAnimName = null;
            } else {
                if (dialogAnimRaw == null || !dialogAnimName.equals(currentOverrideAnim)) {
                    dialogAnimRaw = RawAnimation.begin().thenPlay(dialogAnimName);
                    currentOverrideAnim = dialogAnimName;
                    controller.forceAnimationReset();
                }
                controller.setAnimation(dialogAnimRaw);
                return PlayState.CONTINUE;
            }
        }
        // C: Debounce movement detection
        boolean isMoving = !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F) && !walkAnim.isEmpty();
        if (isMoving == wasMoving) {
            if (stableTicks < 20) stableTicks++;
        } else {
            stableTicks = 0;
            wasMoving = isMoving;
        }

        String targetAnim;
        if (stableTicks >= STABLE_THRESHOLD) {
            targetAnim = isMoving ? walkAnim : idleAnim;
        } else {
            targetAnim = currentBaseAnimName;
        }

        if (targetAnim == null || targetAnim.isEmpty()) {
            currentBaseAnimName = "";
            return PlayState.STOP;
        }

        // B: Only switch on actual change or when controller is stopped
        boolean animChanged = !targetAnim.equals(currentBaseAnimName);
        boolean stopped = controller.getAnimationState() == AnimationController.State.STOPPED;

        if (animChanged || stopped) {
            currentBaseAnimName = targetAnim;
            if (stopped) {
                controller.forceAnimationReset();
            }
            // Use 0 transition for base anim to avoid GeckoLib queue starvation
            int configuredTransition = 10;
            if (owner != null) {
                configuredTransition = ((IDataDisplay) owner.display).getCustomModelData().getTransitionLengthTicks();
            }
            controller.transitionLength(0);
            controller.setAnimation(RawAnimation.begin().thenLoop(targetAnim));
            controller.transitionLength(configuredTransition);
        }

        return PlayState.CONTINUE;
    }

    {
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

    public EntityCustomModel(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
        this.noCulling = true;
    }

    public void setSize(float width, float height) {
        dims = EntityDimensions.scalable(width, height);
    }

    @Override
    public EntityDimensions getDimensions(Pose p_213305_1_) {
        if(dims==null){
            dims = EntityDimensions.scalable(0.7F, 2F);
        }
        return dims;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 10, this::predicateMovement).setCustomInstructionKeyframeHandler(this::handleCustomInstruction));
    }

    public void handleCustomInstruction(CustomInstructionKeyframeEvent<EntityCustomModel> event) {
        NetworkWrapper.sendToServer(new PacketInstructionKeyframe(owner.getId(), event.getKeyframeData().getInstructions()));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    @Override
    public double getTick(Object entity) {
        if (owner != null) return owner.tickCount;
        return tickCount;
    }

    public void startAttackAnimation(String animName, float frameThreshold, Entity target) {
        this.currentAttackAnim = animName;
        this.currentAttackFrame = frameThreshold;
        this.attackingTarget = target;
        this.attackDamageDealt = false;
        this.currentAttackSound = null;
        for (int i = 0; i < attackCount; i++) {
            if (animName.equals(attackAnimNames[i])) {
                this.currentAttackSound = attackSoundNames[i];
                break;
            }
        }
        this.manualAnimName = animName;
        this.manualAnimInstant = false;
        if (!level().isClientSide && owner != null) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(owner.getId(), animName, false));
        }
    }

    public void resetAttackState() {
        currentAttackAnim = null;
        attackingTarget = null;
        currentAttackFrame = 0;
        attackAnimStartTick = 0;
        attackDamageDealt = false;
        frameAttackInProgress = false;
        currentAttackSound = null;
    }

    public void playDeathAnimation(String deathAnimName) {
        if (deathAnimName == null || deathAnimName.isEmpty()) return;
        this.manualAnimName = deathAnimName;
        this.manualAnimInstant = false;
        this.deathAnimationPlaying = true;
        if (!level().isClientSide && owner != null) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(owner.getId(), deathAnimName, false));
        }
    }

    public String pickWeightedDeathAnim() {
        int totalWeight = 0;
        int validCount = 0;
        for (int i = 0; i < deathAnimCount; i++) {
            if (deathAnimNames[i] != null && !deathAnimNames[i].isEmpty()) {
                totalWeight += Math.max(deathWeights[i], 0);
                validCount++;
            }
        }
        if (validCount == 0 || totalWeight <= 0 || owner == null) return null;
        int rand = owner.getRandom().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < deathAnimCount; i++) {
            if (deathAnimNames[i] != null && !deathAnimNames[i].isEmpty()) {
                cumulative += Math.max(deathWeights[i], 0);
                if (rand < cumulative) return deathAnimNames[i];
            }
        }
        return null;
    }

    public void playHurtAnimation(String hurtAnimName) {
        if (hurtAnimName == null || hurtAnimName.isEmpty()) return;
        if (this.currentAttackAnim != null) return;
        this.manualAnimName = hurtAnimName;
        this.manualAnimInstant = true;
        if (!level().isClientSide && owner != null) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(owner.getId(), hurtAnimName, true));
        }
    }

    public String pickWeightedHurtAnim() {
        int totalWeight = 0;
        int validCount = 0;
        for (int i = 0; i < hurtAnimCount; i++) {
            if (hurtAnimNames[i] != null && !hurtAnimNames[i].isEmpty()) {
                totalWeight += Math.max(hurtWeights[i], 0);
                validCount++;
            }
        }
        if (validCount == 0 || totalWeight <= 0 || owner == null) return null;
        int rand = owner.getRandom().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < hurtAnimCount; i++) {
            if (hurtAnimNames[i] != null && !hurtAnimNames[i].isEmpty()) {
                cumulative += Math.max(hurtWeights[i], 0);
                if (rand < cumulative) return hurtAnimNames[i];
            }
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    public double getAttributeValue(Attribute p_233637_1_) {
        try {
            return this.getAttributes().getValue(p_233637_1_);
        }catch (Exception e){
            return 1.0;
        }
    }
}