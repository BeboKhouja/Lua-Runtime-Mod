package com.mokkachocolata.minecraft.mod.luaruntime;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import static org.luaj.vm2.LuaValue.tableOf;

public class LuaEvent {
    private final LuaFunction func;
    private boolean disconnected = false;
    public LuaEvent(LuaFunction func) {
        this.func = func;
    }

    public void Call() {
        if (disconnected) return;
        func.call();
    }
    public void Call(LuaValue arg1) {
        if (disconnected) return;
        func.call(arg1);
    }
    public void Call(LuaValue arg1, LuaValue arg2) {
        if (disconnected) return;
        func.call(arg1, arg2);
    }
    public void Call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        if (disconnected) return;
        func.call(arg1, arg2, arg3);
    }

    public LuaTable GetTable() {
        LuaValue table = tableOf();
        table.set("Disconnect", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                disconnected = true;
                table.set("Connected", LuaValue.valueOf(false));
                return NONE;
            }
        });
        table.set("Connected", LuaValue.valueOf(disconnected));
        return table.checktable();
    }
}
