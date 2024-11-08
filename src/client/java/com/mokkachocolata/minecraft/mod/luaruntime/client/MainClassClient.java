package com.mokkachocolata.minecraft.mod.luaruntime.client;

import com.mojang.brigadier.context.CommandContext;
import com.mokkachocolata.minecraft.mod.luaruntime.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.ee;
import com.yevdo.jwildcard.JWildcard;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
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
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Environment(EnvType.CLIENT)
public class MainClassClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("luaruntimemod");
    public final ArrayList<LuaEvent> mainMenuListeners = new ArrayList<>();
    public static MainClassClient Instance;
    public MainClassClient.Minecraft LuaInstance;
    public ee.eee.eeee.eeeee.eeeeee.eeeeeee.eeeeeeee.eeeeeeeee.eeeeeeeeee.eeeeeeeeeee.eeeeeeeeeeee.eeeeeeeeeeeee eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee = new ee.eee.eeee.eeeee.eeeeee.eeeeeee.eeeeeeee.eeeeeeeee.eeeeeeeeee.eeeeeeeeeee.eeeeeeeeeeee.eeeeeeeeeeeee();
    public Config conf;
    private final ArrayList<LuaGUI> guis = new ArrayList<>();

    private <T> T[] toArray(Collection collection, Class<T> clazz) {
        T[] array = (T[]) Array.newInstance(clazz, collection.size());
        return ((Collection<T>) collection).toArray(array);
    }

    private OneArgFunction newGUIInstance() {
        return new OneArgFunction() {
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
        };
    }
    private boolean IsRunningOnPojavLauncher() {
        if (System.getenv("POJAV_RENDERER") != null) return true;
        String librarySearchPaths = System.getProperty("java.library.path", null);

        if (librarySearchPaths != null)
            for (var path: librarySearchPaths.split(":"))
                if (isKnownAndroidPathFragment(path)) {
                    LOGGER.warn("Found a library search path which seems like its hosted on an Android filesystem (It's actually ext4): {}", path);
                    return true;
                }

        String workingDirectory = System.getProperty("user.home", null);

        if (workingDirectory != null && isKnownAndroidPathFragment(workingDirectory))
            LOGGER.warn("It looks like the working directory is in an Android filesystem (cant repeat to say that again): {}", workingDirectory);

        return false;
    }

    private boolean isKnownAndroidPathFragment(String path) {
        return path.matches("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
    }

    public class Minecraft extends TwoArgFunction {
        private final LuaValue functions = tableOf();
        public void AddToTable(LuaValue value) {
            functions.add(value);
        }
        private boolean CheckIfURLisBlocked(String url) {
            for (Config.URLConfig urlConfig : conf.urls)
                if (Objects.equals(urlConfig.url, "$local") && (JWildcard.matches("*127.0.*.*", url) ||
                        JWildcard.matches("*192.168.*.*", url) ||
                        JWildcard.matches("*10.0.*.*", url) ||
                        JWildcard.matches("*0.0.*.*", url)
                ) && !urlConfig.allow)
                    return true;
                else return JWildcard.matches("*" + urlConfig.url, url) && !urlConfig.allow;
            return false;
        }
        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            arg2.set("print", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LOGGER.info(arg.toString());
                    return NIL;
                }
            });
            functions.set("Print", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LOGGER.info(arg.toString());
                    return NONE;
                }
            });
            functions.set("Platform", System.getProperty("os.name"));
            functions.set("Version", SharedConstants.getGameVersion().getName());
            functions.set("Loader", "Fabric");
            functions.set("LuaRuntimeVersion", 0.8);
            functions.set("ClientOrServer", "Client");
            {
                LuaValue table = getKeyTable();
                functions.set("Keybinds", table);
            }
            functions.set("Paste", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (!conf.allowPaste) throw new LuaError("Getting text from clipboard is not allowed by config");
                    try {
                        String data = (String) Toolkit.getDefaultToolkit()
                                .getSystemClipboard().getData(DataFlavor.stringFlavor);
                        return valueOf(data);
                    } catch (UnsupportedFlavorException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            functions.set("Copy", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if (!conf.allowCopy) throw new LuaError("Copying strings are not allowed by config");
                    MinecraftClient.getInstance().keyboard.setClipboard(arg.toString());
                    return NONE;
                }
            });
            functions.set("OpenLink", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg, LuaValue arg2) {
                    if (!conf.allowOpenLinks) throw new LuaError("Opening links are not allowed by config");
                    ConfirmLinkScreen.open(arg2.isnil() ? null : guis.get(arg2.getmetatable().get("__index").checkint()), arg.toString());
                    return NONE;
                }
            });
            LuaValue plrTable = tableOf();
            {
                plrTable.set("GetPos", new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs v) {
                        assert MinecraftClient.getInstance().player != null;
                        final ServerCommandSource source = MinecraftClient.getInstance().player.getCommandSource();
                        Vec3d position = source.getPosition();
                        return varargsOf(new LuaValue[]{
                                LuaValue.valueOf(position.x),
                                LuaValue.valueOf(position.y),
                                LuaValue.valueOf(position.z),
                        });
                    }
                });
                plrTable.set("GetRot", new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs v) {
                        assert MinecraftClient.getInstance().player != null;
                        final ServerCommandSource source = MinecraftClient.getInstance().player.getCommandSource();
                        Vec2f rotation = source.getRotation();
                        return varargsOf(new LuaValue[]{
                                LuaValue.valueOf(rotation.x),
                                LuaValue.valueOf(rotation.y),
                        });
                    }
                });
                plrTable.set("GetDimension", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        assert MinecraftClient.getInstance().world != null;
                        return valueOf(MinecraftClient.getInstance().world.getDimensionEntry().getIdAsString());
                    }
                });
            }
            functions.set("Player", plrTable);
            {
                LuaValue httpTable = tableOf();
                httpTable.set("Get", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg1) {
                        if (CheckIfURLisBlocked(arg1.toString())) throw new LuaError("URL blocked by config");
                        StringBuilder result = new StringBuilder();
                        try {
                            URL url = new URI(arg1.toString()).toURL();
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            for (String line; (line = reader.readLine()) != null;) result.append(line);
                            return LuaValue.valueOf(result.toString());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                httpTable.set("Post", new ThreeArgFunction() {
                    @Override
                    public LuaValue call(LuaValue url, LuaValue args, LuaValue contentType) {
                        if (CheckIfURLisBlocked(url.toString())) throw new LuaError("URL blocked by config");
                        java.util.Map<String, String> arguments = new java.util.HashMap<>();
                        for (int i = 0; i < args.checktable().length(); i++) {
                            args.get(i).checktable();
                            arguments.put(args.get(i).get("name").toString(), args.get(i).get("value").toString());
                        }
                        StringBuilder sb = new StringBuilder();
                        try {
                            URLConnection con = new URI(url.toString()).toURL().openConnection();
                            HttpURLConnection urlConnection = (HttpURLConnection) con;
                            urlConnection.setRequestMethod("POST");
                            urlConnection.setDoOutput(true);
                            StringJoiner sj = new StringJoiner("&");
                            for (Map.Entry<String, String> entry : arguments.entrySet())
                                sj.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
                            int len = out.length;
                            urlConnection.setFixedLengthStreamingMode(len);
                            urlConnection.setRequestProperty("Content-Type", contentType.toString());
                            urlConnection.connect();
                            OutputStream os = urlConnection.getOutputStream();
                            os.write(out);
                            os.flush();
                            os.close();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            for (String line; (line = reader.readLine()) != null;) sb.append(line);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return LuaValue.valueOf(sb.toString());
                    }
                });
                functions.set("Http", httpTable);
            }
            functions.set("AddCommand", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    var obj = new Object() {
                        CommandContext<FabricClientCommandSource> Context;
                    };
                    ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal(arg1.toString())
                            .executes(context -> {
                                obj.Context = context;
                                arg2.checkfunction().call();
                                return 1;
                            })
                    ));
                    {
                        LuaValue table = tableOf();
                        table.set("SendFeedback", new OneArgFunction() {
                            @Override
                            public LuaValue call(LuaValue arg1) {
                                obj.Context.getSource().sendFeedback(Text.literal(arg1.toString()));
                                return NONE;
                            }
                        });
                        return table;
                    }
                }
            });
            {
                LuaValue table = tableOf();
                table.set("ChatEnabled", LuaValue.valueOf(conf.allowChat));
                table.set("CommandsEnabled", LuaValue.valueOf(conf.allowCommands));
                LuaValue blockedTables = tableOf();
                for (int i = 0; i < conf.urls.length; i++) {
                    LuaValue blockUrlTable = tableOf();
                    blockUrlTable.set("Allowed", LuaValue.valueOf(conf.urls[i].allow));
                    blockUrlTable.set("URL", LuaValue.valueOf(conf.urls[i].url));
                    blockedTables.set(i, blockUrlTable);
                }
                table.set("BlockedUrls" , blockedTables);
                table.set("CopyEnabled", valueOf(conf.allowCopy));
                table.set("PasteEnabled", valueOf(conf.allowPaste));
                table.set("OpenLinkEnabled", valueOf(conf.allowOpenLinks));
                table.set("ListenChatEnabled", valueOf(conf.allowListenLinks));
                table.set("IsPojavLauncher", valueOf(IsRunningOnPojavLauncher()));
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
            functions.set("AddChatListener", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaEvent event = new LuaEvent(arg.checkfunction());
                    if (conf.allowListenLinks) // When disabled, the function won't be called
                        ServerMessageEvents.CHAT_MESSAGE.register((message, player, none) ->
                               event.Call(valueOf(Objects.requireNonNull(message.getContent().getLiteralString())), valueOf(Objects.requireNonNull(player.getName().getLiteralString())))
                        );
                    return event.GetTable();
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
            functions.set("CreateNewGUI", newGUIInstance());
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
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ClientTickEvents.START_CLIENT_TICK.register(client -> event.Call());
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
        if (IsRunningOnPojavLauncher())
            LOGGER.warn("Detected we are running on PojavLauncher, this will slow down script execution!");
        File scriptsFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), "lua");
        File config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lua_runtime_config.json");
        if (!config.exists()) {
            try {
                conf = new Config(false, false, new Config.URLConfig[] {
                        new Config.URLConfig(
                                false,
                                "$local"
                        )
                }, true, false, true, false);
                FileWriter writer = createConfigFile(config);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                FileInputStream inputStream = new FileInputStream(config);
                String fileContents = IOUtils.toString(inputStream);
                JSONObject configJSON = new JSONObject(fileContents);
                ArrayList<Config.URLConfig> urlConfigs = new ArrayList<>();
                JSONArray urls = configJSON.getJSONArray("urls");
                for (int i = 0; i < urls.length(); i++) {
                    JSONObject object = urls.getJSONObject(i);
                    urlConfigs.add(new Config.URLConfig(object.getBoolean("allow"), object.getString("url")));
                }
                conf = new Config(configJSON.getBoolean("allowCommands"), configJSON.getBoolean("allowChat"), toArray(urlConfigs, Config.URLConfig.class), configJSON.getBoolean("allowCopy"), configJSON.getBoolean("allowPaste"), configJSON.getBoolean("allowOpenLinks"), configJSON.getBoolean("allowListenChat"));
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
        new Thread(MinecraftClient.getInstance()::tick).start();
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

    private static @NotNull FileWriter createConfigFile(File config) throws IOException {
        config.createNewFile();
        JSONObject configJSON = new JSONObject();
        configJSON.put("allowCommands", false);
        configJSON.put("allowChat", false);
        JSONArray urls = new JSONArray();
        JSONObject local = new JSONObject();
        local.put("url", "$local");
        local.put("allow", false);
        urls.put(local);
        configJSON.put("urls", urls);
        configJSON.put("allowCopy", true);
        configJSON.put("allowPaste", false);
        configJSON.put("allowOpenLinks", true);
        configJSON.put("allowListenChat", false);
        FileWriter writer = new FileWriter(config);
        writer.write(configJSON.toString(4));
        return writer;
    }
}
