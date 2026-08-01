package com.goodbird.cnpcgeckoaddon.mixin.impl;

import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.ModelDataShared;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Direct access to the cached model entity so it can be preserved across NPC resets.
 */
@Mixin(value = ModelDataShared.class, remap = false)
public interface ModelDataSharedAccessor {

    @Accessor("entity")
    LivingEntity gecko$getEntity();

    @Accessor("entity")
    void gecko$setEntity(LivingEntity entity);
}
