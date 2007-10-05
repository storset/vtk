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
package org.vortikal.util.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Resource;


/**
 * General resource path utilities.
 */
public class URIUtil {
    
    private static Log logger = LogFactory.getLog(URIUtil.class);

    /**
     * Makes an absolute path of <code>path</code> relative to
     * <code>resource</code>.  If resource is a collection, the path
     * is relative to this resource, otherwise to the parent of the
     * resource.
     * @param path
     * @param resource
     * @return the absolute path
     */
    public static String makeAbsoluteURI(String path, Resource resource) {

        if (path.startsWith("/")) return path;

        if (resource == null) {
            throw new IllegalArgumentException(
                "The relative path '" + path + "' cannot be resolved with a null resource");
        };

        if (resource.isCollection()) { 
            return resource.getURI() + "/" + path; 
        }

        String parentURI = resource.getURI();
        parentURI = parentURI.substring(0, parentURI.lastIndexOf("/"));

        return parentURI + "/" + path;
    }


    public static String makeAbsoluteURI(String ref, String base) {
        if (ref.startsWith("/")) return ref;

        if (base == null || base.equals("")) {
            throw new IllegalArgumentException(
                "The relative path '" + ref + "' cannot be resolved with a null resource");
        };

        if (!base.endsWith("/")) { 
            return base + "/" + ref; 
        }

        String parentURI = base.substring(0, base.lastIndexOf("/"));

        return parentURI + "/" + ref;
        
    }


    /**
     * Finds the parent of a URI.
     * 
     * @param uri the URI for which to find the parent. Must start
     * with a <code>/</code>.
     * @return the expanded URI. If the provided URI is the root URI
     * <code>/</code>, <code>null</code> is returned.
     * @throws IllegalArgumentException if the provided URI does not
     * start with a slash, or if it contains <code>//</code>.
     */
    public static String getParentURI(String uri) {
        if (uri == null || uri.trim().equals("") || !uri.startsWith("/")) {
            throw new IllegalArgumentException("Invalid uri: '" + uri + "'");
        }

        if (uri.indexOf("//") != -1) {
            throw new IllegalArgumentException("Invalid uri: '" + uri + "'");
        }
        
        if ("/".equals(uri)) {
            return null;
        }

        if (uri.endsWith("/")) {
            throw new IllegalArgumentException("Invalid uri: '" + uri + "'");
        }

        String parentURI = uri.substring(0, uri.lastIndexOf("/"));
        if (parentURI.equals("")) {
            return "/";
        }

        return parentURI;
    }
    
    public static String getResourceName(String uri) {
        if (uri == null || uri.trim().equals("") || !uri.startsWith("/")) {
            throw new IllegalArgumentException("Invalid uri: '" + uri + "'");
        }

        if (uri.indexOf("//") != -1) {
            throw new IllegalArgumentException("Invalid uri: '" + uri + "'");
        }
        
        if ("/".equals(uri)) {
            return uri;
        }

        if (uri.endsWith("/")) {
            throw new IllegalArgumentException("Invalid uri: '" + uri + "'");
        }

        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    
    /**
     * Finds the grandparent of a URI.
     * 
     * @param uri the URI for which to find the grandparent. Must start
     * with a <code>/</code>.
     * @return the expanded URI. If the provided URI or its parent is the 
     * root URI <code>/</code>, <code>null</code> is returned.
     * @throws IllegalArgumentException is thrown by envoked method getParentURI()
     * @deprecated This method will be removed
     */
    public static String getGrandparentURI(String uri) {
        return getParentURI(getParentURI(uri));
    }
    
    /**
     * Get list of all absolute ancestor URIs for given absolute URI. 
     * The list is ordered from closest ancestor first and upwards to 
     * (and including) the '/'.
     * 
     * @param uri Absolute URI to get ancestors of.
     * @return List of all ancestor URIs for given URI.
     */
    public static List<String> getAncestorURIs(String uri) {
        List<String> ancestorUris = new ArrayList<String>();

        if (uri.equals("/")) {
            return ancestorUris;
        }
        if (uri.length() == 0 || uri.endsWith("/")) {
            throw new IllegalArgumentException("Invalid URI '" + uri + "'");
        }
        
        int from = uri.length();
        while (from > 0) {
            from = uri.lastIndexOf('/', from);
            if (from == 0) {
                ancestorUris.add("/");
            } else {
                ancestorUris.add(uri.substring(0, from--));
            }
        }
        
        return ancestorUris;
    }

    /**
     * Calculate URI depth
     * @param uri
     * @return
     */
    public static int getUriDepth(String uri) {
        if ("/".equals(uri)) {
            return 0;
        }
        int count = 0;
        for (int index = 0; (index = uri.indexOf('/', index)) != -1; count++, index ++);
        return count;
    }
    
    /**
     * Expands '../' strings in resource URIs. 
     * 
     * XXX: follow the specification
     * 
     * @param uri the URI to expand. Must start with a <code>/</code>.
     * @return the expanded URI.
     * @throws InvalidURIException if the URI to expand is invalid,
     * i.e. contains more '../'s than catalogs.
     */
    public static String expandPath(String uri) throws InvalidURIException {

        // Test to check that start of URI is legal path (e.g. UNIX '/', WINDOWS 'C:')
        // [using string test and regex checking]
        if ( !uri.startsWith("/") && (!uri.substring(0,2).matches("[c-zA-Z]:")) ) {
            // XXX: throw something?
            logger.warn("URI cannot be relative.");
        }

        if (uri.startsWith("/../")) {
            throw new InvalidURIException(
                "URI '" + uri + "' cannot be expanded: Too many '../'s");
        }

        int firstPos = uri.indexOf("../");
        if (firstPos == -1) {
            // Quickfix for trailing ../
            if (!uri.equals("/") && uri.endsWith("/"))
                return uri.substring(0, uri.length() -1);
            return uri;
        }
        String base = uri.substring(0, firstPos - 1);
        base = base.substring(0, base.lastIndexOf("/") + 1);
        uri = base + uri.substring(firstPos + "../".length());
        return expandPath(uri);
    }
    

    /** Resolve path relative to base, reworked from AbstractPathBasedURIResolver
     * @param path
     * @param base
     * @return null if path is a qualified url or base isn't an absolute path, otherwise
     * an '..'-expanded path..
     */
    public static String getAbsolutePath(String path, String base) {

        // qualified path isn't handled.
        if (path == null || path.matches(".+://.+")) return null;
        
        if (path.startsWith("/")) {
            // path starting with '/', don't care about base
        } else if (base == null || !base.startsWith("/")) {
            // Relative path needs to be resolved relative to an absolute base
            return null;
        } else {
            // Strip the name of the base resource    
            base = base.substring(0, base.lastIndexOf("/") + 1);
            
            path = base + path;
        }
        
        if (path.indexOf("../") > -1) {
            return expandPath(path);
        }

        return path;
    }
    /**
     * Strip the trailing slash from a URI, if there is any.
     * @param uri A URI String
     * @return A URI String with any trailing slash stripped
     */
    public static String stripTrailingSlash(String uri) {
        if (uri.length() > 1 && uri.endsWith("/")) {
            return uri.substring(0, uri.length()-1);
        }
        return uri;
    }

    public static boolean isUrl(String string) {
        if (string == null) {
            return false;
        }
        
        return string.contains("://");
    }


    
    /**
     * Verify whether a given string is escaped or not
     *
     * @param original given characters
     * @return true if the given character array is 7 bit ASCII-compatible.
     */
    public static boolean isEscaped(String url) {
        char[] original = url.toCharArray();
        for (int i = 0; i < original.length; i++) {
            int c = original[i];
            if (c > 128) {
                return false;
            } else if (c == ' ') {
                return false;
            } else if (c == '%') {
                if (Character.digit(original[++i], 16) == -1 
                        || Character.digit(original[++i], 16) == -1) {
                    return false;
                }
            }
        }             
        
        return true;
    }

    public static String getAncestorOrSelfAtLevel(String uri, int uriLevel) {
        if (uriLevel < 0) {
            throw new IllegalArgumentException("Uri level must be a positive integer");
        } 

        if (uriLevel == 0) {
            return "/";
        }
        
        for (int i = 0; i < uri.length(); i++) {
            if (uri.charAt(i) == '/' && --uriLevel == -1)
                    return uri.substring(0, i);
        }
      
        if (uriLevel > 0) {
            throw new IllegalArgumentException("Uri level cannot be larger than the level of the supplied uri");
        }
        return uri;
    }


    
}
