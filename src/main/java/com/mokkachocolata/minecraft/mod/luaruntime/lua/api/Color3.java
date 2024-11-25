package com.mokkachocolata.minecraft.mod.luaruntime.lua.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;

import java.awt.Color;
import java.util.ArrayList;

public class Color3 {
    static ArrayList<Color3> color3s = new ArrayList<>();
    public static Color3 RED = new Color3(255, 0, 0);
    public static Color3 GREEN = new Color3(0, 255, 0);
    public static Color3 BLUE = new Color3(0, 0, 255);
    private static int allInd = 0;
    private final int ind;
    public final int r;
    public final int g;
    public final int b;
    public Color3(int r, int g, int b) {
        allInd++;
        ind = allInd;
        this.r = r;
        this.g = g;
        this.b = b;
    }
    public Color3(String hex) {
        allInd++;
        ind = allInd;
        Color color = Color.decode(hex);
        this.r = color.getRed();
        this.g = color.getGreen();
        this.b = color.getBlue();
    }
    public static Color3 checkColor3(LuaValue color3) {
        return color3.getmetatable().get("__type").isstring() ? color3s.get(color3.getmetatable().get("__index").toint()) : null;
    }
    public static LuaValue getLuaTableStatic() {
        LuaValue table = LuaValue.tableOf();
        table.set("Red", RED.getLuaTable());
        table.set("Green", GREEN.getLuaTable());
        table.set("Blue", BLUE.getLuaTable());
        table.set("FromHex", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return new Color3(arg.toString()).getLuaTable();
            }
        });
        table.set("new", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                return new Color3(arg1.checkint(), arg2.checkint(), arg3.checkint()).getLuaTable();
            }
        });
        return table;
    }
    public LuaValue getLuaTable() {
        LuaValue table = LuaValue.tableOf();
        LuaValue metatable = LuaValue.tableOf();
        metatable.set("__type", "color3");
        metatable.set("__index", ind);
        table.setmetatable(metatable);
        table.set("R", r);
        table.set("G", g);
        table.set("B", b);
        table.set("Hex", Integer.toHexString(new Color(r, g, b).getRGB()).substring(2));
        return table;
    }
}
