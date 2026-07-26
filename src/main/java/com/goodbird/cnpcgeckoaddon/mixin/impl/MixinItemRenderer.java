package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.tile.TileEntityCustomModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.items.ItemScripted;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Unique
    private static final BlockPos DUMMY_POS = BlockPos.ZERO;
    @Unique
    private static TileEntityCustomModel cachedTile = null;
    @Unique
    private static String cachedConfigKey = "";
    @Unique
    private static String lastPlayedAnim = "";
    @Unique
    private static long lastPlayedTick = 0;

    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V", at = @At("HEAD"), cancellable = true)
    public void onRenderGeckoItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
        if (!(stack.getItem() instanceof ItemScripted)) return;
        CompoundTag rootTag = stack.getTag();
        if (rootTag == null) return;
        if (!rootTag.contains("geckoData", 10)) return;
        CompoundTag geckoData = rootTag.getCompound("geckoData");
        String modelStr = geckoData.getString("model");
        if (modelStr.isEmpty()) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        String animFile = geckoData.getString("animFile");
        String idleAnim = geckoData.getString("idleAnim");
        String texStr = geckoData.getString("texture");

        // Build config key to detect resource changes
        String configKey = modelStr + "|" + texStr + "|" + animFile + "|" + idleAnim;

        // Reuse or recreate cached tile so animation state persists across frames
        if (cachedTile == null || !configKey.equals(cachedConfigKey)) {
            cachedTile = new TileEntityCustomModel(DUMMY_POS, Blocks.AIR.defaultBlockState());
            cachedTile.setLevel(level);
            cachedConfigKey = configKey;
        }

        // Always update resources on the cached tile (they may change even if config key is stable)
        cachedTile.modelResLoc = new ResourceLocation(modelStr);
        cachedTile.animResLoc = animFile.isEmpty() ? null : new ResourceLocation(animFile);
        cachedTile.idleAnimName = idleAnim;

        if (!texStr.isEmpty()) {
            cachedTile.textureResLoc = new ResourceLocation(texStr);
        } else {
            ItemScriptedWrapper wrapper = (ItemScriptedWrapper) stack.getCapability(ItemStackWrapper.ITEMSCRIPTEDDATA_CAPABILITY, null).orElse(null);
            if (wrapper != null && wrapper.texture != null) {
                cachedTile.textureResLoc = wrapper.texture;
            }
        }

        // Handle playAnimation trigger from script
        String playAnim = geckoData.getString("playAnim");
        if (!playAnim.isEmpty()) {
            long playTick = geckoData.getLong("playAnimTick");
            if (!playAnim.equals(lastPlayedAnim) || playTick != lastPlayedTick) {
                cachedTile.manualAnimName = playAnim;
                lastPlayedAnim = playAnim;
                lastPlayedTick = playTick;
            }
        }

        poseStack.pushPose();

        // Apply item model transform (positions correctly for hand/GUI like a normal item)
        model.applyTransform(transformType, poseStack, leftHand);
        // Center model pivot (same as vanilla render() does)
        poseStack.translate(-0.5, -0.5, -0.5);

        // GUI-specific display tweaks
        if (transformType == ItemDisplayContext.GUI) {
            float ox = geckoData.getFloat("displayOffsetX");
            float oy = geckoData.getFloat("displayOffsetY");
            if (ox != 0 || oy != 0) {
                poseStack.translate(ox / 16.0f, -oy / 16.0f, 0);
            }
            float sx = geckoData.getFloat("itemDisplayScaleX");
            float sy = geckoData.getFloat("itemDisplayScaleY");
            float sz = geckoData.getFloat("itemDisplayScaleZ");
            if (sx == 0) sx = 1;
            if (sy == 0) sy = 1;
            if (sz == 0) sz = 1;
            poseStack.scale(sx, sy, sz);
        }

        // User rotation & scale (applied around model center)
        float scale = geckoData.getFloat("modelScale");
        if (scale == 0) scale = 1;
        poseStack.scale(scale, scale, scale);

        float rotX = geckoData.getFloat("rotationX");
        float rotY = geckoData.getFloat("rotationY");
        float rotZ = geckoData.getFloat("rotationZ");
        if (rotX != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        if (rotY != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        if (rotZ != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));

        Minecraft.getInstance().getBlockEntityRenderDispatcher()
            .getRenderer(cachedTile)
            .render(cachedTile, 0, poseStack, buffer, combinedLight, combinedOverlay);

        poseStack.popPose();
        ci.cancel();
    }
}
