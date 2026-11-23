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
        // 4.8.5 已将时间轴移到每实例的 AnimatableManager，不能再访问 GeoModel 的旧字段。
        // 使用原生时间轴，使控制器、骨骼快照和看门狗保持同一时间源。
        super.handleAnimations(animatable, instanceId, animationState);
        animatable.ownAnimTime = animationState.getAnimationTick();
        animatable.ownLastGameTickTime = animatable.getAnimatableInstanceCache().getManagerForId(instanceId).getLastUpdateTime();
        // 保留原有行为：清掉重渲染判据，修渲染 bug 用的，不动。
        ((GeoModelAccessor) this).setLastRenderedInstance(-1L);
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
