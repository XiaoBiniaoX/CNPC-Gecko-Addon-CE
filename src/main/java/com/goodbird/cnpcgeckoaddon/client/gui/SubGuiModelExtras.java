package com.goodbird.cnpcgeckoaddon.client.gui;

import noppes.npcs.entity.EntityNPCInterface;

/** 保留旧界面入口，直接打开统一编辑器的额外设置页。 */
public class SubGuiModelExtras extends GuiGeckoEditor {
    public SubGuiModelExtras(EntityNPCInterface npc) {
        super(npc, 2);
    }
}
