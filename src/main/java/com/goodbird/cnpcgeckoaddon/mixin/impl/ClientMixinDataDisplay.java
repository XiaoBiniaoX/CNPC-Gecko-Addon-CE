package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.ModelData;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DataDisplay.class)
public class ClientMixinDataDisplay {
    @Shadow(remap = false)
    EntityNPCInterface npc;


    @Inject(method = "readToNBT", at = @At("TAIL"), remap = false)
    public void readFromNBTEnd(CompoundTag nbttagcompound, CallbackInfo ci){
        if(npc instanceof EntityCustomNpc customNpc) {
            LivingEntity model = ModelData.get(customNpc).getEntity(npc);
            if(model != null) {
                // Only copy data here. Do NOT reset the animation controller: this method
                // runs on every routine display sync, and resetting would restart the base
                // animation constantly (visible flicker). Config changes are detected by
                // signature comparison in MixinEntityCustomNpc.
                EntityUtil.Copy(npc, model);
            }
        }
    }
}
