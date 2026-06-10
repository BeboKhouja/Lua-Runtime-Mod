package com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api.gui;

import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;

/**
 * The wrapper to create a GUI in Lua.
 *
 * @author Mokka Chocolata
 */
public class GUI extends OneArgFunction {
    private final ArrayList<LuaGUI> guis;

    public GUI(ArrayList<LuaGUI> guis) {
        this.guis = guis;
    }
    @Override
    public LuaValue call(LuaValue arg) {
        LuaValue table = tableOf();
        LuaGUI thisGui = new LuaGUI(Component.literal(arg.toString()));
        LuaTable metaTable = tableOf();
        guis.add(thisGui);
        metaTable.set("__index", guis.size());
        table.setmetatable(metaTable);
        table.set("SetParent", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (!arg.istable() || arg.getmetatable().get("__index").isnil()) throw new LuaError("Not a GUI");
                thisGui.parent = guis.get(arg.getmetatable().get("__index").toint());
                return NONE;
            }
        });
        table.set("SetCancelable", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                thisGui.Cancelable = arg.checkboolean();
                return NONE;
            }
        });
        table.set("AddCloseListener", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                LuaEvent event = new LuaEvent(arg.checkfunction());
                thisGui.CloseCallback.add(event);
                return event.GetTable();
            }
        });
        table.set("Close", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                thisGui.onClose();
                return NONE;
            }
        });
        table.set("Display", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                Minecraft.getInstance().setScreen(thisGui);
                return NONE;
            }
        });
        table.set("NewButton", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                LuaValue table = tableOf();
                Button.Builder button = Button.builder(Component.literal(arg1.toString()), p -> arg2.checkfunction().call());
                table.set("SetTooltip", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        button.tooltip(Tooltip.create(Component.literal(arg.toString())));
                        return NONE;
                    }
                });
                table.set("SetPosition", new TwoArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2) {
                        button.pos(arg1.toint(), arg2.toint());
                        return NONE;
                    }
                });
                table.set("SetSize", new TwoArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2) {
                        button.size(arg1.toint() / 2 - 205, arg2.toint());
                        return NONE;
                    }
                });
                table.set("AddDrawableChild", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        Button builded = button.build();
                        int index = thisGui.clickableWidgets.size();
                        thisGui.addClickableDrawableChild(builded);
                        LuaValue meta = tableOf();
                        meta.set("Delete", new ZeroArgFunction() {
                            @Override
                            public LuaValue call() {
                                thisGui.clickableWidgets.remove(index);
                                return NONE;
                            }
                        });
                        meta.set("IsFocused", new ZeroArgFunction() {
                            @Override
                            public LuaValue call() {
                                return LuaValue.valueOf(builded.isFocused());
                            }
                        });
                        meta.set("SetText", new OneArgFunction() {
                            @Override
                            public LuaValue call(LuaValue arg) {
                                builded.setMessage(Component.literal(arg.toString()));
                                return NONE;
                            }
                        });
                        meta.set("SetTooltip", new OneArgFunction() {
                            @Override
                            public LuaValue call(LuaValue arg) {
                                builded.setTooltip(Tooltip.create(Component.literal(arg.toString())));
                                return NONE;
                            }
                        });
                        meta.set("SetPosition", new TwoArgFunction() {
                            @Override
                            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                                builded.setPosition(arg1.toint(), arg2.toint());
                                return NONE;
                            }
                        });
                        meta.set("SetSize", new TwoArgFunction() {
                            @Override
                            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                                builded.setSize(arg1.toint() / 2 - 205, arg2.toint());
                                return NONE;
                            }
                        });
                        return meta;
                    }
                });
                return table;
            }
        });
        table.set("NewText", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1) {
                LuaValue table = tableOf();
                StringWidget button = new StringWidget(Component.literal(arg1.toString()), Minecraft.getInstance().font);
                table.set("SetText", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        button.setMessage(Component.literal(arg.toString()));
                        return NONE;
                    }
                });
                table.set("SetPosition", new TwoArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2) {
                        button.setX(arg1.toint());
                        button.setY(arg2.toint());
                        return NONE;
                    }
                });
                table.set("IsFocused", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(button.isFocused());
                    }
                });
                table.set("SetSize", new TwoArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2) {
                        button.setWidth(arg1.toint() / 2 - 205);
                        button.setHeight(arg2.toint());
                        return NONE;
                    }
                });
                table.set("AddDrawableChild", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        int index = thisGui.clickableWidgets.size();
                        thisGui.addClickableDrawableChild(button);
                        LuaValue meta = tableOf();
                        meta.set("Delete", new ZeroArgFunction() {
                            @Override
                            public LuaValue call() {
                                thisGui.clickableWidgets.remove(index);
                                return NONE;
                            }
                        });
                        return meta;
                    }
                });
                return table;
            }
        });
        table.set("NewTextField", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                LuaValue table = tableOf();
                EditBox button = new EditBox(Minecraft.getInstance().font, arg1.toint(), arg2.toint(), Component.empty());
                table.set("SetPlaceholder", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        button.setHint(Component.literal(arg.toString()));
                        return NONE;
                    }
                });
                table.set("SetEditable", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        button.setEditable(arg.checkboolean());
                        return NONE;
                    }
                });
                table.set("SetVisible", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        button.setVisible(arg.checkboolean());
                        return NONE;
                    }
                });
                table.set("IsFocused", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(button.isFocused());
                    }
                });
                table.set("SetPosition", new TwoArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2) {
                        button.setX(arg1.toint());
                        button.setY(arg2.toint());
                        return NONE;
                    }
                });
                table.set("SetSize", new TwoArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1, LuaValue arg2) {
                        button.setWidth(arg1.toint() / 2 - 205);
                        button.setHeight(arg2.toint());
                        return NONE;
                    }
                });
                table.set("GetText", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(button.getValue());
                    }
                });
                table.set("AddDrawableChild", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        int index = thisGui.clickableWidgets.size();
                        thisGui.addClickableDrawableChild(button);
                        LuaValue meta = tableOf();
                        meta.set("Delete", new ZeroArgFunction() {
                            @Override
                            public LuaValue call() {
                                thisGui.clickableWidgets.remove(index);
                                return NONE;
                            }
                        });
                        return meta;
                    }
                });
                return table;
            }
        });
        return table;
    }
}
