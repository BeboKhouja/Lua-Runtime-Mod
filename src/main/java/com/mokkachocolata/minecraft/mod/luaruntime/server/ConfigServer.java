package com.mokkachocolata.minecraft.mod.luaruntime.server;


public class ConfigServer {
    public URLConfig[] urls;
    public boolean allowListenChat;
    public boolean allowLuaJavaLibrary;
    public ConfigServer(URLConfig[] urls, boolean allowListenChat, boolean allowLuaJavaLibrary) {
        this.urls = urls;
        this.allowListenChat = allowListenChat;
        this.allowLuaJavaLibrary = allowLuaJavaLibrary;
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
