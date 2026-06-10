package com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mokkachocolata.minecraft.mod.luaruntime.Consts;
import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.client.Config;
import com.mokkachocolata.minecraft.mod.luaruntime.client.LuaGUI;
import com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api.gui.GUI;
import com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api.gui.Keys;
import com.mokkachocolata.minecraft.mod.luaruntime.lua.api.Property;
import com.yevdo.jwildcard.JWildcard;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;
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
import java.util.*;

import static com.mokkachocolata.minecraft.mod.luaruntime.Utils.toArray;


/**
 * The base Lua class for Lua Runtime.
 *
 * @author Mokka Chocolata
 */
public class Minecraft extends TwoArgFunction {
    private final LuaValue functions = tableOf();

    /**
     * Adds a {@link LuaValue} to the Minecraft class for scripts to use.
     * @param value The {@link LuaValue} to add to the Minecraft class.
     */
    public void AddToTable(LuaValue value) {
        functions.add(value);
    }
    private final Logger LOGGER;
    private final ArrayList<LuaGUI> guis;
    private final Config conf;

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
    private static String TryGetStringFromTable(LuaValue table) {
        final String originalString = table.toString();
        final LuaValue metatable = table.getmetatable();
        if (metatable.isnil()) return originalString;
        if (!metatable.get("__tostring").isfunction()) return originalString;
        return metatable.get("__tostring").call(table).toString();
    }
    @Override
    public LuaValue call(LuaValue arg1, LuaValue arg2) {
        OneArgFunction print = new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (arg.istable())
                    LOGGER.info(TryGetStringFromTable(arg));
                else
                    LOGGER.info(arg.toString());
                return NONE;
            }
        };
        arg2.set("print", print);
        functions.set("Print", print);
        functions.set("GetWindowScale", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                var window = net.minecraft.client.Minecraft.getInstance().getWindow();
                return varargsOf(
                        LuaValue.valueOf(window.getGuiScaledWidth()),
                        LuaValue.valueOf(window.getGuiScaledHeight())
                );
            }
        });
        arg2.set("waitAsync", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                new Thread(() -> {
                    try {
                        Thread.sleep(arg2.checklong());
                        arg1.checkfunction().call();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                return NONE;
            }
        });
        {
            LuaValue config = tableOf();
            config.set("GetFOV", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return valueOf(net.minecraft.client.Minecraft.getInstance().options.fov().get());
                }
            });
        }
        functions.set("Platform", System.getProperty("os.name"));
        functions.set("Version", SharedConstants.getCurrentVersion().name());
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
                net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(arg.toString());
                return NONE;
            }
        });
        functions.set("OpenLink", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg, LuaValue arg2) {
                if (!conf.allowOpenLinks) throw new LuaError("Opening links are not allowed by config");
                ConfirmLinkScreen.confirmLinkNow(arg2.isnil() ? null : guis.get(arg2.getmetatable().get("__index").checkint()), arg.toString());
                return NONE;
            }
        });
        LuaValue plrTable = tableOf();
        {
            LuaValue plrMetaTable = tableOf();
            plrMetaTable.set("__index", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs v) {
                    final net.minecraft.client.Minecraft mcInstance = net.minecraft.client.Minecraft.getInstance();
                    final String index = v.arg(2).checkstring().toString();
                    assert mcInstance.player != null;
                    switch (index) {
                        case "IsPlaying":
                            return varargsOf(new LuaValue[]{LuaValue.valueOf(mcInstance.isLocalServer())});
                        case "Position":
                            Vec3 position = mcInstance.player.position();
                            return varargsOf(new LuaValue[]{
                                    LuaValue.valueOf(position.x),
                                    LuaValue.valueOf(position.y),
                                    LuaValue.valueOf(position.z),
                            });
                        case "Rotation":
                            LocalPlayer player = mcInstance.player;
                            Vec2 rotation = new Vec2(player.getXRot(), player.getYRot());
                            return varargsOf(new LuaValue[]{
                                    LuaValue.valueOf(rotation.x),
                                    LuaValue.valueOf(rotation.y),
                            });
                        case "Dimension":
                            return valueOf(mcInstance.level.dimension().identifier().toString());
                        default:
                            return NIL;
                    }
                }
            });
            plrMetaTable.set("__newindex", new ThreeArgFunction() {
                @Override
                public LuaValue call(LuaValue table, LuaValue index, LuaValue value) {
                    switch (index.checkstring().toString()) {
                        case "Position":
                            throw new LuaError("Position is read-only.");
                        case "Rotation":
                            throw new LuaError("Rotation is read-only.");
                        case "Dimension":
                            throw new LuaError("Dimension is read-only.");
                    }
                    return NIL;
                }
            });
            plrTable.setmetatable(plrMetaTable);
            plrTable.set("Username", valueOf(net.minecraft.client.Minecraft.getInstance().getUser().getName()));
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
                    Map<String, String> arguments = new HashMap<>();
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
        functions.set("AddCommand", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
                var obj = new Object() {
                    CommandContext<FabricClientCommandSource> Context;
                };
                OneArgFunction sendFeedback = new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        obj.Context.getSource().sendFeedback(Component.literal(arg.toString()));
                        return NONE;
                    }
                };
                ClientCommandRegistrationCallback.EVENT.register(
                        (dispatcher, Nothing) -> {
                            var commandManager = ClientCommands.literal(arg1.toString());
                            if (arg2.istable()) {
                                HashMap<String, ArgumentType<?>> properties = new HashMap<>();
                                ArrayList<RequiredArgumentBuilder<FabricClientCommandSource, ?>> list = new ArrayList<>();
                                if (arg2.checktable().keyCount() == 0) throw new LuaError("Too few arguments");
                                for (int i = 0; i < arg2.checktable().keyCount(); i++) {
                                    String type = arg2.get(i).checktable().get("type").toString();
                                    ArgumentType<?> argumentType = switch (type) {
                                        case "string" -> StringArgumentType.string();
                                        case "number" -> DoubleArgumentType.doubleArg();
                                        case "boolean" -> BoolArgumentType.bool();
                                        case "nil" -> throw new LuaError("Why would you give nil to an argument type?");
                                        case "function" -> throw new LuaError("Why would you give a function to an argument type?");
                                        case "table" -> throw new LuaError("Tables not supported as an argument");
                                        default -> throw new LuaError("Unexpected type: " + type);
                                    };
                                    String name = arg2.get(i).checktable().get("name").toString();
                                    list.add(ClientCommands.argument(name, argumentType));
                                    properties.put(name, argumentType);
                                }
                                list.getLast().executes(context -> {
                                    LuaValue table = tableOf();
                                    table.set("SendFeedback", sendFeedback);
                                    ArrayList<LuaValue> values = new ArrayList<>();
                                    values.add(table);
                                    properties.forEach((k, v) ->
                                            values.add(switch (v) {
                                                        case BoolArgumentType ignored ->
                                                                valueOf(BoolArgumentType.getBool(context, k));
                                                        case StringArgumentType ignored ->
                                                                valueOf(StringArgumentType.getString(context, k));
                                                        case DoubleArgumentType ignored ->
                                                                valueOf(DoubleArgumentType.getDouble(context, k));
                                                        default -> NIL;
                                                    }
                                            ));
                                    arg3.checkfunction().invoke(toArray(values, LuaValue.class));
                                    return Command.SINGLE_SUCCESS;
                                });
                                for (int in = 0; in < list.size(); in++)
                                    if (in != list.size() - 1)
                                        list.get(in).then(list.get(in + 1));
                                commandManager.then(list.getFirst());
                            } else
                                commandManager.executes(context -> {
                                    obj.Context = context;
                                    LuaValue table = tableOf();
                                    table.set("SendFeedback", new OneArgFunction() {
                                        @Override
                                        public LuaValue call(LuaValue arg) {
                                            obj.Context.getSource().sendFeedback(Component.literal(arg.toString()));
                                            return NONE;
                                        }
                                    });
                                    arg2.checkfunction().call(table);
                                    return 1;
                                });
                            dispatcher.register(commandManager);
                        });
                {
                    LuaValue table = tableOf();
                    table.set("SendFeedback", sendFeedback);
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
            table.set("ListenChatEnabled", valueOf(conf.allowListenChat));
            functions.set("Config", table);
        }
        functions.set("PlaySound", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg, LuaValue volume, LuaValue pitch) {
                if (pitch.isnil()) pitch = valueOf(0);
                if (volume.isnil()) volume = valueOf(.5);
                final LuaValue finalVolume = volume;
                final LuaValue finalPitch = pitch;
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(new SoundInstance() {
                    @Override
                    public Identifier getIdentifier() {
                        return Identifier.parse(arg.toString());
                    }

                    @Override
                    public @Nullable WeighedSoundEvents resolve(SoundManager soundManager) {
                        return null;
                    }

                    @Override
                    public Sound getSound() {
                        return null;
                    }

                    @Override
                    public SoundSource getSource() {
                        return null;
                    }

                    @Override
                    public boolean isLooping() {
                        return false;
                    }

                    @Override
                    public boolean isRelative() {
                        return false;
                    }

                    @Override
                    public int getDelay() {
                        return 0;
                    }

                    @Override
                    public float getVolume() {
                        return finalVolume.checknumber().tofloat();
                    }

                    @Override
                    public float getPitch() {
                        return finalPitch.checknumber().tofloat();
                    }

                    @Override
                    public double getX() {
                        return 0;
                    }

                    @Override
                    public double getY() {
                        return 0;
                    }

                    @Override
                    public double getZ() {
                        return 0;
                    }

                    @Override
                    public SoundInstance.Attenuation getAttenuation() {
                        return null;
                    }
                });
                return NONE;
            }
        });
        functions.set("RegisterKeybind", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg2, LuaValue arg3) {
                KeyMapping bind = new KeyMapping(
                        "com.mokkachocolata.minecraft.mod.luaruntime.keys." + arg2,
                        InputConstants.Type.KEYSYM,
                        arg3.checkint(),
                        KeyMapping.Category.MISC
                );
                KeyMappingHelper.registerKeyMapping(bind);
                LuaValue table = tableOf();
                table.set("GetPressed", new ZeroArgFunction() {
                    @Override
                    public LuaValue call() {
                        return LuaValue.valueOf(bind.consumeClick());
                    }
                });
                return table;
            }
        });
        functions.set("ShutdownClient", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                net.minecraft.client.Minecraft.getInstance().close();
                return NONE;
            }
        });
        functions.set("AddChatListener", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                LuaEvent event = new LuaEvent(arg.checkfunction());
                if (conf.allowListenChat)
                    ServerMessageEvents.CHAT_MESSAGE.register((message, player, none) ->
                            event.Call(valueOf(Objects.requireNonNull(message.signedContent())), valueOf(Objects.requireNonNull(player.getName().getString())))
                    );
                return event.GetTable();
            }
        });
        functions.set("DisplayToast", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                SystemToast.add(
                        net.minecraft.client.Minecraft.getInstance().getToastManager(),
                        SystemToast.SystemToastId.NARRATOR_TOGGLE,
                        Component.literal(arg1.toString()),
                        Component.literal(arg2.toString())
                );
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
        functions.set("CreateNewGUI", new GUI(guis));
        functions.set("RunCommand", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (!conf.allowCommands) throw new LuaError("Commands are not allowed by config");
                LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
                if (player != null) {
                    Commands commandManager = Objects.requireNonNull(net.minecraft.client.Minecraft.getInstance().getSingleplayerServer()).getCommands();
                    CommandSourceStack commandSource = net.minecraft.client.Minecraft.getInstance().getSingleplayerServer().createCommandSourceStack();
                    commandManager.performPrefixedCommand(commandSource, arg.toString());
                }
                return NONE;
            }
        });
        functions.set("SendMessage", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg, LuaValue arg2) {
                assert net.minecraft.client.Minecraft.getInstance().player != null;
                if (arg2.isnil()) arg2 = valueOf(false);
                net.minecraft.client.Minecraft.getInstance().player.sendSystemMessage(Component.literal(arg.toString()));
                return NONE;
            }
        });
        functions.set("Chat", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if (!conf.allowChat) throw new LuaError("Sending messages to other players are not allowed by config");
                assert net.minecraft.client.Minecraft.getInstance().player != null;
                net.minecraft.client.Minecraft.getInstance().player.connection.sendChat(arg.toString());
                return NONE;
            }
        });
        functions.set("GetFPS", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(net.minecraft.client.Minecraft.getInstance().getFps());
            }
        });
        LuaValue ticks = tableOf();
        {
            LuaValue clientTicks = tableOf();
            OneArgFunction tick = new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaEvent event = new LuaEvent(arg.checkfunction());
                    ClientTickEvents.START_CLIENT_TICK.register(client -> event.Call());
                    return event.GetTable();
                }
            };
            clientTicks.set("Tick", tick);
            clientTicks.set("Start", tick);
            clientTicks.set("StartWorld", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaEvent event = new LuaEvent(arg.checkfunction());
                    ClientTickEvents.START_LEVEL_TICK.register(world -> event.Call());
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
                    ClientTickEvents.END_LEVEL_TICK.register(world -> event.Call());
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
        for (Property<Integer> key : Keys.keys) keyTable.set(key.propertyName, key.propertyValue);
        table.set("Key", keyTable);
        return table;
    }

    public Minecraft(Config conf, Logger logger, ArrayList<LuaGUI> guis, ArrayList<LuaEvent> mainMenuListeners, Globals g) {
        this.conf = conf;
        this.LOGGER = logger;
        this.guis = guis;
        this.mainMenuListeners = mainMenuListeners;
    }
}
