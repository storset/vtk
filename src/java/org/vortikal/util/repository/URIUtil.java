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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.Resource;


/**
 * General resource path utilities.
 */
public class URIUtil {


    private static Log logger = LogFactory.getLog("org.vortikal.util.repository.URIUtil");

    
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


    /**
     * Expands '../' strings in resource URIs. 
     * 
     * TODO: follow the specification
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
            System.out.print("URI cannot be relative.");            
        }

        if (uri.startsWith("/../")) {
            throw new InvalidURIException(
                "URI '" + uri + "' cannot be expanded: Too many '../'s");
        }

        int firstPos = uri.indexOf("../");
        if (firstPos == -1) {
            return uri;
        }
        String base = uri.substring(0, firstPos - 1);
        base = base.substring(0, base.lastIndexOf("/") + 1);
        uri = base + uri.substring(firstPos + "../".length());
        return expandPath(uri);
    }
    


}
