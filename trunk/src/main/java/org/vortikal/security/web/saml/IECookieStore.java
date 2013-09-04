/* Copyright (c) 2012, University of Oslo, Norway
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of the University of Oslo nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *      
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.vortikal.security.web.saml;

import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.util.cache.SimpleCache;

public class IECookieStore {

    private SimpleCache<String, Map<String, String>> cache = null;

    public void setCache(SimpleCache<String, Map<String, String>> cache) {
        this.cache = cache;
    }

    public UUID addToken(HttpServletRequest request, Map<String, String> cookies) {
        String ip = request.getRemoteAddr();
        UUID uuid = UUID.randomUUID();
        String key = ip + uuid.toString();
        this.cache.put(key, cookies);
        return uuid;
    }

    public Map<String, String> getToken(HttpServletRequest request, UUID id) {
        String ip = request.getRemoteAddr();
        String key = ip + id.toString();
        return this.cache.get(key);
    }

    public void dropToken(HttpServletRequest request, UUID id) {
        String ip = request.getRemoteAddr();
        String key = ip + id.toString();

        this.cache.remove(key);
    }
}
