package com.mokkachocolata.minecraft.mod.luaruntime;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

public class MainClass implements ModInitializer {

    private class Minecraft extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            LuaValue functions = tableOf();
            functions.set("Print", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Log.info(LogCategory.GENERAL, arg.toString());
                    return NONE;
                }
            });
            arg2.set("Minecraft", functions);
            return functions;
        }

        public Minecraft() {}
    }

    @Override
    public void onInitialize() {
        Globals g = JsePlatform.standardGlobals();
        g.load(new Minecraft());
        g.load("Minecraft.Print('Hello')");
    }
}
