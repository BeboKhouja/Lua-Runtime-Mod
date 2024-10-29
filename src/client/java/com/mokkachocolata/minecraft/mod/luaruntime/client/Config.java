package com.mokkachocolata.minecraft.mod.luaruntime.client;

public class Config {
    public boolean allowCommands;
    public boolean allowChat;
    public Config(boolean allowCommands, boolean allowChat) {
        this.allowCommands = allowCommands;
        this.allowChat = allowChat;
    }
}
