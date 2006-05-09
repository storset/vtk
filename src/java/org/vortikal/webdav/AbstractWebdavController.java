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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Namespace;
import org.springframework.web.servlet.mvc.Controller;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.ResourceLockedException;
import org.vortikal.webdav.ifheader.IfHeader;
import org.vortikal.webdav.ifheader.IfHeaderImpl;




/**
 * The superclass of all the WebDAV method controllers.
 */
public abstract class AbstractWebdavController implements Controller {

    protected Log logger = LogFactory.getLog(this.getClass());

    /**
     * If true: Only use ifheaders when refreshing locks
     */ 
    protected boolean ignoreIfHeaderVerify = false;
    
    protected IfHeader ifHeader;

    protected Repository repository = null;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
    
    /**
     * The standard WebDAV XML properties supported by the WebDAV
     * controllers.
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

        
    protected void verifyIfHeader(Resource resource, boolean ifHeaderRequiredIfLocked) {
        if (ignoreIfHeaderVerify) {
            return;
        }
        logger.debug("resource.getLock(): " + resource.getLock());
        logger.debug("ifHeader.hasTokens(): " + ifHeader.hasTokens());
        if (ifHeaderRequiredIfLocked) {
            if (resource.getLock() != null && !ifHeader.hasTokens()) {
                logger.debug("resource locked and if-header hasn't any locktokens");
                throw new ResourceLockedException();
            }
        }
        if (!matchesIfHeader(resource, true)) {
            logger.debug("verifyIfHeader: matchesIfHeader false");
            throw new ResourceLockedException();
        } else {
            logger.debug("verifyIfHeader: matchesIfHeader true");
        }
    }
  
    protected boolean matchesIfHeader(Resource resource, boolean shouldMatchOnNoIfHeader) {
        if (ifHeader == null) {
            return shouldMatchOnNoIfHeader;
        }
        return ifHeader.matches(resource, shouldMatchOnNoIfHeader);
    }
    
//    protected boolean matchesIfHeaderEtags(Resource resource, boolean shouldMatchOnNoIfHeader) {
//        if (ignoreIfHeader) {
//            return true;
//        }
//        if (ifHeader == null) {
//            return shouldMatchOnNoIfHeader;
//        }
//        return ifHeader.matchesEtags(resource, shouldMatchOnNoIfHeader);
//    }
//   
}

