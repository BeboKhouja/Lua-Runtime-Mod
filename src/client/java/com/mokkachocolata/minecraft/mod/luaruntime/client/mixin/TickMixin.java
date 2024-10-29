package com.mokkachocolata.minecraft.mod.luaruntime.client.mixin;

import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.client.MainClassClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class TickMixin {
    @Inject(at = @At("TAIL"), method = "tick()V")
    private void listener(CallbackInfo ci) {
        for (LuaEvent listener : MainClassClient.Instance.tickListener) listener.Call();
    }
}
