package com.mokkachocolata.minecraft.mod.luaruntime.lua.api;

public class Property {
    public final String propertyName;
    public final int propertyValue;

    public Property(String propertyName, int propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }
}