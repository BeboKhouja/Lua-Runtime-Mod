package com.mokkachocolata.minecraft.mod.luaruntime.client.mixin;

import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.client.MainClassClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MainMenuMixin extends Screen {
    protected MainMenuMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "initWidgetsNormal")
    private void versions(int y, int spacingY, CallbackInfo ci) {
        this.addDrawableChild(new TextWidget(0, 0, 90, 10, Text.of("Lua Runtime v0.3"), textRenderer));
        this.addDrawableChild(new TextWidget(0, 10, 120, 10, Text.of("Powered by LuaJ v3.0.1"), textRenderer));
        callScriptsMenuListeners();
    }

    @Unique
    private void callScriptsMenuListeners()  {
        for (LuaEvent voids : MainClassClient.Instance.mainMenuListeners) {
            voids.Call();
        }
    }
}
