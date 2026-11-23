package com.goodbird.cnpcgeckoaddon.client.gui;

import net.minecraft.client.gui.GuiScreen;
import noppes.npcs.entity.EntityNPCInterface;

/** Retains the original 1.12.2 constructor used by existing callers. */
public class SubGuiModelExtras extends GuiGeckoEditor {
    public SubGuiModelExtras(GuiScreen parent, EntityNPCInterface npc) {
        super(parent, npc, 0);
    }
}
