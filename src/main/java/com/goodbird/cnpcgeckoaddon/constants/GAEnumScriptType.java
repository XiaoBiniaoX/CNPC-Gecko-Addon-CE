package com.goodbird.cnpcgeckoaddon.constants;

import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.entity.data.DataScript;

/**
 * 动画关键帧脚本事件的分发（对应 NPC 脚本里的函数名 animationInstruction）。
 *
 * 历史：原实现用 mixin 反射改写 EnumScriptType 的 static final $VALUES 字段，
 * 想「真正」往枚举里追加一个常量，再用 valueOf("ANIMATION_INSTRUCTION") 取回。
 * 但 JDK 17+ 禁止反射写 static final，实测 100% 抛
 * IllegalAccessException: Can not set static final ... $VALUES，
 * 于是常量恒为 null、关键帧脚本事件静默失效，并且每次玩家登录往日志吐 22 行堆栈。
 * 反射调枚举构造器同样不行（Constructor.newInstance 硬编码拒绝：
 * IllegalArgumentException: Cannot reflectively create enum objects），
 * Unsafe.allocateInstance 虽能绕过但 name() 为 null 且需要 --add-exports，太脆。
 *
 * 查证 CNPC 源码后发现根本不需要碰枚举：事件分发链路
 * DataScript.runScript → ScriptContainer.run(EnumScriptType, Event)
 * 第一步就把枚举降级成 type.function 字符串，而
 * ScriptContainer.run(String, Object) 这个重载本身就是 public 的。
 * 直接按函数名调用即可，全程零反射、零枚举 hack。
 */
public class GAEnumScriptType {
    /** 脚本里对应的函数名：function animationInstruction(event){ ... } */
    public static final String ANIMATION_INSTRUCTION_FUNCTION = "animationInstruction";

    private GAEnumScriptType() {
    }

    /**
     * 把关键帧事件派发给该 NPC 的所有脚本容器。
     * 脚本里没定义 animationInstruction 时，CNPC 内部会记入 unknownFunctions 并静默忽略
     * （Jsr223Executor 捕获 NoSuchMethodException），不会报错也不会重复尝试。
     *
     * @param script NPC 的脚本处理器（EntityNPCInterface.script）
     * @param event  传给脚本函数的事件对象
     */
    public static void runAnimationInstruction(DataScript script, Object event) {
        if (script == null || !script.isEnabled()) return;
        for (ScriptContainer container : script.getScripts()) {
            if (container != null) {
                container.run(ANIMATION_INSTRUCTION_FUNCTION, event);
            }
        }
    }
}
