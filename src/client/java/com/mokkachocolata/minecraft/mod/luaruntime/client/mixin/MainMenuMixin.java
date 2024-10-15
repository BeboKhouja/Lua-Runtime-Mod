package com.mokkachocolata.minecraft.mod.luaruntime.client.mixin;

import com.mokkachocolata.minecraft.mod.luaruntime.client.MainClassClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.luaj.vm2.LuaValue;
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
        this.addDrawableChild(new TextWidget(0, 0, 90, 10, Text.of("Lua Runtime v1.0.0"), textRenderer));
        this.addDrawableChild(new TextWidget(0, 10, 56, 10, Text.of("LuaJ v1.0.0"), textRenderer));
        callScriptsMenuListeners();
    }

    @Unique
    private void callScriptsMenuListeners()  {
        for (LuaValue voids : MainClassClient.Instance.listeners) {
            voids.call();
        }
    }
}
