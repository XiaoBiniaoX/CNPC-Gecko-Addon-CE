package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.ModelData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * Keeps the existing gecko model entity (and therefore its animation state) alive when the NPC
 * is reset / resynced, as long as the model and animation file did not actually change.
 * Without this, every reset rebuilds EntityCustomModel and animations snap back to frame 0.
 */
@Mixin(value = EntityNPCInterface.class, remap = false)
public abstract class MixinEntityNPCReset {

    @Unique
    private EntityCustomModel gecko$oldCustomModel;

    @Unique
    private ResourceLocation gecko$oldEntityName;

    @Inject(method = "readSpawnData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"), remap = false)
    private void gecko$captureModelBeforeReset(CompoundTag tag, CallbackInfo ci) {
        gecko$clearSnapshot();

        Object self = this;
        if (!(self instanceof EntityCustomNpc npc) || !(npc.display instanceof IDataDisplay displayData)) {
            return;
        }

        ModelData modelData = npc.modelData;
        LivingEntity currentEntity = ((ModelDataSharedAccessor) (Object) modelData).gecko$getEntity();
        if (!(currentEntity instanceof EntityCustomModel customModel)) {
            return;
        }

        CustomModelData customData = displayData.getCustomModelData();
        CompoundTag incomingModelData = tag.getCompound("ModelData");
        ResourceLocation incomingEntityName = gecko$parseResource(incomingModelData.getString("EntityName"));
        if (!tag.contains("Model") || !Objects.equals(modelData.getEntityName(), incomingEntityName)) {
            return;
        }

        CustomModelData incoming = new CustomModelData();
        incoming.readFromNBT(tag);
        if (!gecko$sameResource(customData.getModel(), incoming.getModel())) return;
        if (!gecko$sameResource(customData.getAnimFile(), incoming.getAnimFile())) return;

        gecko$oldCustomModel = customModel;
        gecko$oldEntityName = modelData.getEntityName();
    }

    @Inject(
            method = "readSpawnData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnoppes/npcs/ModelData;load(Lnet/minecraft/nbt/CompoundTag;)V",
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void gecko$restoreModelBeforeDisplayRead(CompoundTag tag, CallbackInfo ci) {
        Object self = this;
        if (gecko$oldCustomModel == null
                || !(self instanceof EntityCustomNpc npc)
                || !Objects.equals(gecko$oldEntityName, npc.modelData.getEntityName())) {
            return;
        }
        // Never restore a stale model entity from another level, it would never animate again
        if (gecko$oldCustomModel.level() != npc.level()) {
            return;
        }
        // Preserve the running animation state as-is: that is the whole point of keeping
        // this entity alive across the reset. No forced reset here, or the animation would
        // snap back to frame 0 (the flicker this preservation is meant to avoid).
        ((ModelDataSharedAccessor) (Object) npc.modelData).gecko$setEntity(gecko$oldCustomModel);
    }

    @Inject(method = "readSpawnData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"), remap = false)
    private void gecko$finishReset(CompoundTag tag, CallbackInfo ci) {
        gecko$clearSnapshot();
    }

    @Unique
    private void gecko$clearSnapshot() {
        gecko$oldCustomModel = null;
        gecko$oldEntityName = null;
    }

    @Unique
    private ResourceLocation gecko$parseResource(String value) {
        if (value == null || value.isEmpty()) return null;
        return ResourceLocation.tryParse(value);
    }

    @Unique
    private boolean gecko$sameResource(String first, String second) {
        ResourceLocation a = gecko$parseResource(first);
        ResourceLocation b = gecko$parseResource(second);
        if (a != null && b != null) return a.equals(b);
        return Objects.equals(first, second);
    }
}
