package com.mokkachocolata.minecraft.mod.luaruntime;

import java.lang.reflect.Array;
import java.util.Collection;

public class Utils {
    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection collection, Class<T> clazz) {
        T[] array = (T[]) Array.newInstance(clazz, collection.size());
        return ((Collection<T>) collection).toArray(array);
    }
}
