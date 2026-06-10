package com.mokkachocolata.minecraft.mod.luaruntime.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mokkachocolata.minecraft.mod.luaruntime.client.lua.api.Minecraft;
import com.mokkachocolata.minecraft.mod.luaruntime.ee;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Unique;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static com.mokkachocolata.minecraft.mod.luaruntime.Utils.toArray;

@SuppressWarnings("deprecation")
@Environment(EnvType.CLIENT)
public class LuaRuntimeClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("luaruntimemod");
    public final ArrayList<LuaEvent> mainMenuListeners = new ArrayList<>();
    public static LuaRuntimeClient Instance;
    @Unique public static boolean lua_runtime_mod$loaded = false;
    public Minecraft LuaInstance;
    private File configFile;
    public ee.eee.eeee.eeeee.eeeeee.eeeeeee.eeeeeeee.eeeeeeeee.eeeeeeeeee.eeeeeeeeeee.eeeeeeeeeeee.eeeeeeeeeeeee eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee = new ee.eee.eeee.eeeee.eeeeee.eeeeeee.eeeeeeee.eeeeeeeee.eeeeeeeeee.eeeeeeeeeee.eeeeeeeeeeee.eeeeeeeeeeeee();
    public Config conf;
    private final ArrayList<LuaGUI> guis = new ArrayList<>();

    void ScriptError(Exception e) {
        LOGGER.error("An error occurred while executing a script!");
        LOGGER.error(e.getMessage());
        DisplayScriptError(e);
    }
    private void DisplayScriptError(Exception e) {
        if (lua_runtime_mod$loaded)
            net.minecraft.client.Minecraft.getInstance().execute(() ->
                    net.minecraft.client.Minecraft.getInstance().setScreen(
                            new ScriptErrorInGame(net.minecraft.client.Minecraft.getInstance().font, e)
                    )
            );
        else
            new ScriptError(e.getMessage()).Display();
    }
    private void ScriptError(Exception e, File child) {
        LOGGER.error("An error occurred while executing {}!", child.getName());
        LOGGER.error(e.getMessage());
        DisplayScriptError(e);
    }

    private boolean isKnownAndroidPathFragment(String path) {
        return path.matches("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
    }

    @Override
    public void onInitializeClient() {
        System.setProperty("java.awt.headless", "false");
        Instance = this;
        File scriptsFolder = new File(FabricLoader.getInstance().getGameDir().toFile(), "lua");
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "lua_runtime_config.json");
        File config = configFile;
        if (!config.exists()) {
            try {
                conf = new Config(false, false, new Config.URLConfig[] {
                        new Config.URLConfig(
                                false,
                                "$local"
                        )
                }, true, false, true, false, false, false, true);
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
                conf = new Config(
                        configJSON.getBoolean("allowCommands"),
                        configJSON.getBoolean("allowChat"),
                        toArray(urlConfigs, Config.URLConfig.class),
                        configJSON.getBoolean("allowCopy"),
                        configJSON.getBoolean("allowPaste"),
                        configJSON.getBoolean("allowOpenLinks"),
                        configJSON.getBoolean("allowListenChat"),
                        configJSON.optBoolean("allowLuaJavaLibrary", false),
                        configJSON.optBoolean("allowAllDefFunctions", false),
                        configJSON.optBoolean("allowLuaCommand", true)
                );
            } catch (Exception e) {
                LOGGER.error("An error occurred trying to read the config file!");
                throw new RuntimeException(e);
            }
        }
        if (!scriptsFolder.exists()) scriptsFolder.mkdir();
        Globals g = JsePlatform.debugGlobals();
        if (!(conf.allowLuaJavaLibrary)) g.set("luajava", LuaValue.NIL);
        LuaInstance = new Minecraft(conf, LOGGER, guis, mainMenuListeners, g);
        g.load(LuaInstance);
        if (!(conf.allowAllDefFunctions)) {
            g.set("module", LuaValue.NIL);
            LuaValue os = g.get("os");
            os.set("execute", LuaValue.NIL);
            os.set("getenv", LuaValue.NIL);
            os.set("remove", LuaValue.NIL);
            os.set("rename", LuaValue.NIL);
            os.set("setlocale", LuaValue.NIL);
            os.set("tmpname", LuaValue.NIL);
            g.set("io", LuaValue.NIL);
            g.set("package", LuaValue.NIL);
            g.set("dofile", LuaValue.NIL);
            g.set("loadfile", LuaValue.NIL);
            LuaValue debug = g.get("debug");
            debug.set("gethook", LuaValue.NIL);
            debug.set("getinfo", LuaValue.NIL);
            debug.set("getlocal", LuaValue.NIL);
            debug.set("getregistry", LuaValue.NIL);
            debug.set("getupvalue", LuaValue.NIL);
            debug.set("getuservalue", LuaValue.NIL);
            debug.set("sethook", LuaValue.NIL);
            debug.set("setinfo", LuaValue.NIL);
            debug.set("setlocal", LuaValue.NIL);
            debug.set("setregistry", LuaValue.NIL);
            debug.set("setupvalue", LuaValue.NIL);
            debug.set("setuservalue", LuaValue.NIL);
            debug.set("upvalueid", LuaValue.NIL);
            debug.set("upvaluejoin", LuaValue.NIL);
        }
        g.load(new Scanner(
                        LuaRuntimeClient.class.getResourceAsStream(
                                "/assets/lua_runtime_mod/Minecraft.lua"),
                                StandardCharsets.UTF_8
                ).useDelimiter("\\A").next()).call();
        if (conf.allowLuaCommand) ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommands.literal("lua")
                    .then(ClientCommands.argument("code", StringArgumentType.greedyString())
                        .executes(context -> {
                            String code = StringArgumentType.getString(context, "code");
                            try {
                                LuaValue result = g.load(code).call();
                                if (result != LuaValue.NONE && !result.isnil())
                                    context.getSource().sendFeedback(Component.literal(result.tojstring()));
                            } catch (Exception e) {
                                context.getSource().sendError(Component.literal(e.getMessage()));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
            )
        );
        for (File child : Objects.requireNonNull(scriptsFolder.listFiles())) {
            try {
                if (FilenameUtils.getExtension(child.toPath().toString()).equals("lua")) {
                    String contents = Files.readString(child.toPath());
                    LOGGER.info("Loading {}", child.getName());
                    try {
                        g.load(contents).call();
                    } catch (Exception e) {
                        ScriptError(e, child);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void saveConfig() {
        try {
            JSONObject json = new JSONObject();
            json.put("allowCommands", conf.allowCommands);
            json.put("allowChat", conf.allowChat);
            JSONArray urls = new JSONArray();
            for (Config.URLConfig urlConfig : conf.urls) {
                JSONObject urlObj = new JSONObject();
                urlObj.put("url", urlConfig.url);
                urlObj.put("allow", urlConfig.allow);
                urls.put(urlObj);
            }
            json.put("urls", urls);
            json.put("allowCopy", conf.allowCopy);
            json.put("allowPaste", conf.allowPaste);
            json.put("allowOpenLinks", conf.allowOpenLinks);
            json.put("allowListenChat", conf.allowListenChat);
            json.put("allowLuaJavaLibrary", conf.allowLuaJavaLibrary);
            json.put("allowAllDefFunctions", conf.allowAllDefFunctions);
            json.put("allowLuaCommand", conf.allowLuaCommand);
            FileWriter writer = new FileWriter(configFile);
            writer.write(json.toString(4));
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
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
        configJSON.put("allowLuaJavaLibrary", false);
        configJSON.put("allowAllDefFunctions", false);
        configJSON.put("allowLuaCommand", true);
        FileWriter writer = new FileWriter(config);
        writer.write(configJSON.toString(4));
        return writer;
    }
}
