package com.mokkachocolata.minecraft.mod.luaruntime.client;

public class Config {
    public boolean allowCommands;
    public boolean allowChat;
    public Config.URLConfig[] urls;
    public Config(boolean allowCommands, boolean allowChat, Config.URLConfig[] urls) {
        this.allowCommands = allowCommands;
        this.allowChat = allowChat;
        this.urls = urls;
    }
    public static class URLConfig {
        public boolean allow;
        public String url;
        public URLConfig(boolean allow, String url) {
            this.allow = allow;
            this.url = url;
        }
    }
}
