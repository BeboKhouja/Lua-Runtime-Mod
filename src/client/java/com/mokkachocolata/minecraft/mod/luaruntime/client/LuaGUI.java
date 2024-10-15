package com.mokkachocolata.minecraft.mod.luaruntime.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class LuaGUI extends Screen {
    public Screen parent;
    public boolean Cancelable = true;
    public LuaValue CloseCallback;
    public ArrayList<ClickableWidget> clickableWidgets = new ArrayList<>();

    public LuaGUI(Text title) {
        super(title);
    }

    public ButtonWidget.Builder newButtonBuilder(Text text, LuaFunction callback) {
        return ButtonWidget.builder(text, p -> callback.call());
    }

    public TextWidget newTextBuilder(Text text, TextRenderer textRenderer) {
        assert client != null;
        return new TextWidget(text, textRenderer);
    }

    public TextFieldWidget newTextFieldWidget(TextRenderer textRenderer, int width, int height, Text text) {
        assert client != null;
        return new TextFieldWidget(textRenderer, width, height, text);
    }

    public void addButtonDrawableChild(ClickableWidget element) {
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
        if (CloseCallback != null && CloseCallback.isfunction())
            CloseCallback.call();
    }

    @Override
    public void close() {
        assert client != null;
        if (parent != null && Cancelable)
            client.setScreen(parent);
        else if (parent == null && Cancelable)
            client.setScreen(null);
    }
}
