package com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api;

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
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
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
                Window window = MinecraftClient.getInstance().getWindow();
                return varargsOf(
                        LuaValue.valueOf(window.getScaledWidth()),
                        LuaValue.valueOf(window.getScaledHeight())
                );
            }
        });
        arg2.set("waitAsync", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                new Thread(() -> {
                    try {
                        Thread.sleep(arg2.checklong()); // This means the wait time can go up to 9,223,372,036,854,775,807, which is 12 centuries. You'd have died at this point the timer ends.
                        arg1.checkfunction().call();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                return NONE;
            }
        });
//        arg2.set("wait", new OneArgFunction() {
//            @Override
//            public LuaValue call(LuaValue arg) {
//                g.yield(NONE);
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(arg.checklong());
//                        g.running.resume(NONE);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).start();
//                return NONE;
//            }
//        });
        {
            LuaValue config = tableOf();
            config.set("GetFOV", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return valueOf(MinecraftClient.getInstance().options.getFov().getValue());
                }
            });
        }
        /*functions.set("PixelRaycast", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg, LuaValue arg2, LuaValue arg3) {
                double maxReach = arg3.checkdouble();
                float tickDelta = 1.0F;
                boolean includeFluids = true;
                MinecraftClient client = MinecraftClient.getInstance();
                Window window = client.getWindow();
                int width = window.getScaledWidth();
                int height = window.getScaledHeight();
                assert client.cameraEntity != null;
                Vec3d cameraDirection = client.cameraEntity.getRotationVec(tickDelta);
                double fov = client.options.getFov().getValue();
                double angleSize = fov/height;
                Vector3f verticalRotationAxis = new Vector3f((Vector3fc) cameraDirection);
                return NONE*//*value*//*;
            }
        });*/
        functions.set("Platform", System.getProperty("os.name"));
        functions.set("Version", SharedConstants.getGameVersion().name());
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
            plrTable.set("Username", valueOf(MinecraftClient.getInstance().getSession().getUsername()));
            plrTable.set("GetPos", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs v) {
                    assert MinecraftClient.getInstance().player != null;
                    // Courtesy of a bug report
                    Vec3d position = MinecraftClient.getInstance().player.getPos();
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
                    ClientPlayerEntity player = MinecraftClient.getInstance().player;
                    Vec2f rotation = new Vec2f(player.getPitch(), player.getYaw());
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
                // Get around Java's stupid lambda non-final error
                var obj = new Object() {
                    CommandContext<FabricClientCommandSource> Context;
                };
                OneArgFunction sendFeedback = new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        obj.Context.getSource().sendFeedback(Text.literal(arg.toString()));
                        return NONE;
                    }
                };
                ClientCommandRegistrationCallback.EVENT.register(
                        (dispatcher, Nothing) -> {
                            var commandManager = ClientCommandManager.literal(arg1.toString());
                            // That was way difficult than we thought, took 3 hours to get this to work
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
                                    list.add(ClientCommandManager.argument(name, argumentType));
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
                                            obj.Context.getSource().sendFeedback(Text.literal(arg.toString()));
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
            table.set("IsPojavLauncher", valueOf(IsRunningOnPojavLauncher));
            functions.set("Config", table);
        }
        functions.set("PlaySound", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue arg, LuaValue volume, LuaValue pitch) {
                if (pitch.isnil()) pitch = valueOf(0);
                if (volume.isnil()) volume = valueOf(.5);
                final LuaValue finalVolume = volume;
                final LuaValue finalPitch = pitch;
                MinecraftClient.getInstance().getSoundManager().play(new SoundInstance() {
                    @Override
                    public Identifier getId() {
                        return Identifier.of(arg.toString());
                    }

                    @Override
                    public @Nullable WeightedSoundSet getSoundSet(SoundManager soundManager) {
                        return null;
                    }

                    @Override
                    public Sound getSound() {
                        return null;
                    }

                    @Override
                    public SoundCategory getCategory() {
                        return null;
                    }

                    @Override
                    public boolean isRepeatable() {
                        return false;
                    }

                    @Override
                    public boolean isRelative() {
                        return false;
                    }

                    @Override
                    public int getRepeatDelay() {
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
                    public AttenuationType getAttenuationType() {
                        return null;
                    }
                });
                return NONE;
            }
        });
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
                if (conf.allowListenChat) // When disabled, the function won't be called
                    ServerMessageEvents.CHAT_MESSAGE.register((message, player, none) ->
                            event.Call(valueOf(Objects.requireNonNull(message.getContent().getLiteralString())), valueOf(Objects.requireNonNull(player.getName().getLiteralString())))
                    );
                return event.GetTable();
            }
        });
        functions.set("DisplayToast", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                MinecraftClient.getInstance().getToastManager().add(
                        SystemToast.create(MinecraftClient.getInstance(), SystemToast.Type.NARRATOR_TOGGLE, Text.of(arg1.toString()), Text.of(arg2.toString()))
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
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                if (player != null) {
                    CommandManager commandManager = Objects.requireNonNull(player.getServer()).getCommandManager();
                    ServerCommandSource commandSource = player.getServer().getCommandSource();
                    commandManager.executeWithPrefix(commandSource, arg.toString());
                }
                return NONE;
            }
        });
        functions.set("SendMessage", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg, LuaValue arg2) {
                assert MinecraftClient.getInstance().player != null;
                if (arg2.isnil()) arg2 = valueOf(false);
                MinecraftClient.getInstance().player.sendMessage(Text.literal(arg.toString()), arg2.toboolean());
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
        for (Property<Integer> key : Keys.keys) keyTable.set(key.propertyName, key.propertyValue);
        table.set("Key", keyTable);
        return table;
    }

    public Minecraft(Config conf, Logger logger, ArrayList<LuaGUI> guis, boolean runningOnPojavLauncher, ArrayList<LuaEvent> mainMenuListeners, Globals g) {
        this.conf = conf;
        this.LOGGER = logger;
        this.guis = guis;
        this.IsRunningOnPojavLauncher = runningOnPojavLauncher;
        this.mainMenuListeners = mainMenuListeners;
    }
}