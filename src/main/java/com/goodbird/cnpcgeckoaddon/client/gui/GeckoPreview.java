package com.goodbird.cnpcgeckoaddon.client.gui;

import com.goodbird.cnpcgeckoaddon.client.model.ModelCustom;
import com.goodbird.cnpcgeckoaddon.client.renderer.RenderCustomModel;
import com.goodbird.cnpcgeckoaddon.data.CustomModelDataProvider;
import com.goodbird.cnpcgeckoaddon.data.ICustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.entity.EntityNPCInterface;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.snapshot.BoneSnapshot;
import software.bernie.geckolib3.geo.render.built.*;
import software.bernie.geckolib3.resource.GeckoLibCache;
import software.bernie.geckolib3.util.MatrixStack;
import javax.vecmath.Vector4f;
import java.util.Locale;

/** A detached visual entity. No world insertion, NPC reference or business callbacks. */
public final class GeckoPreview {
    private final PreviewEntity entity;
    private final RenderCustomModel renderer;
    private final ModelCustom provider;
    private GeoModel measured;
    private float[] bounds = {-0.5F, 0F, -0.5F, 0.5F, 2F, 0.5F};

    public GeckoPreview(EntityNPCInterface npc) {
        entity = new PreviewEntity(npc.world);
        net.minecraft.client.renderer.entity.RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        // Controllers resolve their model through RenderManager. Register one
        // preview-only renderer, so they never fetch the world NPC's provider.
        if (!manager.entityRenderMap.containsKey(PreviewEntity.class)) {
            manager.entityRenderMap.put(PreviewEntity.class, new RenderCustomModel(manager, new PreviewModel()));
        }
        renderer = (RenderCustomModel) manager.entityRenderMap.get(PreviewEntity.class);
        provider = (ModelCustom) renderer.getGeoModelProvider();
    }

    public void render(int x, int y, int w, int h, EntityNPCInterface npc, String model,
                       String animFile, ResourceLocation texture, ResourceLocation overlay,
                       String animation, float rotation, float zoom) {
        entity.modelResLoc = EditorResources.parse(model);
        entity.animResLoc = EditorResources.parse(animFile);
        entity.textureResLoc = texture;
        entity.overlay = overlay;
        ICustomModelData config = npc.getCapability(CustomModelDataProvider.DATA_CAP, null);
        entity.glowing = config.isOverlayGlowing();
        String key = model + "|" + animFile + "|" + animation;
        if (!entity.selection.equals(key)) {
            entity.selection = key;
            software.bernie.geckolib3.file.AnimationFile file = GeckoLibCache.getInstance().getAnimations().get(entity.animResLoc);
            entity.animation = file != null && file.getAnimation(animation) != null ? animation : "";
            AnimationData data = entity.getFactory().getOrCreateAnimationData(renderer.getUniqueID(entity));
            for (AnimationController controller : data.getAnimationControllers().values()) controller.markNeedsReload();
        }
        GeoModel geo = provider.getModel(provider.getModelLocation(entity));
        if (measured != geo) {
            measured = geo;
            AnimationData data = entity.getFactory().getOrCreateAnimationData(renderer.getUniqueID(entity));
            data.clearSnapshotCache();
            for (AnimationController controller : data.getAnimationControllers().values()) {
                controller.getBoneAnimationQueues().clear();
                controller.markNeedsReload();
            }
            float[] box = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                    Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
            MatrixStack stack = new MatrixStack();
            for (GeoBone bone : geo.topLevelBones) measure(bone, stack, box);
            if (Float.isFinite(box[0])) bounds = box;
        }
        float bw = Math.max(0.1F, bounds[3] - bounds[0]);
        float bh = Math.max(0.1F, bounds[4] - bounds[1]);
        float bd = Math.max(0.1F, bounds[5] - bounds[2]);
        float scale = Math.min((w - 20) / (float)Math.hypot(bw, bd), (h - 68) / bh) * 0.85F * zoom;
        Minecraft mc = Minecraft.getMinecraft();
        int factor = new ScaledResolution(mc).getScaleFactor();
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * factor, mc.displayHeight - (y + h) * factor, w * factor, h * factor);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableDepth();
            GlStateManager.enableColorMaterial();
            RenderHelper.enableStandardItemLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            GlStateManager.translate(x + w / 2F, y + (h - 48) / 2F, 100F);
            GlStateManager.scale(scale, -scale, scale);
            GlStateManager.rotate(rotation, 0, 1, 0);
            // RenderCustomModel applies a half turn around Y.
            GlStateManager.translate((bounds[0] + bounds[3]) / 2F, -(bounds[1] + bounds[4]) / 2F, (bounds[2] + bounds[5]) / 2F);
            renderer.doRender(entity, 0, 0, 0, 0, 0);
        } finally {
            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableColorMaterial();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.disableDepth();
            GL11.glPopAttrib();
        }
        float diagram = Math.min(25F / Math.max(bh, Math.max(config.getHeight(), 0.1F)),
                (w - 18F) / Math.max(bw, Math.max(config.getWidth(), 0.1F)));
        int foot = y + h - 20, cx = x + w / 2;
        outline(cx, foot, bw * diagram, bh * diagram, 0xffeeeeee);
        outline(cx, foot, config.getWidth() * diagram, config.getHeight() * diagram, 0xff77dd77);
        mc.fontRenderer.drawString(String.format(Locale.ROOT, "A %.2f x %.2f", bw, bh), x + 4, y + h - 18, 0xeeeeee);
        mc.fontRenderer.drawString(String.format(Locale.ROOT, "B %.2f x %.2f", config.getWidth(), config.getHeight()), x + 4, y + h - 9, 0x77dd77);
    }

    private static void outline(int cx, int foot, float w, float h, int color) {
        int x = Math.round(cx - w / 2), right = Math.round(cx + w / 2), top = Math.round(foot - h);
        Gui.drawRect(x, top, right + 1, top + 1, color);
        Gui.drawRect(x, foot, right + 1, foot + 1, color);
        Gui.drawRect(x, top, x + 1, foot, color);
        Gui.drawRect(right, top, right + 1, foot, color);
    }

    private static void measure(GeoBone bone, MatrixStack stack, float[] box) {
        BoneSnapshot pose = bone.getInitialSnapshot();
        if (pose == null) pose = new BoneSnapshot(bone);
        stack.push();
        stack.translate(-pose.positionOffsetX / 16F, pose.positionOffsetY / 16F, pose.positionOffsetZ / 16F);
        stack.moveToPivot(bone);
        stack.rotateZ(pose.rotationValueZ);
        stack.rotateY(pose.rotationValueY);
        stack.rotateX(pose.rotationValueX);
        stack.scale(pose.scaleValueX, pose.scaleValueY, pose.scaleValueZ);
        stack.moveBackFromPivot(bone);
        for (GeoCube cube : bone.childCubes) {
            stack.push();
            stack.moveToPivot(cube);
            stack.rotate(cube);
            stack.moveBackFromPivot(cube);
            for (GeoQuad quad : cube.quads) {
                if (quad == null) continue;
                for (GeoVertex vertex : quad.vertices) {
                    Vector4f v = new Vector4f(vertex.position.x, vertex.position.y, vertex.position.z, 1);
                    stack.getModelMatrix().transform(v);
                    box[0] = Math.min(box[0], v.x); box[3] = Math.max(box[3], v.x);
                    box[1] = Math.min(box[1], v.y); box[4] = Math.max(box[4], v.y);
                    box[2] = Math.min(box[2], v.z); box[5] = Math.max(box[5], v.z);
                }
            }
            stack.pop();
        }
        for (GeoBone child : bone.childBones) measure(child, stack, box);
        stack.pop();
    }

    private static final class PreviewModel extends ModelCustom {
        @Override
        public Animation getAnimation(String name, IAnimatable animatable) {
            PreviewEntity preview = (PreviewEntity) animatable;
            Animation source = super.getAnimation(name, animatable);
            if (source == null) return null;
            if (preview.sourceAnimation != source) {
                preview.sourceAnimation = source;
                Animation copy = new Animation();
                copy.animationName = source.animationName;
                copy.animationLength = source.animationLength;
                copy.boneAnimations = source.boneAnimations;
                // GeckoLib 3 mutates Animation.loop in setAnimation. Keep
                // preview loop state and event lists out of the shared cache.
                preview.previewAnimation = copy;
            }
            return preview.previewAnimation;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setLivingAnimations(EntityCustomModel model, Integer id, AnimationEvent event) {
            // Drive the GUI clock directly, without installing a persistent ticker.
            double tick = (System.nanoTime() - ((PreviewEntity) model).started) / 50000000.0;
            AnimationData data = model.getFactory().getOrCreateAnimationData(id);
            data.tick = tick;
            event.animationTick = tick;
            getAnimationProcessor().preAnimationSetup(model, tick);
            getAnimationProcessor().tickAnimation(model, id, tick, event, GeckoLibCache.getInstance().parser, false);
        }
    }

    private static final class PreviewEntity extends EntityCustomModel {
        final long started = System.nanoTime();
        String animation = "", selection = "";
        Animation sourceAnimation, previewAnimation;
        ResourceLocation overlay;
        boolean glowing;

        PreviewEntity(net.minecraft.world.World world) { super(world); }
        @Override public ResourceLocation getOverlayTexture() { return overlay; }
        @Override public boolean isOverlayGlowing() { return glowing; }
        @Override public void registerControllers(AnimationData data) {
            AnimationController<PreviewEntity> controller = new AnimationController<>(this, "preview", 0, event -> {
                if (animation.isEmpty()) return PlayState.STOP;
                event.getController().setAnimation(new AnimationBuilder().loop(animation));
                return PlayState.CONTINUE;
            });
            controller.registerCustomInstructionListener(event -> {});
            controller.registerSoundListener(event -> {});
            controller.registerParticleListener(event -> {});
            data.addAnimationController(controller);
        }
    }
}
