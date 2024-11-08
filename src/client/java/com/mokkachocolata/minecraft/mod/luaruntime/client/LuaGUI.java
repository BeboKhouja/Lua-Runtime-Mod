package com.mokkachocolata.minecraft.mod.luaruntime.client;

import com.mokkachocolata.minecraft.mod.luaruntime.LuaEvent;
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

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class LuaGUI extends Screen {
    public Screen parent;
    public boolean Cancelable = true;
    public ArrayList<LuaEvent> CloseCallback = new ArrayList<>();
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
        for (LuaEvent event : CloseCallback) event.Call();
    }

    @Override
    public void close() {
        assert client != null;
        if (!Cancelable) return;
        client.setScreen(parent);
    }
}
