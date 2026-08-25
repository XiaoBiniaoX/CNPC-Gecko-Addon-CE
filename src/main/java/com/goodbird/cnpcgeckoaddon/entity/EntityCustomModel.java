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
import software.bernie.geckolib.core.animation.AnimationProcessor;
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

    // ========== 一次性动画优先级 ==========
    // 期望优先级：脚本播放 > 死亡 > 受伤 > 攻击 > 走路 > 待机。
    // 走路/待机（base 分支）天然最低，因为一次性动画分支在它之前 return。
    // 上面 4 种一次性动画共用 manualAnimName 这一个槽，所以必须显式记录优先级来仲裁，
    // 否则谁最后写谁生效（旧行为：攻击能覆盖脚本动画，受伤反被攻击挡住）。
    /** 无一次性动画。 */
    public static final int PRIO_NONE = 0;
    /** 攻击动画：最低，可被任何其他一次性动画打断。 */
    public static final int PRIO_ATTACK = 1;
    /** 受伤动画。 */
    public static final int PRIO_HURT = 2;
    /** 死亡动画。 */
    public static final int PRIO_DEATH = 3;
    /** 脚本主动播放：最高，不会被任何自动动画打断。 */
    public static final int PRIO_SCRIPT = 4;
    /** 当前一次性动画的优先级，无动画时为 PRIO_NONE。 */
    public int manualAnimPriority = PRIO_NONE;
    // ---- 被拒动画的补播槽 ----
    // CNPC 受伤时把 hurtTime 拉到 100 tick（EntityNPCInterface:769-771，原版只有 10），
    // 而近战冷却默认只有 20 tick（EntityAIAttackTarget:145，attackSpeed=20）。
    // 于是「受伤动画还在播 → 攻击请求到达 → 因优先级更低被拒」是常态而非例外，
    // 被拒的一次性请求若直接丢弃，这一次攻击动画就永久消失
    // （用户实测：走路中被打一下，下一次攻击动画 100% 不播）。
    // 优先级语义不变（受伤仍然压住攻击），只是把被拒者记下来，等前者播完立刻补上。
    // 只留一个槽、后来者覆盖：攻击动画积压两个以上没有观感意义，补最新的才对。
    private String pendingAnimName = null;
    private boolean pendingAnimInstant = false;
    private int pendingAnimPriority = PRIO_NONE;
    /** 补播槽写入时的 tick，用于判定是否已过时。 */
    private int pendingAnimTick = 0;
    /**
     * 补播槽保质期。受伤动画实测约 50 tick，取 60 tick（3 秒）够它播完还有余量；
     * 再久说明这个攻击请求早就不合时宜了（NPC 可能已脱战/换目标），补出来是鬼畜。
     */
    private static final int PENDING_ANIM_TTL = 60;
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
    // Push/squeeze fix: require a stronger, longer-lasting limb swing before switching
    // idle <-> walk, so being shoved by other entities does not restart the animation.
    private static final int STABLE_THRESHOLD = 6;
    private static final float LIMB_SWING_THRESHOLD = 0.25F;
    // 一次性动画卡死看门狗：**必须按动画时间（tick）计，不能按渲染帧数计**。
    // 病史（2026-08-25 实测抓到）：旧实现 manualAnimTicks 每渲染一帧 +1，阈值 400。
    // 但 predicateMovement 是渲染回调，帧率越高它涨得越快，而动画进度走的是 tick 时间。
    // 朋友端实测 5.71 帧/tick，于是 400 帧只等于约 70 tick（3.5 秒）——
    // 任何超过 70 tick 的动画都会在播完前被看门狗砍掉：
    //   PLAY anim=a3 held=400 adjusted=64.8 len=110.0  ← 才播到 59%
    //   KILL-BY-WATCHDOG anim=a3 heldTicks=401         ← 下一帧就被杀
    // 这就是「长动画抬手一半直接断掉回 idle」的真凶，也解释了为何 a1(35t)/jump(55t) 正常
    // 而 a3(110t)/s1(135t) 必卡，以及为何帧率越高越容易复现。
    // 现在改为按 animTime 计：40 秒 = 800 tick（哈基彬指定），给超长动画留足余量。
    private static final int MANUAL_ANIM_TIMEOUT_TICKS = 800;
    private String manualAnimWatchName = null;
    /** 本次一次性动画开始时的 animTime，看门狗据此按动画时间计时（不受帧率影响）。 */
    private double manualAnimStartAnimTime = 0.0;
    /** 诊断心跳去重用（同一 animTick 只打一条）。 */
    private int gecko$lastHeartbeatTick = -1;
    /**
     * Set only when the animation config actually changed (see MixinEntityCustomNpc).
     * The next animation tick drops any stale override and re-applies the base animation.
     * Must never be triggered by routine data syncs, or the base loop restarts and flickers.
     */
    public boolean needsAnimResync = false;
    /** Signature of the last applied animation config, used to detect real changes. */
    public String animConfigSignature = null;

    public void requestAnimResync() {
        this.needsAnimResync = true;
    }

    // ==================== 每实体独立动画时间轴 ====================
    // GeoModel 的 animTime / lastGameTickTime 是实例字段，而 ModelCustom 全局只有一个实例
    // （RenderCustomModel 构造时 new ModelCustom()），所以所有 Gecko NPC 本来共用同一条时间轴。
    // 而 getTick() 返回的是各自宿主的 owner.tickCount（跨模型实体重建保持稳定，不能改）。
    // 两者叠加的后果：NPC A 把 animTime 推到自己的 tickCount，NPC B 渲染时又按自己的
    // tickCount 拉回去，animTime 在多个 NPC 之间反复跳变、失去单调性。
    // seekTime 一旦不单调，AnimationController 的 adjustedTick 就会乱跳，
    // 一次性长动画极易撞上 adjustedTick >= length() 被判「播完」而中途消失
    // （越长的动画窗口越大、越容易中招；玩家离远后渲染频率下降、交错模式被打乱，
    //  正是「离远 + 10 秒以上动画」这个复现条件的由来）。
    // 解法：每个模型实体自己存一份，由 ModelCustom.handleAnimations 在调用前后换入/换出。
    /** 本实体自己的 animTime。NaN 表示尚未初始化（首次渲染时以 GeoModel 现值为起点）。 */
    public double ownAnimTime = Double.NaN;
    /** 本实体自己的 lastGameTickTime。 */
    public double ownLastGameTickTime = Double.NaN;

    // 注：曾在此加过「一次性动画未播完不许同级覆盖」的保护（manualAnimEndsAt），
    // 实测证明是错的，已删除。它要修的「视野外空窗被覆盖」并不存在（END 全 done=true），
    // 却带来实锤副作用：飞天大草（jump）落地接平A 时，平A 被拦进补播槽，
    // 要等 jump 剩余 2~12 tick 播完才播出，观感就是「平A 有一点延迟」。
    // 长动画被卡的真凶是看门狗按帧数计时（见 MANUAL_ANIM_TIMEOUT_TICKS 注释），与此无关。
    // 结论：同优先级必须允许立刻覆盖，这是技能连按/连续攻击手感的基础。

    /** GUI 配置的过渡帧数，取不到时用 GeckoLib 构造时的默认值 10。 */
    private int configuredTransitionTicks() {
        if (owner == null) return 10;
        return ((IDataDisplay) owner.display).getCustomModelData().getTransitionLengthTicks();
    }

    /**
     * 按优先级申请播放一次性动画。优先级低于当前正在播放的动画时直接拒绝，
     * 这样攻击动画就不会打断脚本动画（旧行为是无条件覆盖）。
     * 同优先级允许覆盖（例如连续两次攻击、脚本连续播两个动画）。
     *
     * @return true 表示已接受并设置
     */
    public boolean requestManualAnim(String animName, boolean instant, int priority) {
        if (animName == null || animName.isEmpty()) return false;
        // 服务端不参与渲染，predicateMovement 只在客户端的动画回调里跑，
        // 所以服务端写下的 manualAnimName / manualAnimPriority 永远没人清除，
        // 会变成一把「只进不出」的永久闩锁：第一次受伤（prio=2）之后，
        // 之后每一次攻击（prio=1）都被一个早就播完的受伤动画拒绝。
        // 实测日志中 16 次 REJECT 的 heldTicks 全是 0，正是这个症状。
        // 仲裁只在客户端做；服务端只负责发包（发包在调用方，不依赖本方法的返回值）。
        if (!level().isClientSide) return false;
        // 已有更高优先级动画在播放：拒绝当次抢镜，但记进补播槽，等它播完再放。
        // manualAnimName 为 null 时视为无动画（优先级归零兜底）。
        if (manualAnimName != null && priority < manualAnimPriority) {
            this.pendingAnimName = animName;
            this.pendingAnimInstant = instant;
            this.pendingAnimPriority = priority;
            this.pendingAnimTick = tickCount;
            return false;
        }

        // 被新请求接管：旧的补播槽作废，否则它会在新动画播完后突然冒出来抢镜。
        this.pendingAnimName = null;
        this.pendingAnimPriority = PRIO_NONE;
        this.manualAnimName = animName;
        this.manualAnimInstant = instant;
        this.manualAnimPriority = priority;
        // 强制重建：同名动画再次请求时（连续两次同一个攻击/受伤动画），
        // 若沿用旧的 currentOverrideAnim，predicateMovement 会命中
        // manualAnimName.equals(currentOverrideAnim) 而跳过 forceAnimationReset，
        // setAnimation 又因 RawAnimation 相等提前返回，结果动画停在播完的最后一帧不重播。
        // 实测 43 次 ACCEPT 只换来 26 次起播，差的 17 次全是同名重复请求。
        this.currentOverrideAnim = "";
        this.manualAnimRaw = null;
        return true;
    }

    /**
     * 清除一次性动画状态。
     * 必须同时清空 currentOverrideAnim：否则「同名动画连播」会被守卫误吞——
     * 上一次播完清了 manualAnimName 但 currentOverrideAnim 仍留着同一个名字，
     * 新请求命中 manualAnimName.equals(currentOverrideAnim) 就不会 forceAnimationReset，
     * 控制器仍停在 STOPPED，下一帧守卫立刻又把它清掉（技能连按第二下没反应）。
     */
    private void clearManualAnim() {
        this.manualAnimName = null;
        this.manualAnimRaw = null;
        this.manualAnimPriority = PRIO_NONE;
        this.currentOverrideAnim = "";
        this.manualAnimWatchName = null;
        this.manualAnimStartAnimTime = ownAnimTime;
    }

    /**
     * 高优先级动画播完后，补播先前被拒的那个（若有）。
     * 必须在 clearManualAnim() 之后调用：此时 manualAnimPriority 已归零，
     * requestManualAnim 不会再把它拒掉。
     * 补播槽用完即清，避免同一个请求被放两次。
     */
    private void consumePendingAnim() {
        if (pendingAnimName == null) return;
        String name = pendingAnimName;
        boolean instant = pendingAnimInstant;
        int priority = pendingAnimPriority;
        int age = tickCount - pendingAnimTick;
        pendingAnimName = null;
        pendingAnimPriority = PRIO_NONE;
        // 过时请求直接丢：NPC 可能已脱战或换了目标，此时补一个旧攻击动画反而突兀。
        if (age < 0 || age > PENDING_ANIM_TTL) return;
        requestManualAnim(name, instant, priority);
    }

    private PlayState predicateMovement(AnimationState<EntityCustomModel> event) {
        AnimationController<?> controller = event.getController();
        if (needsAnimResync) {
            // A reset happened while this model entity was kept alive: throw away every
            // cached override/base state so idle/walk is re-applied from scratch.
            needsAnimResync = false;
            clearManualAnim();
            // 配置变更/重建：补播槽里的动画可能已不存在于新动画文件，一并丢弃。
            pendingAnimName = null;
            pendingAnimPriority = PRIO_NONE;
            dialogAnimName = null;
            dialogAnimRaw = null;
            currentBaseAnimName = "";
            hurtAnimationPlaying = false;
            deathAnimationPlaying = false;
            stableTicks = 0;
            controller.forceAnimationReset();
        }
        if (manualAnimName != null) {
            // Watchdog: a one-shot override must never hold the controller forever.
            // A stale name (e.g. left over from before a config change, or an animation
            // that no longer exists in the current file) would otherwise block idle/walk.
            if (!manualAnimName.equals(manualAnimWatchName)) {
                manualAnimWatchName = manualAnimName;
                // 记下起始 animTime；用动画时间计时，与帧率无关。
                manualAnimStartAnimTime = ownAnimTime;
            } else if (ownAnimTime - manualAnimStartAnimTime > MANUAL_ANIM_TIMEOUT_TICKS) {
                clearManualAnim();
                // 卡死 40 秒才走到这里，补播槽里的请求早已过时，丢弃而不是补播。
                pendingAnimName = null;
                pendingAnimPriority = PRIO_NONE;
                hurtAnimationPlaying = false;
                deathAnimationPlaying = false;
            }
        } else {
            manualAnimWatchName = null;
            manualAnimStartAnimTime = ownAnimTime;
        }
        if (manualAnimName != null) {
            // 只有「本动画自己播完」才清除。旧代码只判 STOPPED，任何原因造成的 STOPPED
            // （base 分支返回 PlayState.STOP、动画名不存在导致 buildAnimationQueue 失败、
            // 过渡期队列饿死）都会把刚设上、一帧未播的动画直接吞掉——这正是
            // 「NPC 转身时脚本动画被走路吞掉」的病灶：走路迟滞未稳定时 base 分支
            // 返回 STOP 让控制器进 STOPPED，此时到达的脚本动画当场蒸发。
            // 三重确认：控制器停了 + 停的正是本动画（名字与 raw 实例都对得上）。
            boolean playedOut = controller.getAnimationState() == AnimationController.State.STOPPED
                    && manualAnimRaw != null
                    && manualAnimName.equals(currentOverrideAnim)
                    && controller.getCurrentRawAnimation() == manualAnimRaw;
            if (playedOut) {
                clearManualAnim();
                // 正常播完才补播被拒的动画。看门狗超时/动画名错误/resync 都不补：
                // 那几种是异常收尾，此时再塞一个动画只会掩盖问题。
                consumePendingAnim();
            } else {
                boolean justStarted = false;
                if (manualAnimRaw == null || !manualAnimName.equals(currentOverrideAnim)) {
                    manualAnimRaw = RawAnimation.begin().thenPlay(manualAnimName);
                    currentOverrideAnim = manualAnimName;
                    controller.forceAnimationReset();
                    justStarted = true;
                }
                // 只在「本次起播」那一帧用 0 过渡消除 0.5 秒延后；播放期间绝不再碰
                // transitionLength —— 这是「长动画抬手一半直接断掉回 idle」的真凶：
                // AnimationController:489-491
                //   if (transitionLength == 0.0 && shouldResetTick && state == TRANSITIONING)
                //       currentAnimation = animationQueue.poll();
                // thenPlay 单段动画的队列只有 1 个元素、起播时已被取走，队列此时是空的，
                // 于是 currentAnimation 被置成 null → 下一帧 :275 的
                // (currentAnimation != null || !queue.isEmpty()) 两者皆不成立 → :366 置 STOPPED
                // → 我们的 playedOut 三重判定成立 → clearManualAnim() → 动画中途蒸发。
                // 官方版全程不改 transitionLength（恒为构造值 10），所以原版不犯这个病。
                if (justStarted) {
                    controller.transitionLength(0);
                    controller.setAnimation(manualAnimRaw);
                    controller.transitionLength(configuredTransitionTicks());
                } else {
                    controller.setAnimation(manualAnimRaw);
                }
                // 动画名在当前动画文件里不存在时，GeckoLib 的 setAnimation 内部
                // buildAnimationQueue 返回 null 并直接 stop()，currentRawAnimation 不会被改写。
                // 此时若继续占着控制器，模型会一直冻结到 40 秒看门狗超时；
                // 立刻放弃、回落到走路/待机（与旧版对错误动画名的表现一致）。
                if (controller.getAnimationState() == AnimationController.State.STOPPED
                        && controller.getCurrentRawAnimation() != manualAnimRaw) {
                    clearManualAnim();
                    // 动画名错误导致的放弃：这里补播是安全的，被拒的那个动画名可能是好的。
                    // （与看门狗超时不同，这条是「立刻失败」，请求还很新鲜。）
                    consumePendingAnim();
                } else {
                    if (controller.getAnimationState() == AnimationController.State.TRANSITIONING &&
                            !((AnimControllerAccessor) controller).getJustStartedTransition() && manualAnimInstant) {
                        ((AnimControllerAccessor) controller).setAnimationState(AnimationController.State.RUNNING);
                    }
                    return PlayState.CONTINUE;
                }
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
        float limb = event.getLimbSwingAmount();
        boolean isMoving = !(limb > -LIMB_SWING_THRESHOLD && limb < LIMB_SWING_THRESHOLD) && !walkAnim.isEmpty();
        if (isMoving == wasMoving) {
            if (stableTicks < 20) stableTicks++;
        } else {
            stableTicks = 0;
            wasMoving = isMoving;
        }

        String targetAnim;
        // currentBaseAnimName 为空时不要再「保持现状」——现状是「什么都没播」，
        // 保持它会 return PlayState.STOP 让控制器进 STOPPED（模型实体刚重建、
        // 刚 resync 后都会出现这个持续 6 tick 的空窗），窗口内到达的一次性动画
        // 极易受连带影响。此时直接采用真实目标动画，迟滞只在「已有动画可保持」时才生效，
        // 因此不会削弱 idle/walk 的抗抖动能力。
        if (stableTicks >= STABLE_THRESHOLD || currentBaseAnimName.isEmpty()) {
            targetAnim = isMoving ? walkAnim : idleAnim;
        } else {
            targetAnim = currentBaseAnimName;
        }

        if (targetAnim == null || targetAnim.isEmpty()) {
            currentBaseAnimName = "";
            return PlayState.STOP;
        }

        // B: Only switch on actual change or when the controller is stopped.
        // Deliberately do NOT force a re-apply just because the controller currently plays
        // a different animation (e.g. right after a hurt one-shot): doing that restarts the
        // base loop from frame 0 every time and causes the visible flicker.
        boolean animChanged = !targetAnim.equals(currentBaseAnimName);
        boolean stopped = controller.getAnimationState() == AnimationController.State.STOPPED;
        // 过渡期队列饿死自救（「动画断掉后定格在 idle/walk 第一帧」的病灶）：
        // 一次性动画播完回落 base 时走的是 animChanged 分支，此时 transitionLength 已被
        // 恢复成配置值（默认 10），setAnimation 让控制器进 TRANSITIONING。
        // 若 AnimationController:296 的 poll 条件
        //   lastPollTime != seekTime && (adjustedTick == 0.0 || isJustStarting)
        // 当帧不满足，currentAnimation 会停在 null，:337 直接 return —— 一根骨头都不动。
        // 而下一帧 animChanged=false、stopped=false（状态是 TRANSITIONING 而非 STOPPED），
        // 上面两个条件都进不去、不会再 setAnimation → 自持卡死，模型定格在第 0 帧。
        // 这里补一条：TRANSITIONING 且队列已空、当前动画为 null，就重新起播。
        boolean starved = controller.getAnimationState() == AnimationController.State.TRANSITIONING
                && controller.getCurrentAnimation() == null
                && ((AnimControllerAccessor) controller).gecko$getAnimationQueue().isEmpty();

        if (animChanged || stopped || starved) {
            currentBaseAnimName = targetAnim;
            if (stopped || starved) {
                // 饿死自救也要强制重建：否则 setAnimation 会因 RawAnimation 相等提前返回，
                // 队列不会被重新填充，卡死状态维持原样。
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
        // 攻击动画优先级最低：脚本/死亡/受伤动画正在播时不抢镜。
        // 单机时本方法在集成服务端线程跑，requestManualAnim 会因非客户端而直接返回，
        // 实际播放由下面的发包驱动客户端完成。
        requestManualAnim(animName, false, PRIO_ATTACK);
        // 发包始终执行，不依赖 requestManualAnim 的返回值：优先级仲裁只在客户端做
        // （PacketSyncAnimation.handle → requestManualAnim）。
        if (!level().isClientSide && owner != null) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(owner.getId(), animName, false, PRIO_ATTACK));
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
        // 死亡动画高于受伤/攻击，但低于脚本播放。
        requestManualAnim(deathAnimName, false, PRIO_DEATH);
        this.deathAnimationPlaying = true;
        if (!level().isClientSide && owner != null) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(owner.getId(), deathAnimName, false, PRIO_DEATH));
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
        // 原先这里有 `if (currentAttackAnim != null) return;`——攻击动画会挡住受伤动画，
        // 与约定的「受伤 > 攻击」正好相反，现由优先级仲裁取代。
        // 注意：这不影响帧延时伤害结算，那套逻辑在服务端按 tickCount 计时
        // （MixinEntityCustomNpc.tick），与播放哪个动画无关。
        requestManualAnim(hurtAnimName, true, PRIO_HURT);
        if (!level().isClientSide && owner != null) {
            NetworkWrapper.sendToAll(new PacketSyncAnimation(owner.getId(), hurtAnimName, true, PRIO_HURT));
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