package com.mokkachocolata.minecraft.mod.luaruntime.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class ScriptErrorInGame extends Screen {
    public ScriptErrorInGame(Text cause) {
        super(Text.literal("Script Error"));
    }

    private final TextWidget title = new TextWidget(Text.literal("Script Error"), textRenderer);

    @Override
    protected void init(){
        addDrawableChild(title);
    }
}
