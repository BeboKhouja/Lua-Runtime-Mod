package com.mokkachocolata.minecraft.mod.luaruntime.lua.api;

import net.minecraft.util.math.ColorHelper;
import org.luaj.vm2.LuaTable;

public class Color3 {
    private static boolean VerifyColor3LuaTable(LuaTable color) {
        if (!color.get("R").isnumber()) return false;
        if (!color.get("G").isnumber()) return false;
        if (!color.get("B").isnumber()) return false;
        if (!color.get("A").isnumber()) return false;
        return true;
    }

    public static int fromTable(LuaTable color) {
        if (!VerifyColor3LuaTable((color))) return 0;

        return ColorHelper.fromFloats(
                color.get("A").tofloat(),
                color.get("R").tofloat(),
                color.get("G").tofloat(),
                color.get("B").tofloat()
        );
    }
}
