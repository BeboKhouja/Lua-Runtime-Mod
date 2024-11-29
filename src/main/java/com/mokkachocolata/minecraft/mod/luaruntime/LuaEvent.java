package com.mokkachocolata.minecraft.mod.luaruntime;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import static org.luaj.vm2.LuaValue.tableOf;

/**
 * An event that can be used with Lua.
 *
 * @author Mokka Chocolata
 */
@Environment(EnvType.SERVER)
public class LuaEvent {
    private final LuaFunction func;
    private boolean disconnected = false;
    public LuaEvent(LuaFunction func) {
        this.func = func;
    }

    /**
     * Call this function without parameters.
     */
    public void Call() {
        if (disconnected) return;
        func.call();
    }
    /**
     * Call this function with the specified parameters.
     * @param arg1
     */
    public void Call(LuaValue arg1) {
        if (disconnected) return;
        func.call(arg1);
    }
    /**
     * Call this function with the specified parameters.
     * @param arg1
     * @param arg2
     */
    public void Call(LuaValue arg1, LuaValue arg2) {
        if (disconnected) return;
        func.call(arg1, arg2);
    }
    /**
     * Call this function with the specified parameters.
     * @param arg1
     * @param arg2
     * @param arg3
     */
    public void Call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        if (disconnected) return;
        func.call(arg1, arg2, arg3);
    }

    /**
     * Returns a table that can be used with Lua.
     * @return A table that can be used in Lua to interact with this event.
     */
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
