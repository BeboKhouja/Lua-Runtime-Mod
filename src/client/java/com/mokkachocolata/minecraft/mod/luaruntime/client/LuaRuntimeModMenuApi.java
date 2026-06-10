package com.mokkachocolata.minecraft.mod.luaruntime.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class LuaRuntimeModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            Config conf = LuaRuntimeClient.Instance.conf;

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("Lua Runtime Mod"))
                    .setSavingRunnable(() -> LuaRuntimeClient.Instance.saveConfig());

            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
            ConfigEntryBuilder entry = builder.entryBuilder();

            general.addEntry(entry.startBooleanToggle(Component.empty().append(Component.literal("[CAUTION] ").withStyle(ChatFormatting.YELLOW)).append(Component.literal("Allow /lua Command")), conf.allowLuaCommand)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Lets the local player run Lua code via /lua. Requires restart."))
                    .setSaveConsumer(v -> conf.allowLuaCommand = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.empty().append(Component.literal("[CAUTION] ").withStyle(ChatFormatting.YELLOW)).append(Component.literal("Allow Server Commands")), conf.allowCommands)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Scripts can run server commands (e.g. /give, /op)."))
                    .setSaveConsumer(v -> conf.allowCommands = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.empty().append(Component.literal("[CAUTION] ").withStyle(ChatFormatting.YELLOW)).append(Component.literal("Allow Chat")), conf.allowChat)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Scripts can send chat messages as the player."))
                    .setSaveConsumer(v -> conf.allowChat = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.empty().append(Component.literal("[CAUTION] ").withStyle(ChatFormatting.YELLOW)).append(Component.literal("Allow Listen Chat")), conf.allowListenChat)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Scripts can read all incoming chat messages."))
                    .setSaveConsumer(v -> conf.allowListenChat = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.literal("Allow Copy"), conf.allowCopy)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Scripts can write to the clipboard."))
                    .setSaveConsumer(v -> conf.allowCopy = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.empty().append(Component.literal("[CAUTION] ").withStyle(ChatFormatting.YELLOW)).append(Component.literal("Allow Paste")), conf.allowPaste)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Scripts can read the clipboard. Risk: exposes anything the user has copied (passwords, tokens, etc)."))
                    .setSaveConsumer(v -> conf.allowPaste = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.literal("Allow Open Links"), conf.allowOpenLinks)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("Scripts can open URLs in the browser. Minecraft's confirmation dialog still appears."))
                    .setSaveConsumer(v -> conf.allowOpenLinks = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.empty().append(Component.literal("[DANGEROUS] ").withStyle(ChatFormatting.RED)).append(Component.literal("Allow Lua Java Library")), conf.allowLuaJavaLibrary)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Risk: scripts can call any Java class, including unrestricted file, network, and process access. Requires restart."))
                    .setSaveConsumer(v -> conf.allowLuaJavaLibrary = v)
                    .build());

            general.addEntry(entry.startBooleanToggle(Component.empty().append(Component.literal("[DANGEROUS] ").withStyle(ChatFormatting.RED)).append(Component.literal("Allow All Default Lua Functions")), conf.allowAllDefFunctions)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("Risk: unlocks os.execute (shell commands), io (any file), and other restricted Lua functions. Requires restart."))
                    .setSaveConsumer(v -> conf.allowAllDefFunctions = v)
                    .build());

            return builder.build();
        };
    }
}
