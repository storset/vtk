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
package org.vortikal.webdav;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.vortikal.repository.Repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Namespace;
import org.springframework.web.servlet.mvc.Controller;




/**
 * The superclass of all the WebDAV method controllers.
 */
public abstract class AbstractWebdavController implements Controller {

    protected Log logger = LogFactory.getLog(this.getClass());


    protected Repository repository = null;


    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    


    /**
     * The standard WebDAV XML properties supported by the Vortex
     * servlet.
     *
     */
    protected static final List davProperties;

    static {
        davProperties = new ArrayList();
        davProperties.add("creationdate");
        davProperties.add("displayname");
        davProperties.add("getcontentlanguage");
        davProperties.add("getcontentlength");
        davProperties.add("getcontenttype");
        davProperties.add("getetag");
        davProperties.add("getlastmodified");
        davProperties.add("lockdiscovery");
        davProperties.add("resourcetype");
        davProperties.add("source");
        davProperties.add("supportedlock");

        // TESTING MS
        davProperties.add("iscollection");

        //davProperties.add("supported-privilege-set");
        //davProperties.add("current-user-privilege-set");
    }
   

    /**
     * Determines whether a DAV property name is supported.
     *
     * @param propertyName the name to check
     * @return <code>true</code> if the property is recognized,
     * <code>false</code> otherwise
     */
    protected boolean isSupportedProperty(
        String propertyName, Namespace namespace) {

        if (!WebdavConstants.DAV_NAMESPACE.equals(namespace)) {
            return true;
        }

        // We don't protect any other namespace than "DAV:", even
        // though some of the UIO properties are interpreted/live.

        for (Iterator iter = davProperties.iterator(); iter.hasNext();) {
            String property = (String) iter.next();
            if (property.toLowerCase().equals(propertyName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    

    /**
     * @deprecated Will be moved into the mapping framework
     */
    public String mapToResourceURI(String url) {
        String prefix = "";//getPrefix();

        if (url.startsWith("/")) {
            return url;
        }

        String uri = url;
        
        if (uri.indexOf("//") >= 0) {
            uri = uri.substring(uri.indexOf("//") + "//".length(), uri.length());
        }

        if (uri.indexOf("/") > 0) {
            uri = uri.substring(uri.indexOf("/"), uri.length());
        }

        if (uri.indexOf(prefix) == 0) {
            uri = uri.substring(uri.indexOf(prefix) + prefix.length(),
                                uri.length());
        }
        
        return uri;
    }





    /**
     * Parses the WebDAV "If" header.
     *
     * @param req the <code>HttpServletRequest</code>
     * @return a <code>UriState</code> representing the state tokens
     * (ETags and lock tokens) that the client is aware of on various
     * resources.
     * @throws InvalidRequestException if the lock token format is
     * invalid
     */
    protected UriState parseIfHeader(HttpServletRequest req, String uri)
        throws InvalidRequestException {

        String ifHeader = req.getHeader("If");
        if (ifHeader == null || ifHeader.trim().equals("")) {
            return new UriState(uri);
        }

        if (ifHeader.startsWith("(") && ifHeader.endsWith(")")) {
            // FIXME: must look for *several* parenthesis, not only one
            return parseIfHeaderNoTagList(uri, ifHeader);
        }
        
        if (ifHeader.startsWith("<")) {
            return parseIfHeaderTaggedList(uri, ifHeader);
        }

        return new UriState(uri);
        
    }
    


    private UriState parseIfHeaderTaggedList(String uri, String ifHeader)
        throws InvalidRequestException {
        // FIXME: implement this (see section 9.4.2 of RFC 2518)
        return new UriState(uri);
    }
    


    private UriState parseIfHeaderNoTagList(String uri, String ifHeader)
        throws InvalidRequestException {

        /* This is a "No-tag-list" production. It consists of one or
         * more "List" productions. List productions, in turn, are
         * defined by one or more, possibly negated, state tokens or
         * entity tags. */
        String list = ifHeader.substring(1, ifHeader.length() - 1);
            
        StringTokenizer tokenizer = new StringTokenizer(list);

        UriState state = new UriState(uri);

        while (tokenizer.hasMoreTokens()) {
            String s = tokenizer.nextToken();
            boolean negated = false;

            if (s.equals("Not")) {
                negated = true;

                if (! tokenizer.hasMoreTokens()) {
                    throw new InvalidRequestException(
                        "Invalid If header: " + ifHeader);
                }
                s = tokenizer.nextToken();
            }
                
            if (s.startsWith("[") && s.endsWith("]")) {
                /* This is an Etag */

                if (s.length() <= 2) {
                    throw new InvalidRequestException(
                        "Invalid If header: " + ifHeader);
                }
                    
                StateToken token = new Etag(
                    s.substring(1, s.length() - 1), negated);
                state.addToken(token);

            } else if (s.startsWith("<") && s.endsWith(">")) {
                /* This is a regular state token (i.e. a lock
                 * token): */
                    
                if (s.length() <= 2) {
                    throw new InvalidRequestException(
                        "Invalid If header: " + ifHeader);
                }
                    
                StateToken token = new LockToken(
                    s.substring(1, s.length() - 1), negated);
                state.addToken(token);

            } else {
                /* This is an unsupported token */
                throw new InvalidRequestException(
                    "Invalid If header: " + ifHeader);
            }
        }

        return state;
    }
    


    protected class UriState {
        private String uri;
        private ArrayList tokens;
        
        public UriState(String uri) {
            this.uri = uri;
            this.tokens = new ArrayList();
        }
        
        public String getURI() {
            return this.uri;
        }

        public void addToken(StateToken token) {
            tokens.add(token);
        }

        public List getTokens() {
            return this.tokens;
        }
    }
    


    protected abstract class StateToken {
        public abstract String getValue();
        public abstract boolean isNegated();
    }
    



    protected class Etag extends StateToken {
        private String etag;
        private boolean negated;
        
        public Etag(String etag, boolean negated) {
            this.etag = etag;
            this.negated = negated;
        }

        public String getValue() {
            return this.etag;
        }
        
        public boolean isNegated() {
            return this.negated;
        }
    }
    


    protected class LockToken extends StateToken {
        private String lockToken;
        private boolean negated;

        public LockToken(String lockToken, boolean negated) {
            this.lockToken = lockToken;
            this.negated = negated;
        }

        public String getValue() {
            return this.lockToken;
        }

        public boolean isNegated() {
            return this.negated;
        }

    }
}

