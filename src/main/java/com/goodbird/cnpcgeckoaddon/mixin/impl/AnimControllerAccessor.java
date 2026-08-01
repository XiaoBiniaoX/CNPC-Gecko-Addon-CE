package com.goodbird.cnpcgeckoaddon.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.data.KeyFrameData;

import java.util.Queue;
import java.util.Set;

@Mixin(value = AnimationController.class, remap = false)
public interface AnimControllerAccessor {

    @Accessor
    void setAnimationState(AnimationController.State state);

    @Accessor
    boolean getJustStartedTransition();

    @Accessor
    AnimationController.State getAnimationState();

    @Accessor("animationQueue")
    Queue<AnimationProcessor.QueuedAnimation> gecko$getAnimationQueue();

    @Accessor("animationQueue")
    void gecko$setAnimationQueue(Queue<AnimationProcessor.QueuedAnimation> queue);

    @Accessor("currentRawAnimation")
    RawAnimation gecko$getCurrentRawAnimation();

    @Accessor("currentRawAnimation")
    void gecko$setCurrentRawAnimation(RawAnimation animation);

    @Accessor("currentAnimation")
    AnimationProcessor.QueuedAnimation gecko$getCurrentAnimation();

    @Accessor("currentAnimation")
    void gecko$setCurrentAnimation(AnimationProcessor.QueuedAnimation animation);

    @Accessor("tickOffset")
    double gecko$getTickOffset();

    @Accessor("tickOffset")
    void gecko$setTickOffset(double tickOffset);

    @Accessor("transitionLength")
    double gecko$getTransitionLength();

    @Accessor("transitionLength")
    void gecko$setTransitionLength(double transitionLength);

    @Accessor("lastPollTime")
    double gecko$getLastPollTime();

    @Accessor("lastPollTime")
    void gecko$setLastPollTime(double lastPollTime);

    @Accessor("isJustStarting")
    boolean gecko$isJustStarting();

    @Accessor("isJustStarting")
    void gecko$setJustStarting(boolean justStarting);

    @Accessor("needsAnimationReload")
    boolean gecko$needsAnimationReload();

    @Accessor("needsAnimationReload")
    void gecko$setNeedsAnimationReload(boolean needsAnimationReload);

    @Accessor("shouldResetTick")
    void gecko$setShouldResetTick(boolean shouldResetTick);

    @Accessor("justStopped")
    boolean gecko$justStopped();

    @Accessor("justStopped")
    void gecko$setJustStopped(boolean justStopped);

    @Accessor("justStartedTransition")
    void gecko$setJustStartedTransition(boolean justStartedTransition);

    @Accessor("executedKeyFrames")
    Set<KeyFrameData> gecko$getExecutedKeyFrames();
}
