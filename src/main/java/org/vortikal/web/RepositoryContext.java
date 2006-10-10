/* Copyright (c) 2004, University of Oslo, Norway
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
package org.vortikal.web;

import java.util.HashMap;
import java.util.Map;

import org.vortikal.repository.Resource;
import org.vortikal.context.BaseContext;


public class RepositoryContext {

    private Map tokenMap = new HashMap();

    private static final String KEY_FOR_PROCESSING_HITS = "forProcessingHits";
    private static final String KEY_FOR_PROCESSING_MISSES = "forProcessingMisses";
    private static final String KEY_NOT_FOR_PROCESSING_HITS = "notForProcessingHits";
    private static final String KEY_NOT_FOR_PROCESSING_MISSES = "notForProcessingMisses";
    
    

    public static void setRepositoryContext(RepositoryContext repositoryContext) {
        BaseContext ctx = BaseContext.getContext();
        ctx.setAttribute(RepositoryContext.class.getName(), repositoryContext);
    }
    

    public static RepositoryContext getRepositoryContext() {
        BaseContext ctx = BaseContext.getContext();
        RepositoryContext repositoryContext = (RepositoryContext)
            ctx.getAttribute(RepositoryContext.class.getName());
        return repositoryContext;
    }
    
    
    public void clear() {
        this.tokenMap = new HashMap();
    }


    private Map getTokenMap(String token) {
        Map tokenMap = (Map) this.tokenMap.get(token);
        if (tokenMap == null) {
            tokenMap = new HashMap();
            tokenMap.put(KEY_FOR_PROCESSING_HITS, new HashMap());
            tokenMap.put(KEY_FOR_PROCESSING_MISSES, new HashMap());
            tokenMap.put(KEY_NOT_FOR_PROCESSING_HITS, new HashMap());
            tokenMap.put(KEY_NOT_FOR_PROCESSING_MISSES, new HashMap());
            this.tokenMap.put(token, tokenMap);
        }
        return tokenMap;
    }
    

    public void addResourceHit(String token, Resource resource,
                               boolean forProcessing) {
        Map tokenMap = getTokenMap(token);
        if (forProcessing) {
            Map forProcessingHits = (Map) tokenMap.get(KEY_FOR_PROCESSING_HITS);
            Map forProcessingMisses = (Map) tokenMap.get(KEY_FOR_PROCESSING_MISSES);
            forProcessingHits.put(resource.getURI(), resource);
            forProcessingMisses.remove(resource.getURI());
        } else {
            Map notForProcessingHits = (Map) tokenMap.get(KEY_NOT_FOR_PROCESSING_HITS);
            Map notForProcessingMisses = (Map) tokenMap.get(KEY_NOT_FOR_PROCESSING_MISSES);
            notForProcessingHits.put(resource.getURI(), resource);
            notForProcessingMisses.remove(resource.getURI());
        }
    }
    
    public Resource getResourceHit(String token, String uri,
                                   boolean forProcessing) {
        Map tokenMap = getTokenMap(token);
        if (forProcessing) {
            Map forProcessingHits = (Map) tokenMap.get(KEY_FOR_PROCESSING_HITS);
            return (Resource) forProcessingHits.get(uri);
        }
        Map notForProcessingHits = (Map) tokenMap.get(KEY_NOT_FOR_PROCESSING_HITS);
        return (Resource) notForProcessingHits.get(uri);
    }
    

    public void addResourceMiss(String token, String uri, Throwable throwable,
                                boolean forProcessing) {
        Map tokenMap = getTokenMap(token);
        if (forProcessing) {
            Map forProcessingMisses = (Map) tokenMap.get(KEY_FOR_PROCESSING_MISSES);
            Map forProcessingHits = (Map) tokenMap.get(KEY_FOR_PROCESSING_HITS);
            forProcessingMisses.put(uri, throwable);
            forProcessingHits.remove(uri);
        } else {
            Map notForProcessingMisses = (Map) tokenMap.get(KEY_NOT_FOR_PROCESSING_MISSES);
            Map notForProcessingHits = (Map) tokenMap.get(KEY_NOT_FOR_PROCESSING_HITS);
            notForProcessingMisses.put(uri, throwable);
            notForProcessingHits.remove(uri);
        }
    }
    
    public Throwable getResourceMiss(String token, String uri,
                                     boolean forProcessing) {
        Map tokenMap = getTokenMap(token);
        if (forProcessing) {
            Map forProcessingMisses = (Map) tokenMap.get(KEY_FOR_PROCESSING_MISSES);
            return (Throwable) forProcessingMisses.get(uri);
        }
        Map notForProcessingMisses = (Map) tokenMap.get(KEY_NOT_FOR_PROCESSING_MISSES);
        return (Throwable) notForProcessingMisses.get(uri);
    }
    
}
