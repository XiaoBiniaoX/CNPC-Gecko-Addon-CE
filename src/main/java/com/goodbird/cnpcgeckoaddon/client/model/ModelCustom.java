package com.goodbird.cnpcgeckoaddon.client.model;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.impl.GeoModelAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ModelCustom extends GeoModel<EntityCustomModel> {

    @Override
    public ResourceLocation getAnimationResource(EntityCustomModel animatable) {
        if(!GeckoLibCache.getBakedAnimations().containsKey(animatable.animResLoc)){
            return new ResourceLocation("cnpcgeckoaddon","animations/none.animations.json");
        }
        return animatable.animResLoc;
    }

    @Override
    public ResourceLocation getModelResource(EntityCustomModel animatable) {
        if(!GeckoLibCache.getBakedModels().containsKey(animatable.modelResLoc)){
            return new ResourceLocation("cnpcgeckoaddon","geo/modelnotfound.geo.json");
        }
        if(!GeckoLibCache.getBakedAnimations().containsKey(animatable.animResLoc)){
            return new ResourceLocation("cnpcgeckoaddon","geo/animfilenotfound.geo.json");
        }
        return animatable.modelResLoc;
    }

    @Override
    public ResourceLocation getTextureResource(EntityCustomModel animatable) {
        if(!GeckoLibCache.getBakedModels().containsKey(animatable.modelResLoc)){
            return new ResourceLocation("cnpcgeckoaddon","textures/model/alphabet.png");
        }
        if(!GeckoLibCache.getBakedAnimations().containsKey(animatable.animResLoc)){
            return new ResourceLocation("cnpcgeckoaddon","textures/model/alphabet.png");
        }
        return animatable.textureResLoc;
    }

    @Override
    public void handleAnimations(EntityCustomModel animatable, long instanceId, AnimationState<EntityCustomModel> animationState) {
        GeoModelAccessor access = (GeoModelAccessor) this;
        // ---- 换入本实体自己的时间轴 ----
        // 本类全局只有一个实例（RenderCustomModel 构造时 new 一次），而 GeoModel 的
        // animTime / lastGameTickTime 是实例字段 → 所有 Gecko NPC 本来共用一条时间轴。
        // 配合 EntityCustomModel.getTick() 返回各自的 owner.tickCount，多个 NPC 交替渲染时
        // animTime 会被来回拉扯而失去单调性，一次性长动画因此被误判「播完」而中途消失。
        // 首次渲染（NaN）时以 GeoModel 当前值为起点，保持与原行为一致。
        // 首帧从 0 起算，绝不继承 GeoModel 的现值（那可能是别的 NPC 推高的）。
        // 原因：BoneSnapshot.lastResetRotationTick 由 stopRotAnim(animTime) 写入、跨帧持久，
        // AnimationProcessor:118 用 (animTime - lastResetRotationTick) / boneResetTime
        // 算骨骼归位进度。若本实体的 animTime 起点与 snapshot 里存的 tick 不同源，
        // 差值会异常巨大 → percentageReset 直接为 1.0 → 骨骼瞬间被 lerp 回初始姿态，
        // 观感就是「挥刀抬到一半，手突然缩回去」（动画其实还在播）。
        if (Double.isNaN(animatable.ownAnimTime)) {
            animatable.ownAnimTime = 0.0;
            // lastGameTickTime 必须与本实体的时间源（getTick() = owner.tickCount）同源，
            // 否则首帧 (lastUpdateTime - lastGameTickTime) 会是一个巨大的差值，
            // animTime 一步跳到几千，同样把骨骼归位算成瞬间完成。
            // 置为 NaN 交给下面的首帧兜底：本帧不推进 animTime，只对齐基准。
            animatable.ownLastGameTickTime = Double.NaN;
        }
        // 首帧基准未定：用本实体当前的时间源对齐，使本帧 delta 为 0（只对齐、不推进）。
        if (Double.isNaN(animatable.ownLastGameTickTime)) {
            animatable.ownLastGameTickTime = animatable.getTick(animatable) + net.minecraft.client.Minecraft.getInstance().getFrameTime();
        }
        access.gecko$setAnimTime(animatable.ownAnimTime);
        access.gecko$setLastGameTickTime(animatable.ownLastGameTickTime);

        super.handleAnimations(animatable, instanceId, animationState);

        // ---- 换出：把本帧推进后的时间轴存回该实体 ----
        animatable.ownAnimTime = access.gecko$getAnimTime();
        animatable.ownLastGameTickTime = access.gecko$getLastGameTickTime();


        // 保留原有行为：清掉重渲染判据，修渲染 bug 用的，不动。
        access.setLastRenderedInstance(-1L);
    }

    @Override
    public void setCustomAnimations(EntityCustomModel animatable, long instanceId, AnimationState<EntityCustomModel> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        CoreGeoBone head = getAnimationProcessor().getBone(animatable.headBoneName);

        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);

            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}