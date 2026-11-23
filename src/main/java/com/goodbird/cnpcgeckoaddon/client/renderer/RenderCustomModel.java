package com.goodbird.cnpcgeckoaddon.client.renderer;

import com.goodbird.cnpcgeckoaddon.client.model.ModelCustom;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.RenderUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.BiFunction;

public class RenderCustomModel extends GeoEntityRenderer<EntityCustomModel> {
    boolean tickManually;

    public RenderCustomModel(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ModelCustom());
    }

    @Override
    public RenderType getRenderType(EntityCustomModel animatable, ResourceLocation texture, @org.jetbrains.annotations.Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    protected void applyRotations(EntityCustomModel entityLiving, PoseStack matrixStackIn, float ageInTicks, float rotationYaw,
                                   float partialTicks) {
        Pose pose = entityLiving.getPose();
        if (pose != Pose.SLEEPING) {
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(180.0F - rotationYaw));
        }

        if (isShaking(animatable))
            rotationYaw += (float)(Math.cos(animatable.tickCount * 3.25d) * Math.PI * 0.4d);

        if (entityLiving.deathTime > 0) {
            float f = ((float) entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) {
                f = 1.0F;
            }

            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(f * this.getDeathMaxRotation(entityLiving)));
        } else if (entityLiving.isAutoSpinAttack()) {
            matrixStackIn.mulPose(Axis.XP.rotationDegrees(-90.0F - entityLiving.getXRot()));
            matrixStackIn
                    .mulPose(Axis.YP.rotationDegrees(((float) entityLiving.tickCount + partialTicks) * -75.0F));
        } else if (pose == Pose.SLEEPING) {
            Direction direction = entityLiving.getBedOrientation();
            float f1 = direction != null ? RenderUtils.getDirectionAngle(direction) : rotationYaw;
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(f1));
            matrixStackIn.mulPose(Axis.ZP.rotationDegrees(this.getDeathMaxRotation(entityLiving)));
            matrixStackIn.mulPose(Axis.YP.rotationDegrees(270.0F));
        } else if (entityLiving.hasCustomName()) {
            String s = ChatFormatting.stripFormatting(entityLiving.getName().getString());
            if ("Dinnerbone".equals(s) || "Grumm".equals(s)) {
                matrixStackIn.translate(0.0D, entityLiving.getBbHeight() + 0.1F, 0.0D);
                matrixStackIn.mulPose(Axis.ZP.rotationDegrees(180.0F));
            }
        }

    }

    @Override
    public void defaultRender(PoseStack poseStack, EntityCustomModel animatable, MultiBufferSource bufferSource, @org.jetbrains.annotations.Nullable RenderType renderType, @org.jetbrains.annotations.Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        withScale(animatable.size / 5f);
        BakedGeoModel bakedModel = getGeoModel().getBakedModel(getGeoModel().getModelResource(animatable));
        if (bakedModel.getBone("held_item").isPresent()) {
            GeoBone bone = bakedModel.getBone("held_item").get();
            bone.setHidden(true);
            if(!animatable.getMainHandItem().isEmpty()) {
                this.renderItem(bone, animatable, animatable.getMainHandItem(), poseStack, bufferSource, packedLight);
            }
        }
        if (bakedModel.getBone("left_held_item").isPresent()) {
            GeoBone bone = bakedModel.getBone("left_held_item").get();
            bone.setHidden(true);
            if(animatable.leftHeldItem!=null && !animatable.leftHeldItem.isEmpty()) {
                this.renderItem(bone, animatable, animatable.leftHeldItem, poseStack, bufferSource, packedLight);
            }
        }
        if(animatable.owner!=null && animatable.owner.level().getEntity(animatable.owner.getId())==null){
            tickManually = true;
        }
        if(tickManually){
            animatable.tickCount=(int)(System.currentTimeMillis()/50);
        }
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
    }

    /** 趁主渲染的实体与缩放姿态仍有效时叠加，不能在 defaultRender 返回后重画。 */
    @Override
    public void applyRenderLayers(PoseStack poseStack, EntityCustomModel animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        super.applyRenderLayers(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
        ResourceLocation overlayId = animatable.getOverlayTexture();
        if (overlayId == null) return;
        // 固定和普通缓冲都先提交主层，避免同一片元的主贴图晚于附加层绘制。
        if (bufferSource instanceof MultiBufferSource.BufferSource source) source.endBatch(renderType);
        boolean emissive = animatable.isOverlayGlowing();
        RenderType overlayType = OverlayRenderTypes.TYPES.apply(overlayId, emissive);
        VertexConsumer overlayBuffer = bufferSource.getBuffer(overlayType);
        reRender(bakedModel, poseStack, bufferSource, animatable, overlayType, overlayBuffer, partialTick,
                emissive ? LightTexture.FULL_BRIGHT : packedLight, packedOverlay, 1, 1, 1, 1);
    }

    /** 原版状态组合：共面偏移只影响深度比较，不改变模型几何或 PoseStack。 */
    private static final class OverlayRenderTypes extends RenderStateShard {
        private OverlayRenderTypes() {
            super("cnpcgeckoaddon_overlay", () -> {}, () -> {});
        }

        private static final BiFunction<ResourceLocation, Boolean, RenderType> TYPES = Util.memoize((texture, emissive) ->
                RenderType.create(emissive ? "cnpcgeckoaddon_overlay_emissive" : "cnpcgeckoaddon_overlay_translucent",
                        DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true,
                        RenderType.CompositeState.builder()
                                .setShaderState(emissive ? RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER : RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                                .setTextureState(new TextureStateShard(texture, false, false))
                                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                .setCullState(NO_CULL)
                                .setDepthTestState(LEQUAL_DEPTH_TEST)
                                .setWriteMaskState(COLOR_WRITE)
                                .setLightmapState(emissive ? NO_LIGHTMAP : LIGHTMAP)
                                .setOverlayState(OVERLAY)
                                .setLayeringState(POLYGON_OFFSET_LAYERING)
                                .createCompositeState(true)));
    }

    public GeoBone[] getPathFromRoot(GeoBone bone) {
        ArrayList<GeoBone> bones;
        for (bones = new ArrayList<>(); bone != null; bone = bone.getParent()) {
            bones.add(0, bone);
        }

        return bones.toArray(new GeoBone[0]);
    }

    public void renderItem(GeoBone locator, EntityCustomModel entity, ItemStack stack, PoseStack poseStack, MultiBufferSource buf, int light) {
        poseStack.pushPose();
        float scale = 0.7F;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.YP.rotationDegrees(180-entity.yBodyRot));
        GeoBone[] bonePath = getPathFromRoot(locator);

        for (GeoBone b : bonePath) {
            poseStack.translate(b.getPosX() / (16.0F * scale), b.getPosY() / (16.0F * scale), b.getPosZ() / (16.0F * scale));
            poseStack.translate(b.getPivotX() / (16.0F * scale), b.getPivotY() / (16.0F * scale), b.getPivotZ() / (16.0F * scale));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) ((double) b.getRotZ() / Math.PI * 180.0)));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) ((double) b.getRotY() / Math.PI * 180.0)));
            poseStack.mulPose(Axis.XP.rotationDegrees((float) ((double) b.getRotX() / Math.PI * 180.0)));
            poseStack.scale(b.getScaleX(), b.getScaleY(), b.getScaleZ());
            poseStack.translate(-b.getPivotX() / (16.0F * scale), -b.getPivotY() / (16.0F * scale), -b.getPivotZ() / (16.0F * scale));
        }
        poseStack.translate(0,0,-0.4);
        poseStack.translate(locator.getPivotX()/10f*5f/6f,locator.getPivotY()/10f*12f/14f,0);
        poseStack.mulPose(Axis.XP.rotationDegrees(215f));
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,light, OverlayTexture.NO_OVERLAY, poseStack, buf,null, 0);
        poseStack.popPose();
    }
}
