package com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api;

import com.mojang.brigadier.context.CommandContext;
import com.mokkachocolata.minecraft.mod.luaruntime.Consts;
import com.mokkachocolata.minecraft.mod.luaruntime.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.client.Config;
import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaGUI;
import com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api.gui.GUI;
import com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api.gui.Keys;
import com.mokkachocolata.minecraft.mod.luaruntime.lua.api.Property;
import com.yevdo.jwildcard.JWildcard;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;
import org.slf4j.Logger;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class Minecraft extends TwoArgFunction {
    private final LuaValue functions = tableOf();
    public void AddToTable(LuaValue value) {
        functions.add(value);
    }
    private final Logger LOGGER;
    private final ArrayList<LuaGUI> guis;
    private final Config conf;
    private final boolean IsRunningOnPojavLauncher;

    public ArrayList<LuaEvent> getMainMenuListeners() {
        return mainMenuListeners;
    }

    private final ArrayList<LuaEvent> mainMenuListeners;

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
        functions.set("LuaRuntimeVersion", Consts.Version);
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
                            LuaValue table = tableOf();
                            table.set("SendFeedback", new OneArgFunction() {
                                @Override
                                public LuaValue call(LuaValue arg) {
                                    obj.Context.getSource().sendFeedback(Text.literal(arg.toString()));
                                    return NONE;
                                }
                            });
                            arg2.checkfunction().call(table);
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
            table.set("IsPojavLauncher", valueOf(IsRunningOnPojavLauncher));
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
        functions.set("CreateNewGUI", new GUI(guis));
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
        for (Property key : Keys.keys) keyTable.set(key.propertyName, key.propertyValue);
        table.set("Key", keyTable);
        return table;
    }

    public Minecraft(Config conf, Logger logger, ArrayList<LuaGUI> guis, boolean runningOnPojavLauncher, ArrayList<LuaEvent> mainMenuListeners) {
        this.conf = conf;
        this.LOGGER = logger;
        this.guis = guis;
        this.IsRunningOnPojavLauncher = runningOnPojavLauncher;
        this.mainMenuListeners = mainMenuListeners;
    }
}