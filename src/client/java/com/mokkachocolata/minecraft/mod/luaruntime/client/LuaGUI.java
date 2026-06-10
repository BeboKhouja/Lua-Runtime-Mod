package com.mokkachocolata.minecraft.mod.luaruntime.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

/**
 * Creates a new GUI for use with Lua.
 *
 * @author Mokka Chocolata
 */
@Environment(EnvType.CLIENT)
public class LuaGUI extends Screen {
    public Screen parent;
    public boolean Cancelable = true;
    public ArrayList<LuaEvent> CloseCallback = new ArrayList<>();
    public ArrayList<AbstractWidget> clickableWidgets = new ArrayList<>();

    public LuaGUI(Component title) {
        super(title);
    }


    public void addClickableDrawableChild(AbstractWidget element) {
        clickableWidgets.add(element);
        init();
    }

    @Override
    protected void init() {
        for (AbstractWidget widget : clickableWidgets) {
            addRenderableWidget(widget);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return Cancelable;
    }

    @Override
    public void removed() {
        for (LuaEvent event : CloseCallback) event.Call();
    }

    @Override
    public void onClose() {
        if (!Cancelable) return;
        minecraft.setScreen(parent);
    }
}
