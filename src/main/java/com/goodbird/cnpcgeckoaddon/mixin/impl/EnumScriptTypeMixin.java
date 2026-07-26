package com.goodbird.cnpcgeckoaddon.mixin.impl;

import noppes.npcs.constants.EnumScriptType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(EnumScriptType.class)
public class EnumScriptTypeMixin {

    @Unique
    private static EnumScriptType ANIMATION_INSTRUCTION_EVENT;

    @Invoker("<init>")
    private static EnumScriptType invokeInit(String internalName, int internalId, String name) {
        throw new AssertionError();
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void onClInit(CallbackInfo ci) {
        try {
            Field valuesField = EnumScriptType.class.getDeclaredField("$VALUES");
            valuesField.setAccessible(true);
            EnumScriptType[] oldValues = (EnumScriptType[]) valuesField.get(null);
            int newOrdinal = oldValues.length;
            ANIMATION_INSTRUCTION_EVENT = invokeInit("ANIMATION_INSTRUCTION", newOrdinal, "animationInstruction");
            EnumScriptType[] newValues = new EnumScriptType[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = ANIMATION_INSTRUCTION_EVENT;
            valuesField.set(null, newValues);
            EnumScriptType.playerScripts = addToArray(EnumScriptType.playerScripts, ANIMATION_INSTRUCTION_EVENT);
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }
    }

    @Unique
    private static EnumScriptType[] addToArray(EnumScriptType[] array, EnumScriptType type) {
        EnumScriptType[] newArray = new EnumScriptType[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = type;
        return newArray;
    }
}
