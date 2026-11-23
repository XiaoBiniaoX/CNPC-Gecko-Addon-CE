package com.goodbird.cnpcgeckoaddon.hooklib.cnpchooks;

import com.goodbird.cnpcgeckoaddon.hooklib.minecraft.HookLoader;
import com.goodbird.cnpcgeckoaddon.hooklib.minecraft.PrimaryClassTransformer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class CNPCHookLoader extends HookLoader {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    boolean isServer(){
        return net.minecraftforge.fml.relauncher.FMLLaunchHandler.side().isServer();
    }

    @Override
    public void registerHooks() {
        if(!isServer()) {
            registerHookContainer("com.goodbird.cnpcgeckoaddon.hooklib.cnpchooks.ClientHooks");
        }
        registerHookContainer("com.goodbird.cnpcgeckoaddon.hooklib.cnpchooks.CommonHooks");
    }
}
