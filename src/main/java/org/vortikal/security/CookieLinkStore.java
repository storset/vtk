package org.vortikal.security;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.util.cache.SimpleCache;

public class CookieLinkStore {

    private SimpleCache<String, String> cache = null;

    public void setCache(SimpleCache<String, String> cache) {
        this.cache = cache;
    }

    public UUID addToken(HttpServletRequest request, String token) {
        String ip = request.getRemoteAddr();
        UUID uuid = UUID.randomUUID();
        String key = ip + uuid.toString();
        this.cache.put(key, token);
        return uuid;
    }
   
    public String getToken(HttpServletRequest request, UUID id) {
        String ip = request.getRemoteAddr();
        String key = ip + id.toString();
        return this.cache.get(key);
    }
}
