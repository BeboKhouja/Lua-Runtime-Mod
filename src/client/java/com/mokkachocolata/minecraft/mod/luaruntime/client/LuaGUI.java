package com.mokkachocolata.minecraft.mod.luaruntime.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

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
    public ArrayList<ClickableWidget> clickableWidgets = new ArrayList<>();

    public LuaGUI(Text title) {
        super(title);
    }


    public void addClickableDrawableChild(ClickableWidget element) {
        clickableWidgets.add(element);
        init();
    }

    @Override
    protected void init() {
        for (ClickableWidget widget : clickableWidgets) {
            addDrawableChild(widget);
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
    public void close() {
        assert client != null;
        if (!Cancelable) return;
        client.setScreen(parent);
    }
}
