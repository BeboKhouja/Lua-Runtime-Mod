package com.mokkachocolata.minecraft.mod.luaruntime.client.mixin;

import com.mokkachocolata.minecraft.mod.luaruntime.Consts;
import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaRuntimeClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Calls scripts when the player loads the main menu.
 *
 * @author Mokka Chocolata
 */
@Mixin(TitleScreen.class)
public abstract class MainMenuMixin extends Screen {
    protected MainMenuMixin(Text title) {
        super(title);
    }

    @Inject(at = @At("RETURN"), method = "initWidgetsNormal")
    private void versions(int y, int spacingY, CallbackInfo ci) {
        LuaRuntimeClient.lua_runtime_mod$loaded = true;
        this.addDrawableChild(new TextWidget(0, 0, 90, 10, Text.of("Lua Runtime " + Consts.Version), textRenderer));
        this.addDrawableChild(new TextWidget(0, 10, 120, 10, Text.of("Powered by LuaJ v3.0.1"), textRenderer));
        lua_runtime_mod$callScriptsMenuListeners();
    }

    @Unique
    private void lua_runtime_mod$callScriptsMenuListeners()  {
        for (LuaEvent voids : LuaRuntimeClient.Instance.LuaInstance.getMainMenuListeners()) {
            voids.Call();
        }
    }
}
