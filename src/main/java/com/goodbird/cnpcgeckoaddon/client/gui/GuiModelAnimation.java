package com.goodbird.cnpcgeckoaddon.client.gui;

import net.minecraft.client.gui.GuiScreen;
import noppes.npcs.entity.EntityNPCInterface;

/** Retains the original 1.12.2 constructor used by existing callers. */
public class GuiModelAnimation extends GuiGeckoEditor {
    public GuiModelAnimation(GuiScreen parent, EntityNPCInterface npc) {
        super(parent, npc, 1);
    }
}
