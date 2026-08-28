package com.goodbird.cnpcgeckoaddon.constants;

import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.entity.data.DataScript;

public final class GAEnumScriptType {
    private static final String ANIMATION_INSTRUCTION_FUNCTION = "animationInstruction";

    private GAEnumScriptType() {
    }

    public static void runAnimationInstruction(DataScript script, Object event) {
        if (script == null || !script.isEnabled()) return;
        for (ScriptContainer container : script.getScripts()) {
            if (container != null) {
                container.run(ANIMATION_INSTRUCTION_FUNCTION, event);
            }
        }
    }
}
