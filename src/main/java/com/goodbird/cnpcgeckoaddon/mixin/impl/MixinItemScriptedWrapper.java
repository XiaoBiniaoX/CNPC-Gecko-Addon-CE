package com.goodbird.cnpcgeckoaddon.mixin.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.wrapper.ItemScriptedWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemScriptedWrapper.class)
public abstract class MixinItemScriptedWrapper {

    @Unique
    private CompoundTag getGeckoData() {
        ItemScriptedWrapper self = (ItemScriptedWrapper)(Object)this;
        ItemStack stack = self.getMCItemStack();
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("geckoData", 10)) {
            tag.put("geckoData", new CompoundTag());
        }
        return tag.getCompound("geckoData");
    }

    @Unique
    private void markUpdated() {
        ((ItemScriptedWrapper)(Object)this).updateClient = true;
    }

    @Unique
    public void setGeckoModel(String model) {
        getGeckoData().putString("model", model);
        markUpdated();
    }

    @Unique
    public void setGeckoTexture(String texture) {
        getGeckoData().putString("texture", texture);
        markUpdated();
    }

    @Unique
    public void setGeckoAnimationFile(String animation) {
        getGeckoData().putString("animFile", animation);
        markUpdated();
    }

    @Unique
    public void setGeckoIdleAnimation(String animation) {
        getGeckoData().putString("idleAnim", animation);
        markUpdated();
    }

    @Unique
    public void playAnimation(String animName) {
        CompoundTag data = getGeckoData();
        data.putString("playAnim", animName);
        data.putLong("playAnimTick", System.currentTimeMillis());
        markUpdated();
    }

    @Unique
    public void setGeckoItemDisplaySize(float x, float y, float z) {
        CompoundTag data = getGeckoData();
        data.putFloat("itemDisplayScaleX", x);
        data.putFloat("itemDisplayScaleY", y);
        data.putFloat("itemDisplayScaleZ", z);
        markUpdated();
    }

    @Unique
    public void setGeckoItemDisplayOffset(float x, float y) {
        CompoundTag data = getGeckoData();
        data.putFloat("displayOffsetX", x);
        data.putFloat("displayOffsetY", y);
        markUpdated();
    }

    @Unique
    public void setGeckoRotation(float x, float y, float z) {
        CompoundTag data = getGeckoData();
        data.putFloat("rotationX", x);
        data.putFloat("rotationY", y);
        data.putFloat("rotationZ", z);
        markUpdated();
    }

    @Unique
    public void setGeckoScale(float scale) {
        getGeckoData().putFloat("modelScale", scale);
        markUpdated();
    }
}
