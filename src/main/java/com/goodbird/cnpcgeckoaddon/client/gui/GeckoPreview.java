package com.goodbird.cnpcgeckoaddon.client.gui;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.registry.EntityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.entity.EntityNPCInterface;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

/** 只存在于界面的展示实体：不加入世界，不绑定 NPC，不执行动画关键帧。 */
public final class GeckoPreview {
    private final EntityNPCInterface npc;
    private PreviewEntity entity;
    private BakedGeoModel measuredModel;
    private final Vector3f min = new Vector3f();
    private final Vector3f max = new Vector3f();
    private float modelWidth, modelHeight, modelDepth;

    public GeckoPreview(EntityNPCInterface npc) {
        this.npc = npc;
    }

    /** 静态骨骼包围盒尺寸（格），供宿主预览绘制 2D 对比框使用。 */
    public float modelWidth() {
        return Math.max(0.01f, modelWidth);
    }

    public float modelHeight() {
        return Math.max(0.01f, modelHeight);
    }

    public float modelDepth() {
        return Math.max(0.01f, modelDepth);
    }

    public void render(GuiGraphics graphics, int x, int y, int width, int height,
                       String model, String animationFile, ResourceLocation texture, ResourceLocation overlay, String animation,
                       float rotation, float zoom) {
        ResourceLocation modelId = ResourceLocation.tryParse(model);
        ResourceLocation animationId = ResourceLocation.tryParse(animationFile);
        BakedGeoModel baked = modelId == null ? null : GeckoLibCache.getBakedModels().get(modelId);
        if (baked == null) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, font.plainSubstrByWidth(GuiGeckoEditor.text("invalid_resource"), width - 12), x + 6, y + height / 2, 0xff8888, false);
            return;
        }
        // 动画文件尚未选好时也能查看几何，替代文件只用于本地预览。
        if (animationId == null || !GeckoLibCache.getBakedAnimations().containsKey(animationId)) {
            animationId = new ResourceLocation("cnpcgeckoaddon", "animations/none.animations.json");
        }

        if (entity == null || !modelId.equals(entity.modelResLoc) || !animationId.equals(entity.animResLoc)) {
            entity = new PreviewEntity(npc);
            entity.modelResLoc = modelId;
            entity.animResLoc = animationId;
        }
        entity.textureResLoc = texture;
        entity.overlay = overlay;
        entity.emissive = npc.display.isOverlayGlowing();
        entity.animation = GeckoLibCache.getBakedAnimations().get(animationId).animations().containsKey(animation) ? animation : "";
        if (measuredModel != baked) {
            measuredModel = baked;
            min.set(Float.POSITIVE_INFINITY);
            max.set(Float.NEGATIVE_INFINITY);
            PoseStack pose = new PoseStack();
            for (GeoBone bone : baked.topLevelBones()) measure(bone, pose);
            if (!Float.isFinite(min.x)) {
                min.set(-0.5f, 0, -0.5f);
                max.set(0.5f, 2, 0.5f);
            }
            modelWidth = max.x - min.x;
            modelHeight = max.y - min.y;
            modelDepth = max.z - min.z;
        }
        // 用静态几何包围盒适配；预览旋转后的横向跨度也包含深度，不读取碰撞箱。
        float span = Math.max(0.01f, (float)Math.hypot(max.x - min.x, max.z - min.z));
        float modelHeight = Math.max(0.01f, max.y - min.y);
        float scale = Math.min((width - 16) / span, (height - 16) / modelHeight) * zoom;
        float centerX = (min.x + max.x) / 2;
        float centerZ = (min.z + max.z) / 2;
        double angle = Math.toRadians(rotation);
        float rotatedCenterX = (float)(centerX * Math.cos(angle) - centerZ * Math.sin(angle));
        entity.yBodyRot = entity.yBodyRotO = 180 + rotation;
        entity.yHeadRot = entity.yHeadRotO = 180 + rotation;
        entity.setYRot(180 + rotation);
        // 先提交界面背景；实体只完整绘制一次，附加材质由渲染器的 reRender 图层完成。
        graphics.flush();
        graphics.enableScissor(x, y, x + width, y + height);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + width / 2f + rotatedCenterX * scale,
                    y + height / 2f + (min.y + max.y) / 2 * scale, 50 - 50 * scale);
            graphics.pose().scale(scale, scale, scale);
            InventoryScreen.renderEntityInInventory(graphics, 0, 0, 1,
                    new Quaternionf().rotateZ((float)Math.PI), null, entity);
        } finally {
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private void measure(GeoBone bone, PoseStack pose) {
        pose.pushPose();
        var initial = bone.getInitialSnapshot();
        // 未经渲染的新资源尚无初始快照，直接读取其未动画化的姿态。
        if (initial == null) initial = bone.saveSnapshot();
        pose.translate(-initial.getOffsetX() / 16f, initial.getOffsetY() / 16f, initial.getOffsetZ() / 16f);
        RenderUtils.translateToPivotPoint(pose, bone);
        pose.mulPose(new Quaternionf().rotationZYX(initial.getRotZ(), initial.getRotY(), initial.getRotX()));
        pose.scale(initial.getScaleX(), initial.getScaleY(), initial.getScaleZ());
        RenderUtils.translateAwayFromPivotPoint(pose, bone);
        for (GeoCube cube : bone.getCubes()) {
            if (cube.quads() == null) continue;
            pose.pushPose();
            RenderUtils.translateToPivotPoint(pose, cube);
            RenderUtils.rotateMatrixAroundCube(pose, cube);
            RenderUtils.translateAwayFromPivotPoint(pose, cube);
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f point = pose.last().pose().transformPosition(new Vector3f(vertex.position()));
                    min.min(point);
                    max.max(point);
                }
            }
            pose.popPose();
        }
        for (GeoBone child : bone.getChildBones()) measure(child, pose);
        pose.popPose();
    }

    private static final class PreviewEntity extends EntityCustomModel {
        private final long started = Util.getMillis();
        private String animation = "";
        private String playing = "";
        private ResourceLocation overlay;
        private boolean emissive;

        PreviewEntity(EntityNPCInterface npc) {
            super(EntityRegistry.entityCustomModel, npc.level());
            headBoneName = "";
        }

        @Override
        public ResourceLocation getOverlayTexture() {
            return overlay;
        }

        @Override
        public boolean isOverlayGlowing() {
            return emissive;
        }

        @Override
        public double getTick(Object animatable) {
            // GeckoLib 会再加游戏的插值时间，此处抵消它，使用连续的界面时钟。
            return (Util.getMillis() - started) / 50d - Minecraft.getInstance().getFrameTime();
        }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<EntityCustomModel>(this, "preview", 0, state -> {
                if (animation.isEmpty()) {
                    playing = "";
                    return PlayState.STOP;
                }
                if (!animation.equals(playing)) {
                    state.getController().forceAnimationReset();
                    state.getController().setAnimation(RawAnimation.begin().thenLoop(animation));
                    playing = animation;
                }
                return PlayState.CONTINUE;
            }).setCustomInstructionKeyframeHandler(event -> {})
                    .setSoundKeyframeHandler(event -> {})
                    .setParticleKeyframeHandler(event -> {}));
        }
    }
}
