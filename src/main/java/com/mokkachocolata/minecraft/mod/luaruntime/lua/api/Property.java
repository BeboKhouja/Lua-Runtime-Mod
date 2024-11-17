package com.mokkachocolata.minecraft.mod.luaruntime.lua.api;

/**
 * A simple property.
 * @param <T>
 * @author Mokka Chocolata
 */
public class Property<T> {
    public final String propertyName;
    public final T propertyValue;

    public Property(String propertyName, T propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    /**
     * Gets the string of the value of this property.
     * @return The value of this property as a string
     */
    @Override public String toString() {
        return propertyValue.toString();
    }

    /**
     * Checks if the object is equal to the value of this property.
     *
     * @param obj The object to compare the value to
     * @return Whether the value is equal to the object in the parameter
     */
    @Override public boolean equals(Object obj) {
        return propertyValue == obj;
    }
}