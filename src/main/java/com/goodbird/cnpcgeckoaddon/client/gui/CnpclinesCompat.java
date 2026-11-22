package com.goodbird.cnpcgeckoaddon.client.gui;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

/** CNPC Lines 是可选依赖：只读取它的资源约定，不硬链接它的界面类。 */
public final class CnpclinesCompat {
    public static final String MOD_ID = "cnpclines";

    private CnpclinesCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static String displayName(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace);
    }

    /** 模组 logo；没有配置时返回 null，由界面显示问号。 */
    public static ResourceLocation logo(String namespace) {
        return ModList.get().getModContainerById(namespace)
                .flatMap(container -> container.getModInfo().getLogoFile())
                .map(path -> ResourceLocation.tryParse(namespace + ":" + path))
                .orElse(null);
    }

    /** CNPC Lines 的皮肤页默认目录。 */
    public static String skinRoot() {
        return "textures/entity/humanmale/";
    }

    /** 附加材质默认沿用 CNPC Lines 的 overlay 页目录。 */
    public static String overlayRoot() {
        return isLoaded() ? "textures/overlays/" : "textures/entity/humanmale/";
    }
}
