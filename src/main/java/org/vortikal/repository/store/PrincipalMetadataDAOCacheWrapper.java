/* Copyright (c) 2009, University of Oslo, Norway
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
package org.vortikal.repository.store;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.security.Principal;
import org.vortikal.util.cache.SimpleCache;
import org.vortikal.util.cache.SimpleCacheImpl;

/**
 * Wraps another <code>PrincipalMetadataDAO</code> and provides simple result
 * caching for methods {@link #getMetadata(String)} and
 * {@link #getMetadata(Principal)}.
 * 
 */
public class PrincipalMetadataDAOCacheWrapper implements PrincipalMetadataDAO, InitializingBean {

    private PrincipalMetadataDAO wrappedDao;
    private SimpleCache<String, CacheItem> cache;
    private int timeoutSeconds = 60;

    public void afterPropertiesSet() throws Exception {
        SimpleCacheImpl<String, CacheItem> cacheImpl = new SimpleCacheImpl<String, CacheItem>(this.timeoutSeconds);
        cacheImpl.setRefreshTimestampOnGet(false);
        this.cache = cacheImpl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vortikal.repository.store.PrincipalMetadataDAO#getMetadata(org.vortikal
     * .security.Principal)
     */
    public PrincipalMetadata getMetadata(Principal principal, Locale preferredLocale) {
        if (principal == null) {
            throw new IllegalArgumentException("Principal cannot be null");
        }

        return getMetadata(principal.getQualifiedName(), preferredLocale);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vortikal.repository.store.PrincipalMetadataDAO#getMetadata(java.lang
     * .String)
     */
    public PrincipalMetadata getMetadata(String qualifiedName, Locale preferredLocale) {
        if (qualifiedName == null) {
            throw new IllegalArgumentException("Qualified name cannot be null");
        }

        String cacheKey = qualifiedName;
        if (preferredLocale != null) {
            cacheKey = qualifiedName.concat(preferredLocale.toString());
        }
        CacheItem item = this.cache.get(cacheKey);
        if (item != null) {
            return item.value;
        }

        // Ignore any unchecked exceptions, let them propagate to caller.
        PrincipalMetadata result = this.wrappedDao.getMetadata(qualifiedName, preferredLocale);

        // Note: will also cache null-results
        // (indicates that metadata is unavailable for the principal).
        this.cache.put(cacheKey, new CacheItem(result));

        return result;
    }

    @Override
    public List<PrincipalMetadata> search(PrincipalSearch search, Locale preferredLocale) {
        return this.wrappedDao.search(search, preferredLocale);
    }

    @Override
    public Set<String> getSupportedPrincipalDomains() {
        return this.wrappedDao.getSupportedPrincipalDomains();
    }

    @Override
    public List<PrincipalMetadata> listPrincipalsInUnit(String areacodeOrDn, Locale preferredLocale) {
        return this.wrappedDao.listPrincipalsInUnit(areacodeOrDn, preferredLocale);
    }

    @Override
    public List<PrincipalMetadata> listPrincipalsInUnitXX(String areacodeOrDn, Locale preferredLocale,
            boolean considerSubUnits) {
        return this.wrappedDao.listPrincipalsInUnitXX(areacodeOrDn, preferredLocale, considerSubUnits);
    }

    private static final class CacheItem {
        PrincipalMetadata value;

        CacheItem(PrincipalMetadata value) {
            this.value = value;
        }
    }

    @Required
    public void setWrappedDao(PrincipalMetadataDAO wrappedDao) {
        this.wrappedDao = wrappedDao;
    }

    /**
     * Default cache item expiry time in seconds.
     * 
     * Default value is 60. Increase if wrapped data source is slow (LDAP is NOT
     * slow).
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds < 1) {
            throw new IllegalArgumentException("Timeout seconds cannot be less than 1");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

}
