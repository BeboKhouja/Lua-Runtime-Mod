package com.mokkachocolata.minecraft.mod.luaruntime.client;

import com.mokkachocolata.minecraft.mod.luaruntime.ee;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;

@Environment(EnvType.CLIENT)
public class MainClassClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("luaruntimemod");
    public final ArrayList<LuaEvent> mainMenuListeners = new ArrayList<>();
    public static MainClassClient Instance;
    public MainClassClient.Minecraft LuaInstance;
    public final ArrayList<LuaEvent> tickListener = new ArrayList<>();
    public ee.eee.eeee.eeeee.eeeeee.eeeeeee.eeeeeeee.eeeeeeeee.eeeeeeeeee.eeeeeeeeeee.eeeeeeeeeeee.eeeeeeeeeeeee eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee = new ee.eee.eeee.eeeee.eeeeee.eeeeeee.eeeeeeee.eeeeeeeee.eeeeeeeeee.eeeeeeeeeee.eeeeeeeeeeee.eeeeeeeeeeeee();
    public Config conf;

    public class Minecraft extends TwoArgFunction {
        private final LuaValue functions = tableOf();
        public void AddToTable(LuaValue value) {
            functions.add(value);
        }
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            final ArrayList<LuaGUI> guis = new ArrayList<>();
            functions.set("Print", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LOGGER.info(arg.toString());
                    return NONE;
                }
            });
            functions.set("Platform", System.getProperty("os.name"));
            functions.set("Version", "1.21.1");
            functions.set("LuaRuntimeVersion", 0.3);
            functions.set("ClientOrServer", "Client");
            {
                LuaValue table = getKeyTable();
                functions.set("Keybinds", table);
            }
            {
                LuaValue table = tableOf();
                table.set("ChatEnabled", LuaValue.valueOf(conf.allowChat));
                table.set("CommandsEnabled", LuaValue.valueOf(conf.allowCommands));
                functions.set("Config", table);
            }
            functions.set("RegisterKeybind", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg2, LuaValue arg3) {
                    KeyBinding bind = new KeyBinding(
                            "com.mokkachocolata.minecraft.mod.luaruntime.keys." + arg2,
                            InputUtil.Type.KEYSYM,
                            arg3.checkint(),
                            "com.mokkachocolata.minecraft.mod.luaruntime.keys.category"
                    );
                    KeyBindingHelper.registerKeyBinding(bind);
                    LuaValue table = tableOf();
                    table.set("GetPressed", new ZeroArgFunction() {
                        @Override
                        public LuaValue call() {
                            return LuaValue.valueOf(bind.wasPressed());
                        }
                    });
                    return table;
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
                    LuaEvent event = new LuaEvent(arg.checkfunction());
                    mainMenuListeners.add(event);
                    return event.GetTable();
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
                            table.set("AddDrawableChild", new ZeroArgFunction() {
                                @Override
                                public LuaValue call() {
                                    ButtonWidget builded = button.build();
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
                                    meta.set("IsFocused", new ZeroArgFunction() {
                                        @Override
                                        public LuaValue call() {
                                            return LuaValue.valueOf(builded.isFocused());
                                        }
                                    });
                                    meta.set("SetText", new OneArgFunction() {
                                        @Override
                                        public LuaValue call(LuaValue arg) {
                                            builded.setMessage(Text.literal(arg.toString()));
                                            return NONE;
                                        }
                                    });
                                    meta.set("SetTooltip", new OneArgFunction() {
                                        @Override
                                        public LuaValue call(LuaValue arg) {
                                            builded.setTooltip(Tooltip.of(Text.literal(arg.toString())));
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
                                            builded.setDimensions(arg1.toint() / 2 - 205, arg2.toint());
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
                    table.set("NewTextField", new TwoArgFunction() {
                        @Override
                        public LuaValue call(LuaValue arg1, LuaValue arg2) {
                            LuaValue table = tableOf();
                            TextFieldWidget button = thisGui.newTextFieldWidget(MinecraftClient.getInstance().textRenderer, arg1.checkint(), arg2.checkint(), Text.literal(""));
                            table.set("SetPlaceholder", new OneArgFunction() {
                                @Override
                                public LuaValue call(LuaValue arg) {
                                    button.setPlaceholder(Text.literal(arg.toString()));
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
            functions.set("RunCommand", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (!conf.allowCommands) throw new LuaError("Commands are not allowed by config");
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    if (player != null) {
                        CommandManager commandManager = Objects.requireNonNull(player.getServer()).getCommandManager();
                        ServerCommandSource commandSource = player.getServer().getCommandSource();
                        commandManager.executeWithPrefix(commandSource, arg.toString());
                    }
                    return NONE;
                }
            });
            functions.set("SendMessage", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    assert MinecraftClient.getInstance().player != null;
                    MinecraftClient.getInstance().player.sendMessage(Text.literal(arg.toString()));
                    return NONE;
                }
            });
            functions.set("Chat", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (!conf.allowChat) throw new LuaError("Sending messages to other players are not allowed by config");
                    assert MinecraftClient.getInstance().player != null;
                        MinecraftClient.getInstance().player.networkHandler.sendChatMessage(arg.toString());
                    return NONE;
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
                        LuaEvent event = new LuaEvent(arg.checkfunction(/* nothing so far */));
                        tickListener.add(event);
                        return event.GetTable();
                    }
                });
                clientTicks.set("Start", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ClientTickEvents.START_CLIENT_TICK.register(client -> event.Call());
                        return event.GetTable();
                    }
                });
                clientTicks.set("StartWorld", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ClientTickEvents.START_WORLD_TICK.register(world -> event.Call());
                        return event.GetTable();
                    }
                });
                clientTicks.set("End", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ClientTickEvents.END_CLIENT_TICK.register(client -> event.Call());
                        return event.GetTable();
                    }
                });
                clientTicks.set("EndWorld", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ClientTickEvents.END_WORLD_TICK.register(world -> event.Call());
                        return event.GetTable();
                    }
                });
                ticks.set("Client", clientTicks);
            }
            functions.set("Ticks", ticks);
            arg2.set("Minecraft", functions);
            return functions;
        }

        private @NotNull LuaValue getKeyTable() {
            LuaValue table = tableOf();
            LuaValue keyTable = tableOf();
            {
                // Alphabet Keys
                {
                    keyTable.set("A", GLFW.GLFW_KEY_A);
                    keyTable.set("B", GLFW.GLFW_KEY_B);
                    keyTable.set("C", GLFW.GLFW_KEY_C);
                    keyTable.set("D", GLFW.GLFW_KEY_D);
                    keyTable.set("E", GLFW.GLFW_KEY_E);
                    keyTable.set("F", GLFW.GLFW_KEY_F);
                    keyTable.set("G", GLFW.GLFW_KEY_G);
                    keyTable.set("H", GLFW.GLFW_KEY_H);
                    keyTable.set("I", GLFW.GLFW_KEY_I);
                    keyTable.set("J", GLFW.GLFW_KEY_J);
                    keyTable.set("K", GLFW.GLFW_KEY_K);
                    keyTable.set("L", GLFW.GLFW_KEY_L);
                    keyTable.set("M", GLFW.GLFW_KEY_M);
                    keyTable.set("N", GLFW.GLFW_KEY_N);
                    keyTable.set("O", GLFW.GLFW_KEY_O);
                    keyTable.set("P", GLFW.GLFW_KEY_P);
                    keyTable.set("Q", GLFW.GLFW_KEY_Q);
                    keyTable.set("R", GLFW.GLFW_KEY_R);
                    keyTable.set("S", GLFW.GLFW_KEY_S);
                    keyTable.set("T", GLFW.GLFW_KEY_T);
                    keyTable.set("U", GLFW.GLFW_KEY_U);
                    keyTable.set("V", GLFW.GLFW_KEY_V);
                    keyTable.set("W", GLFW.GLFW_KEY_W);
                    keyTable.set("X", GLFW.GLFW_KEY_X);
                    keyTable.set("Y", GLFW.GLFW_KEY_Y);
                    keyTable.set("Z", GLFW.GLFW_KEY_Z);
                }
                // Numerical Keys
                {
                    keyTable.set("0", GLFW.GLFW_KEY_0);
                    keyTable.set("1", GLFW.GLFW_KEY_1);
                    keyTable.set("2", GLFW.GLFW_KEY_2);
                    keyTable.set("3", GLFW.GLFW_KEY_3);
                    keyTable.set("4", GLFW.GLFW_KEY_4);
                    keyTable.set("5", GLFW.GLFW_KEY_5);
                    keyTable.set("6", GLFW.GLFW_KEY_6);
                    keyTable.set("7", GLFW.GLFW_KEY_7);
                    keyTable.set("8", GLFW.GLFW_KEY_8);
                    keyTable.set("9", GLFW.GLFW_KEY_9);
                }
            }
            table.set("Key", keyTable);
            return table;
        }

        public Minecraft() {}
    }


    @Override
    public void onInitializeClient() {
        LOGGER.info("SpongePowered LUAU Subsystem Version=0.8.7 Source=file:/home/user/net.fabricmc/sponge-mixin/0.15.3+mixin.0.8.7/51ee0a44ab05f6fddd66b09e66b3a16904f9c55d/sponge-mixin-0.15.3+mixin.0.8.7.jar Service=Knot/Fabric Env=CLIENT                       Just kidding obviously"); // Why not
        Instance = this;
        File scriptsFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), "lua");
        File config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lua_runtime_config.json");
        if (!config.exists()) {
            try {
                conf = new Config(false, false);
                config.createNewFile();
                JSONObject configJSON = new JSONObject();
                configJSON.put("allowCommands", false);
                configJSON.put("allowChat", false);
                FileWriter writer = new FileWriter(config);
                writer.write(configJSON.toString(4));
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                FileInputStream inputStream = new FileInputStream(config);
                String fileContents = IOUtils.toString(inputStream);
                JSONObject configJSON = new JSONObject(fileContents);
                conf = new Config(configJSON.getBoolean("allowCommands"), configJSON.getBoolean("allowChat"));
            } catch (Exception e) {
                LOGGER.error("An error occurred trying to read the config file!");
                throw new RuntimeException(e);
            }
        }
        if (!scriptsFolder.exists()) scriptsFolder.mkdir();
        Globals g = JsePlatform.standardGlobals();
        g.set("luajava", LuaValue.NIL);
        LuaInstance = new Minecraft();
        g.load(LuaInstance);
        for (File child : Objects.requireNonNull(scriptsFolder.listFiles())) {
            try {
                if (FilenameUtils.getExtension(child.toPath().toString()).equals("lua")) {
                    String contents = Files.readString(child.toPath());
                    LOGGER.info("Loading {}", child.getName());
                    try {
                        g.load(contents).call();
                    } catch (Exception e) {
                        LOGGER.error("An error occurred while executing {}!", child.getName());
                        LOGGER.error(e.getMessage());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
