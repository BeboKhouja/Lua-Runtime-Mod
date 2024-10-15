package com.mokkachocolata.minecraft.mod.luaruntime.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class MainClassClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("luaruntimemod");
    public final ArrayList<LuaValue> listeners = new ArrayList<>();
    public static MainClassClient Instance;
    public final ArrayList<LuaValue> tickListener = new ArrayList<>();

    private class Minecraft extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            final ArrayList<LuaGUI> guis = new ArrayList<>();
            LuaValue functions = tableOf();
            functions.set("Print", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LOGGER.info(arg.toString());
                    return NONE;
                }
            });
            functions.set("GetPlatform", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return valueOf(System.getProperty("os.name"));
                }
            });
            functions.set("GetClientOrServer", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return valueOf("Client");
                }
            });
            functions.set("ForceShutdown", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    System.exit(0);
                    return NONE;
                }
            });
            functions.set("ShutdownClient", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    MinecraftClient.getInstance().close();
                    return NONE;
                }
            });
            functions.set("AddClientLoadedListener", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (!arg.isfunction()) throw new LuaError("Not a function");
                    listeners.add(arg);
                    return NONE;
                }
            });
            functions.set("CreateNewGUI", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaValue table = tableOf();
                    LuaGUI thisGui = new LuaGUI(Text.literal(arg.toString()));
                    LuaTable metaTable = tableOf();
                    guis.add(thisGui);
                    metaTable.set("__index", guis.size());
                    table.setmetatable(metaTable);
                    table.set("SetParent", new OneArgFunction() {
                        @Override
                        public LuaValue call(LuaValue arg) {
                            if (!arg.istable() || !arg.getmetatable().get("__index").isnil()) throw new LuaError("Not a GUI");
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
                    table.set("SetCloseListener", new OneArgFunction() {
                        @Override
                        public LuaValue call(LuaValue arg) {
                            thisGui.CloseCallback = arg;
                            return NONE;
                        }
                    });
                    table.set("Close", new ZeroArgFunction() {
                        @Override
                        public LuaValue call() {
                            thisGui.close();
                            return NONE;
                        }
                    });
                    table.set("Display", new ZeroArgFunction() {
                        @Override
                        public LuaValue call() {
                            MinecraftClient.getInstance().setScreen(thisGui);
                            return NONE;
                        }
                    });
                    table.set("NewButton", new TwoArgFunction() {
                        @Override
                        public LuaValue call(LuaValue arg1, LuaValue arg2) {
                            LuaValue table = tableOf();
                            ButtonWidget.Builder button = thisGui.newButtonBuilder(Text.literal(arg1.toString()), arg2.checkfunction());
                            table.set("SetTooltip", new OneArgFunction() {
                                @Override
                                public LuaValue call(LuaValue arg) {
                                    button.tooltip(Tooltip.of(Text.literal(arg.toString())));
                                    return NONE;
                                }
                            });
                            table.set("SetPosition", new TwoArgFunction() {
                                @Override
                                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                                    button.position(arg1.toint(), arg2.toint());
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
                            table.set("Build", new ZeroArgFunction() {
                                @Override
                                public LuaValue call() {
                                    LuaValue table = tableOf();
                                    ButtonWidget builded = button.build();
                                    table.set("AddDrawableChild", new ZeroArgFunction() {
                                        @Override
                                        public LuaValue call() {
                                            int index = thisGui.clickableWidgets.size();
                                            thisGui.addButtonDrawableChild(builded);
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
                    });
                    table.set("NewText", new OneArgFunction() {
                        @Override
                        public LuaValue call(LuaValue arg1) {
                            LuaValue table = tableOf();
                            TextWidget button = thisGui.newTextBuilder(Text.literal(arg1.toString()), MinecraftClient.getInstance().textRenderer);
                            table.set("SetText", new OneArgFunction() {
                                @Override
                                public LuaValue call(LuaValue arg) {
                                    button.setMessage(Text.literal(arg.toString()));
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
                                    thisGui.addButtonDrawableChild(button);
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
                    table.set("NewTextField", new ThreeArgFunction() {
                        @Override
                        public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                            LuaValue table = tableOf();
                            TextFieldWidget button = thisGui.newTextFieldWidget(MinecraftClient.getInstance().textRenderer, arg1.checkint(), arg2.checkint(), Text.literal(arg3.toString()));
                            table.set("SetPlaceholder", new OneArgFunction() {
                                @Override
                                public LuaValue call(LuaValue arg) {
                                    button.setPlaceholder(Text.literal(arg.toString()));
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
                                    return LuaValue.valueOf(button.getText());
                                }
                            });
                            table.set("AddDrawableChild", new ZeroArgFunction() {
                                @Override
                                public LuaValue call() {
                                    int index = thisGui.clickableWidgets.size();
                                    thisGui.addButtonDrawableChild(button);
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
            });
            functions.set("GetFPS", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.getInstance().getCurrentFps());
                }
            });
            LuaValue ticks = tableOf();
            {
                LuaValue clientTicks = tableOf();
                clientTicks.set("ForTicks", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        arg.checkfunction(/* nothing so far */);
                        tickListener.add(arg);
                        return NONE;
                    }
                });
                clientTicks.set("Start", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        ClientTickEvents.START_CLIENT_TICK.register(client -> arg.checkfunction().call());
                        return NONE;
                    }
                });
                clientTicks.set("StartWorld", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        ClientTickEvents.START_WORLD_TICK.register(world -> arg.checkfunction().call());
                        return NONE;
                    }
                });
                clientTicks.set("End", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        ClientTickEvents.END_CLIENT_TICK.register(client -> arg.checkfunction().call());
                        return NONE;
                    }
                });
                clientTicks.set("EndWorld", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        ClientTickEvents.END_WORLD_TICK.register(world -> arg.checkfunction().call());
                        return NONE;
                    }
                });
                ticks.set("Client", clientTicks);
            }
            functions.set("Ticks", ticks);
            arg2.set("Minecraft", functions);
            return functions;
        }

        public Minecraft() {}
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("SpongePowered LUAU Subsystem Version=0.8.7 Source=file:/home/user/net.fabricmc/sponge-mixin/0.15.3+mixin.0.8.7/51ee0a44ab05f6fddd66b09e66b3a16904f9c55d/sponge-mixin-0.15.3+mixin.0.8.7.jar Service=Knot/Fabric Env=CLIENT                       Just kidding obviously");
        File scripts = FabricLoader.getInstance().getGameDir().toFile();
        Instance = this;
        File scriptsFolder = new File(scripts, "lua");
        if (!scriptsFolder.exists()) scriptsFolder.mkdir();
        Globals g = JsePlatform.standardGlobals();
        g.load(new Minecraft());
        for (File child : Objects.requireNonNull(scriptsFolder.listFiles())) {
            try {
                String contents = Files.readString(child.toPath());
                LOGGER.info("Loading {}", child.getName());
                g.load(contents).call();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
