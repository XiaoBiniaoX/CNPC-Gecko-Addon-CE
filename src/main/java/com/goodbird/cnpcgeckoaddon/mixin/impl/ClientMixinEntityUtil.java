package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.entity.EntityCustomModel;
import com.goodbird.cnpcgeckoaddon.utils.NpcTextureUtils;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 贴图解析需要客户端纹理管理器，公共复制逻辑不加载它。 */
@Mixin(EntityUtil.class)
public class ClientMixinEntityUtil {
    @Inject(method = "Copy", at = @At("TAIL"), remap = false)
    private static void copyTexture(LivingEntity copied, LivingEntity entity, CallbackInfo ci) {
        if (entity instanceof EntityCustomModel modelEntity && copied instanceof EntityNPCInterface npc) {
            modelEntity.textureResLoc = NpcTextureUtils.getNpcTexture(npc);
        }
    }
}
