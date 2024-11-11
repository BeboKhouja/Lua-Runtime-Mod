package com.mokkachocolata.minecraft.mod.luaruntime.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

public class ScriptErrorInGame extends Screen {
    private final TextRenderer renderer;
    private final Exception exception;

    public ScriptErrorInGame(TextRenderer renderer, Exception exception) {
        super(Text.literal("Script Error"));
        this.renderer = renderer;
        this.exception = exception;
    }

    @Override
    protected void init() {
        final TextWidget title = new TextWidget(Text.literal("Script Error"), renderer);
        title.setPosition(50, 50);
        final TextFieldWidget errorWidget = new TextFieldWidget(renderer, 50, 70, 550, 200, Text.literal(exception.getMessage()));
        errorWidget.setText(exception.getMessage());
        errorWidget.setEditable(false);
        addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), a -> {
                    assert client != null;
                    client.setScreen(null);
                })
                    .position(520, 300)
                    .size(80, 23)
                    .build()
        );
        addDrawableChild(title);
        addDrawableChild(errorWidget);
    }
}
