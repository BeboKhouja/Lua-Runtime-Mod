package com.mokkachocolata.minecraft.mod.luaruntime.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A GUI designed for when a script error occurs.
 *
 * @author Mokka Chocolata
 * @apiNote This is designed for when the game has started.
 */
public class ScriptErrorInGame extends Screen {
    private final Font renderer;
    private final Exception exception;

    public ScriptErrorInGame(Font renderer, Exception exception) {
        super(Component.literal("Script Error"));
        this.renderer = renderer;
        this.exception = exception;
    }

    @Override
    protected void init() {
        final StringWidget title = new StringWidget(Component.literal("Script Error"), renderer);
        title.setX(50); title.setY(50);
        final EditBox errorWidget = new EditBox(renderer, 50, 70, 550, 200, Component.literal(exception.getMessage()));
        errorWidget.setValue(exception.getMessage());
        errorWidget.setEditable(false);
        addRenderableWidget(
                Button.builder(Component.literal("Close"), a -> {
                    minecraft.setScreen(null);
                })
                    .pos(520, 300)
                    .size(80, 23)
                    .build()
        );
        addRenderableWidget(title);
        addRenderableWidget(errorWidget);
    }
}
