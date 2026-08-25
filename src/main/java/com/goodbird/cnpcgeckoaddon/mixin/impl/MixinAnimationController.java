package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Map;

/**
 * 修「渲染中断后长动画被跳帧判完」。
 *
 * 病因（GeckoLib 4.8.3 反编译实证）：
 * 动画时间轴只在被渲染的那一帧推进 —— 唯一入口是
 * GeoEntityRenderer.render → GeoModel.handleAnimations → AnimationProcessor.tickAnimation
 * → AnimationController.process，没有任何 tick 事件兜底。
 * 而 EntityCustomModel.getTick() 返回宿主 NPC 的 tickCount，只要 NPC 还在追踪范围内
 * 就每 tick 递增，**与是否渲染无关**。两者一解耦就出事：
 *   1. GeoModel:151  animTime += (lastUpdateTime - lastGameTickTime)，中断 200 tick 就一次性 +200
 *   2. AnimationController:517  adjustedTick = speed * (seekTime - tickOffset)，tickOffset 中断期间冻结
 *   3. AnimationController:372  adjustedTick >= animation.length() 当场成立
 *   4. AnimationController:383  一次性动画（thenPlay，loopType 返回 false）立刻置 STOPPED 并 return，
 *      连最后一帧姿态都不写入，骨骼按 getBoneResetTime() 归位 —— 这就是「动画被卡掉」
 * 全链路没有任何 delta 上限钳制。
 *
 * 与复现条件完全吻合：动画越长（10 秒往上）越容易被一次跳跃跨过整条；玩家离远导致宿主
 * NPC 停止渲染；脚本触发或 AI 触发表现一致，因为病灶与触发源无关。
 *
 * 修法：在 process 的 HEAD 注入 —— 这是唯一能在 adjustTick(第 266 行) 之前介入的位置，
 * 若放在 predicate 里（第 274 行才调）本帧的判定已经用旧值算完了。
 * 检测本帧 seekTime 相对上一帧的跳跃量，超阈值就把 tickOffset 同量前推，
 * 使 adjustedTick 保持连续 —— 动画从中断处接着播完，而不是被判完。
 *
 * 只对自家 EntityCustomModel 生效，其它 mod 的 GeoAnimatable 一律放行。
 */
@Mixin(value = AnimationController.class, remap = false)
public abstract class MixinAnimationController<T extends GeoAnimatable> {

    @Shadow
    @Final
    protected T animatable;

    @Shadow
    protected double tickOffset;

    /** 上一帧的 seekTime；-1 表示尚无基准（首帧）。 */
    @Unique
    private double gecko$lastSeekTime = -1.0;

    /**
     * 容许的单帧跳跃上限。正常渲染下相邻两帧 seekTime 只差 1 tick 上下，
     * 取 3 tick 留出卡顿与低帧率余量，超过即判定为「渲染中断过」。
     */
    @Unique
    private static final double GECKO_MAX_FRAME_DELTA = 3.0;

    @Inject(method = "process", at = @At("HEAD"))
    private void gecko$compensateRenderGap(CoreGeoModel<T> model, AnimationState<T> state,
                                           Map<String, CoreGeoBone> bones,
                                           Map<String, BoneSnapshot> snapshots,
                                           double seekTime, boolean crashWhenCantFindBone,
                                           CallbackInfo ci) {
        // 只管自家模型实体：别的 mod 的动画时间轴自有其语义，不插手。
        if (!(this.animatable instanceof EntityCustomModel)) return;
        double previous = this.gecko$lastSeekTime;
        this.gecko$lastSeekTime = seekTime;
        if (previous < 0.0) return;
        double delta = seekTime - previous;
        // 时间倒流：模型实体被 clearEntity 重建后 owner 换了实例、tickCount 换源时会出现
        // （跨 NPC 共享时间轴那一份污染已由 ModelCustom 的每实体时间轴隔离修掉）。
        // 以本帧为新基准，避免 adjustedTick 出现负增量导致姿态倒退抽搐。
        if (delta < 0.0) {
            this.tickOffset = seekTime;
            return;
        }
        if (delta <= GECKO_MAX_FRAME_DELTA) return;
        // 渲染中断了 delta 个 tick：基准同量前推，抵消这次跳跃。
        // 语义等价于「中断期间动画暂停」，恢复渲染后从中断处继续播。
        this.tickOffset += delta;
    }
}
