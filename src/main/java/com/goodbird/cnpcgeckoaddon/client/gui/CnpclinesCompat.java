package com.goodbird.cnpcgeckoaddon.client.gui;

import net.neoforged.fml.ModList;

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

    /** CNPC Lines 的皮肤页默认目录。 */
    public static String skinRoot() {
        return "textures/entity/humanmale/";
    }

    /** 附加材质默认沿用 CNPC Lines 的 overlay 页目录。 */
    public static String overlayRoot() {
        return isLoaded() ? "textures/overlays/" : "textures/entity/humanmale/";
    }
}
