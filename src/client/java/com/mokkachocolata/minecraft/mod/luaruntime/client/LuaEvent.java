package com.mokkachocolata.minecraft.mod.luaruntime.client;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ZeroArgFunction;

import static org.luaj.vm2.LuaValue.tableOf;

/**
 * An event that can be used with Lua.
 *
 * @author Mokka Chocolata
 * @apiNote For client use, it has script error functionality.
 */
public class LuaEvent {
    private final LuaFunction func;
    private boolean disconnected = false;
    public LuaEvent(LuaFunction func) {
        this.func = func;
    }

    private static void LoadFunc(LuaFunction func) {
        try {
            func.call();
        } catch (Exception e) {
            LuaRuntimeClient.Instance.ScriptError(e);
        }
    }
    private static void LoadFunc(LuaFunction func, LuaValue arg1) {
        try {
            func.call(arg1);
        } catch (Exception e) {
            LuaRuntimeClient.Instance.ScriptError(e);
        }
    }

    private static void LoadFunc(LuaFunction func, LuaValue arg1, LuaValue arg2) {
        try {
            func.call(arg1);
        } catch (Exception e) {
            LuaRuntimeClient.Instance.ScriptError(e);
        }
    }

    private static void LoadFunc(LuaFunction func, LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        try {
            func.call(arg1);
        } catch (Exception e) {
            LuaRuntimeClient.Instance.ScriptError(e);
        }
    }

    /**
     * Call this function without parameters.
     */
    public void Call() {
        if (disconnected) return;
        LoadFunc(func);
    }

    /**
     * Call this function with the specified parameters.
     * @param arg1
     */
    public void Call(LuaValue arg1) {
        if (disconnected) return;
        LoadFunc(func, arg1);
    }

    /**
     * Call this function with the specified parameters.
     * @param arg1
     * @param arg2
     */
    public void Call(LuaValue arg1, LuaValue arg2) {
        if (disconnected) return;
        LoadFunc(func, arg1, arg2);
    }

    /**
     * Call this function with the specified parameters.
     * @param arg1
     * @param arg2
     * @param arg3
     */
    public void Call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
        if (disconnected) return;
        LoadFunc(func, arg1, arg2, arg3);
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
