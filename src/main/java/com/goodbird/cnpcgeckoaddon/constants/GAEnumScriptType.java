package com.goodbird.cnpcgeckoaddon.constants;

import noppes.npcs.constants.EnumScriptType;

public class GAEnumScriptType {
    public static EnumScriptType ANIMATION_INSTRUCTION;
    static {
        try {
            ANIMATION_INSTRUCTION = EnumScriptType.valueOf("ANIMATION_INSTRUCTION");
        } catch (Exception e) {
            ANIMATION_INSTRUCTION = null;
        }
    }
}