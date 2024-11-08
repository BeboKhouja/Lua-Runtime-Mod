package com.mokkachocolata.minecraft.mod.luaruntime.server;


public class ConfigServer {
    public URLConfig[] urls;
    // This is mistakenly named
    public boolean allowListenLinks;
    public ConfigServer(URLConfig[] urls,  boolean allowListenLinks) {
        this.urls = urls;
        this.allowListenLinks = allowListenLinks;
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
