package com.mokkachocolata.minecraft.mod.luaruntime.server;

import com.mokkachocolata.minecraft.mod.luaruntime.Consts;
import com.mokkachocolata.minecraft.mod.luaruntime.LuaEvent;
import com.mokkachocolata.minecraft.mod.luaruntime.lua.api.Color3;
import com.yevdo.jwildcard.JWildcard;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Environment(EnvType.SERVER)
public class LuaRuntimeServer implements DedicatedServerModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("luaruntimemod");
    public ConfigServer conf;
    public LuaRuntimeServer Instance;
    public Minecraft LuaInstance;

    private <T> T[] toArray(Collection collection, Class<T> clazz) {
        T[] array = (T[]) Array.newInstance(clazz, collection.size());
        return ((Collection<T>) collection).toArray(array);
    }


    public class Minecraft extends TwoArgFunction {
        private final LuaValue functions = tableOf();

        public void AddToTable(LuaValue value) {
            functions.add(value);
        }

        private boolean CheckIfURLisBlocked(String url) {
            for (ConfigServer.URLConfig urlConfig : conf.urls)
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
            arg2.set("Color3", Color3.getLuaTableStatic());
            functions.set("Platform", System.getProperty("os.name"));
            functions.set("Version", SharedConstants.getGameVersion().getName());
            functions.set("Loader", "Fabric");
            functions.set("LuaRuntimeVersion", Consts.Version);
            functions.set("ClientOrServer", "Server");
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
                            for (String line; (line = reader.readLine()) != null; ) result.append(line);
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
                            for (String line; (line = reader.readLine()) != null; ) sb.append(line);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return LuaValue.valueOf(sb.toString());
                    }
                });
                functions.set("Http", httpTable);
            }
            {
                LuaValue table = tableOf();
                LuaValue blockedTables = tableOf();
                for (int i = 0; i < conf.urls.length; i++) {
                    LuaValue blockUrlTable = tableOf();
                    blockUrlTable.set("Allowed", LuaValue.valueOf(conf.urls[i].allow));
                    blockUrlTable.set("URL", LuaValue.valueOf(conf.urls[i].url));
                    blockedTables.set(i, blockUrlTable);
                }
                table.set("BlockedUrls", blockedTables);
                table.set("ListenChatEnabled", valueOf(conf.allowListenChat));
                functions.set("Config", table);
            }
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
            LuaValue ticks = tableOf();
            {
                LuaValue clientTicks = tableOf();
                OneArgFunction forTicks = new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ServerTickEvents.START_SERVER_TICK.register(client -> event.Call());
                        return event.GetTable();
                    }
                };
                clientTicks.set("Tick", forTicks);
                clientTicks.set("Start", forTicks);
                clientTicks.set("StartWorld", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ServerTickEvents.START_WORLD_TICK.register(world -> event.Call());
                        return event.GetTable();
                    }
                });
                clientTicks.set("End", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ServerTickEvents.END_SERVER_TICK.register(client -> event.Call());
                        return event.GetTable();
                    }
                });
                clientTicks.set("EndWorld", new OneArgFunction() {
                    @Override
                    public LuaValue call(LuaValue arg) {
                        LuaEvent event = new LuaEvent(arg.checkfunction());
                        ServerTickEvents.END_WORLD_TICK.register(world -> event.Call());
                        return event.GetTable();
                    }
                });
                ticks.set("Server", clientTicks);
            }
            functions.set("Ticks", ticks);
            arg2.set("Minecraft", functions);
            return functions;
        }
    }

    @Override
    public void onInitializeServer() {
        LOGGER.info("SpongePowered LUAU Subsystem Version=0.8.7 Source=file:/home/user/net.fabricmc/sponge-mixin/0.15.3+mixin.0.8.7/51ee0a44ab05f6fddd66b09e66b3a16904f9c55d/sponge-mixin-0.15.3+mixin.0.8.7.jar Service=Knot/Fabric Env=CLIENT                       Just kidding obviously"); // Why not
        Instance = this;
        File scriptsFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), "lua");
        File config = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lua_runtime_config_server.json");
        if (!config.exists()) {
            try {
                conf = new ConfigServer(new ConfigServer.URLConfig[] {
                        new ConfigServer.URLConfig(
                                false,
                                "$local"
                        )
                }, true);
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
                ArrayList<ConfigServer.URLConfig> urlConfigs = new ArrayList<>();
                JSONArray urls = configJSON.getJSONArray("urls");
                for (int i = 0; i < urls.length(); i++) {
                    JSONObject object = urls.getJSONObject(i);
                    urlConfigs.add(new ConfigServer.URLConfig(object.getBoolean("allow"), object.getString("url")));
                }
                conf = new ConfigServer(toArray(urlConfigs, ConfigServer.URLConfig.class), configJSON.getBoolean("allowListenChat"));
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
    private static @NotNull FileWriter createConfigFile(File config) throws IOException {
        config.createNewFile();
        JSONObject configJSON = new JSONObject();
        JSONArray urls = new JSONArray();
        JSONObject local = new JSONObject();
        local.put("url", "$local");
        local.put("allow", false);
        urls.put(local);
        configJSON.put("urls", urls);
        configJSON.put("allowListenChat", false);
        FileWriter writer = new FileWriter(config);
        writer.write(configJSON.toString(4));
        return writer;
    }
}
