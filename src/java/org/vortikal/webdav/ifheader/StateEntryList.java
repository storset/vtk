package org.vortikal.webdav.ifheader;

import org.vortikal.repository.Resource;

/**
 * The <code>IfHeaderInterface</code> interface abstracts away the difference of
 * tagged and untagged <em>If</em> header lists. The single method provided
 * by this interface is to check whether a request may be applied to a
 * resource with given token and etag.
 */
public interface StateEntryList {

    /**
     * Matches the resource, token, and etag against this
     * <code>IfHeaderInterface</code> instance.
     *
     * @param resource The resource to match this instance against. This
     *      must be absolute URI of the resource as defined in Section 3
     *      (URI Syntactic Components) of RFC 2396 Uniform Resource
     *      Identifiers (URI): Generic Syntax.
     * @param token The resource's lock token to match
     * @param etag The resource's etag to match
     *
     * @return <code>true</code> if the header matches the resource with
     *      token and etag, which means that the request is applicable
     *      to the resource according to the <em>If</em> header.
     */
    public boolean matches(Resource resource);
    
    public boolean matchesEtags(Resource resource);
    
}