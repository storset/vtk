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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Namespace;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Path;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.webdav.ifheader.IfHeader;


/**
 * The superclass of all the WebDAV method controllers.
 *
 * <p>Configurable JavaBean properties:
 * <ul>
 *   <li><code>repository</code> - the {@link Repository content
 *   repository}
 *   <li><code>ignoreIfHeaderVerify</code> - If true: Only use
 *   ifheaders when refreshing locks. Default is <code>false</code>.
 * </ul>
 */
public abstract class AbstractWebdavController implements Controller {

    protected Log logger = LogFactory.getLog(this.getClass());
    private List<Pattern> deniedFileNamePatterns = new ArrayList<Pattern>();
    protected boolean supportIfHeaders = true;
    protected IfHeader ifHeader;
    protected Repository repository = null;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    public void setSupportIfHeaders(boolean supportIfHeaders) {
        this.supportIfHeaders = supportIfHeaders;
    }
    
    public void setDeniedFileNames(List<String> deniedFileNames) {
        if (deniedFileNames == null) {
            throw new IllegalArgumentException("Argument cannot be NULL");
        }
        this.deniedFileNamePatterns = new ArrayList<Pattern>();
        for (String patternStr: deniedFileNames) {
            Pattern p = Pattern.compile(patternStr);
            this.deniedFileNamePatterns.add(p);
        }
    }

    
    /**
     * Maps WebDAV property names to resource property names
     */
    public static final Map<String, String> MAPPED_DAV_PROPERTIES;

    /**
     * The set of WebDAV property names that do not map directly to
     * resource propery names
     */
    public static final Set<String> SPECIAL_DAV_PROPERTIES;


    /**
     * The set of all WebDAV property names
     */
    public static final Set<String> DAV_PROPERTIES;


    static {
        MAPPED_DAV_PROPERTIES = new HashMap<String, String>();
        MAPPED_DAV_PROPERTIES.put("creationdate", PropertyType.CREATIONTIME_PROP_NAME);
        MAPPED_DAV_PROPERTIES.put("getcontentlanguage", PropertyType.CONTENTLOCALE_PROP_NAME);
        MAPPED_DAV_PROPERTIES.put("getcontentlength", PropertyType.CONTENTLENGTH_PROP_NAME);
        MAPPED_DAV_PROPERTIES.put("getcontenttype", PropertyType.CONTENTTYPE_PROP_NAME);
        MAPPED_DAV_PROPERTIES.put("getlastmodified", PropertyType.LASTMODIFIED_PROP_NAME);

        SPECIAL_DAV_PROPERTIES = new HashSet<String>();
        SPECIAL_DAV_PROPERTIES.add("getetag");
        SPECIAL_DAV_PROPERTIES.add("lockdiscovery");
        SPECIAL_DAV_PROPERTIES.add("resourcetype");
        SPECIAL_DAV_PROPERTIES.add("displayname");
        SPECIAL_DAV_PROPERTIES.add("source");
        SPECIAL_DAV_PROPERTIES.add("supportedlock");

        DAV_PROPERTIES = new HashSet<String>();
        DAV_PROPERTIES.addAll(MAPPED_DAV_PROPERTIES.keySet());
        DAV_PROPERTIES.addAll(SPECIAL_DAV_PROPERTIES);
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
        return DAV_PROPERTIES.contains(propertyName);
    }
    

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

        
    protected void verifyIfHeader(Resource resource, boolean ifHeaderRequiredIfLocked) {
        if (!this.supportIfHeaders) {
            return;
        }
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("resource.getLock(): " + resource.getLock());
            this.logger.debug("ifHeader.hasTokens(): " + this.ifHeader.hasTokens());
        }

        if (ifHeaderRequiredIfLocked) {
            if (resource.getLock() != null && !this.ifHeader.hasTokens()) {
                String msg = "Resource " + resource + " is locked and "
                    + "If-header does not have any lock tokens";
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug(msg);
                }
                throw new ResourceLockedException(msg);
            }
        }
        if (!matchesIfHeader(resource, true)) {
            String msg = "If-header did not match resource " + resource;
            if (this.logger.isDebugEnabled()) {
                this.logger.debug(msg);
            }
            throw new ResourceLockedException(msg);
        }
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("verifyIfHeader: matchesIfHeader true");
        }
    }
  
    protected boolean matchesIfHeader(Resource resource, boolean shouldMatchOnNoIfHeader) {
        if (this.ifHeader == null) {
            return shouldMatchOnNoIfHeader;
        }
        return this.ifHeader.matches(resource, shouldMatchOnNoIfHeader);
    }
    
    protected boolean allowedResourceName(Path uri) {
        String name = uri.getName();
        for (Pattern pattern: this.deniedFileNamePatterns) {
            Matcher m = pattern.matcher(name);
            if (m.find()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Denying file name creation: '" + name + "'");
                }
                return false;
            }
        }
        return true;
    }
    

}

