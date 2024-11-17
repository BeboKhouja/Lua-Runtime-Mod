package com.mokkachocolata.minecraft.mod.luaruntime.client;


public class Config {
    public boolean allowCommands;
    public boolean allowChat;
    public Config.URLConfig[] urls;
    public boolean allowCopy;
    public boolean allowPaste;
    public boolean allowOpenLinks;
    public boolean allowListenChat;
    public Config(boolean allowCommands, boolean allowChat, Config.URLConfig[] urls, boolean allowCopy, boolean allowPaste, boolean allowOpenLinks, boolean allowListenChat) {
        this.allowCommands = allowCommands;
        this.allowChat = allowChat;
        this.urls = urls;
        this.allowOpenLinks = allowOpenLinks;
        this.allowCopy = allowCopy;
        this.allowPaste = allowPaste;
        this.allowListenChat = allowListenChat;
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
