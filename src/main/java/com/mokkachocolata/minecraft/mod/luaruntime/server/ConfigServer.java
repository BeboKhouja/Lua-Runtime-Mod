package com.mokkachocolata.minecraft.mod.luaruntime.server;


public class ConfigServer {
    public URLConfig[] urls;
    public boolean allowListenChat;
    public ConfigServer(URLConfig[] urls,  boolean allowListenChat) {
        this.urls = urls;
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
