package com.goodbird.cnpcgeckoaddon.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.model.GeoModel;

@Mixin(value = GeoModel.class, remap = false)
public interface GeoModelAccessor {

    @Accessor
    void setLastRenderedInstance(long value);

    // ---- 每实体独立时间轴所需的访问器 ----
    // GeoModel 的 animTime / lastGameTickTime 是实例字段，而 ModelCustom 全局只有一个实例
    // （RenderCustomModel 构造时 new 一次），于是所有 Gecko NPC 共用同一条时间轴；
    // 又因为 EntityCustomModel.getTick() 返回各自宿主的 owner.tickCount，
    // 多个 NPC 交替渲染时 animTime 会被来回拉扯，失去单调性。
    // 这两对读写器用于在每次 handleAnimations 前后换入/换出该实体自己的时间轴。

    @Accessor("animTime")
    double gecko$getAnimTime();

    @Accessor("animTime")
    void gecko$setAnimTime(double animTime);

    @Accessor("lastGameTickTime")
    double gecko$getLastGameTickTime();

    @Accessor("lastGameTickTime")
    void gecko$setLastGameTickTime(double lastGameTickTime);
}
