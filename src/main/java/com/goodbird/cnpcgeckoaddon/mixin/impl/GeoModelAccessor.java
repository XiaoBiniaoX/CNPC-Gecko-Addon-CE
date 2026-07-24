package com.goodbird.cnpcgeckoaddon.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import software.bernie.geckolib.model.GeoModel;

@Mixin(value = GeoModel.class, remap = false)
public interface GeoModelAccessor {

    @Accessor
    void setLastRenderedInstance(long value);
}