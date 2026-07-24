package com.goodbird.cnpcgeckoaddon.mixin.impl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemScriptedWrapper.class)
public abstract class MixinItemScriptedWrapper {

    @Unique
    private CompoundTag getGeckoData() {
        ItemScriptedWrapper self = (ItemScriptedWrapper)(Object)this;
        ItemStack stack = self.getMCItemStack();
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        if (!tag.contains("geckoData", 10)) {
            tag.put("geckoData", new CompoundTag());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return tag.getCompound("geckoData");
    }

    @Unique
    private void markUpdated() {
        ((ItemScriptedWrapper)(Object)this).updateClient = true;
    }

    @Unique
    private void syncTag(CompoundTag geckoTag) {
        ItemScriptedWrapper self = (ItemScriptedWrapper)(Object)this;
        ItemStack stack = self.getMCItemStack();
        CompoundTag rootTag = new CompoundTag();
        rootTag.put("geckoData", geckoTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
    }

    @Unique
    public void setGeckoModel(String model) {
        CompoundTag data = getGeckoData();
        data.putString("model", model);
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void setGeckoTexture(String texture) {
        CompoundTag data = getGeckoData();
        data.putString("texture", texture);
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void setGeckoAnimationFile(String animation) {
        CompoundTag data = getGeckoData();
        data.putString("animFile", animation);
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void setGeckoIdleAnimation(String animation) {
        CompoundTag data = getGeckoData();
        data.putString("idleAnim", animation);
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void playAnimation(String animName) {
        CompoundTag data = getGeckoData();
        data.putString("playAnim", animName);
        data.putLong("playAnimTick", System.currentTimeMillis());
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void setGeckoItemDisplaySize(float x, float y, float z) {
        CompoundTag data = getGeckoData();
        data.putFloat("itemDisplayScaleX", x);
        data.putFloat("itemDisplayScaleY", y);
        data.putFloat("itemDisplayScaleZ", z);
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void setGeckoItemDisplayOffset(float x, float y) {
        CompoundTag data = getGeckoData();
        data.putFloat("displayOffsetX", x);
        data.putFloat("displayOffsetY", y);
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void setGeckoRotation(float x, float y, float z) {
        CompoundTag data = getGeckoData();
        data.putFloat("rotationX", x);
        data.putFloat("rotationY", y);
        data.putFloat("rotationZ", z);
        syncTag(data);
        markUpdated();
    }

    @Unique
    public void setGeckoScale(float scale) {
        CompoundTag data = getGeckoData();
        data.putFloat("modelScale", scale);
        syncTag(data);
        markUpdated();
    }
}