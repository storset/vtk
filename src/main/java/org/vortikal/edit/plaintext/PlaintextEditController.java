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
package org.vortikal.edit.plaintext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.resourcetype.PropertyTypeDefinition;
import org.vortikal.security.Principal;
import org.vortikal.security.SecurityContext;
import org.vortikal.util.io.StreamUtil;
import org.vortikal.util.repository.ContentTypeHelper;
import org.vortikal.util.repository.TextResourceContentHelper;
import org.vortikal.util.text.HtmlUtil;
import org.vortikal.web.RequestContext;
import org.vortikal.web.service.Service;
import org.vortikal.web.service.ServiceUnlinkableException;
import java.util.HashMap;

/**
 * Controller that handles editing of plaintext resource content.
 *
 * <h3>A note on character encoding problems:</h3>
 *
 * If the resource is XML/HTML, there are some considerations: First,
 * the resource may have an encoding set in the repository (either
 * explicitly set using the 'contentType' property or inside the
 * document itself). This is called the 'stored encoding'. In addition
 * to that, there is the content that is posted from the form. This
 * content is assumed to be posted using UTF-8, producing a valid
 * String inside the Java code. The problem arises when the posted
 * content contains a charset declaration and that declaration differs
 * from the stored encoding. In such cases, the stored encoding should
 * be altered to reflect that of the posted content (in a best-effort
 * fashion).
 *
 * <p>Configurable JavaBean properties
 * (and those defined by {@link SimpleFormController superclass}):
 * <ul>
 *   <li><code>repository</code> - the content {@link Repository
 *   repository} (required)
 *   <li><code>cancelView</code> - the {@link String view name} to return
 *   when user (required) cancels the operation
 *   <li><code>lockTimeoutSeconds</code> - the number of seconds for
 *   which to request lock timeouts on every request (default is 300)
 *  <li><code>defaultCharacterEncoding</code> - defaults to
 *  <code>utf-8</code>, which encoding to enterpret the supplied
 *  resource content in, if unable to guess.
 * </ul>
 */
public class PlaintextEditController extends SimpleFormController
  implements InitializingBean {

    private PropertyTypeDefinition updateEncodingProperty;
    
    private Log logger = LogFactory.getLog(this.getClass().getName());
    
    private String cancelView;
    private Repository repository;
    private int lockTimeoutSeconds = 300;

    private String defaultCharacterEncoding = "utf-8";
    private TextResourceContentHelper textResourceContentHelper;
    
    private Service[] tooltipServices;
    

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setLockTimeoutSeconds(int lockTimeoutSeconds) {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    public void setCancelView(String cancelView) {
        this.cancelView = cancelView;
    }
    
    public void setDefaultCharacterEncoding(String defaultCharacterEncoding) {
        this.defaultCharacterEncoding = defaultCharacterEncoding;
    }

    public void setUpdateEncodingProperty(PropertyTypeDefinition updateEncodingProperty) {
        this.updateEncodingProperty = updateEncodingProperty;
    }
    
    public void setTooltipServices(Service[] tooltipServices) {
        this.tooltipServices = tooltipServices;
    }
    
    
    public void afterPropertiesSet() {
        if (this.repository == null) {
            throw new BeanInitializationException(
                "Bean property 'repository' must be set");
        }
        if (this.cancelView == null) {
            throw new BeanInitializationException(
                "Bean property 'cancelView' must be set");
        }

        this.textResourceContentHelper = new TextResourceContentHelper(
            this.repository, this.defaultCharacterEncoding);
    }
    


    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        
        this.repository.lock(token, uri, principal.getQualifiedName(), "0",
                        this.lockTimeoutSeconds, null);

        Resource resource = this.repository.retrieve(token, uri, false);
        String url = service.constructLink(resource, principal);
        String characterEncoding = resource.getCharacterEncoding();
        String content = getTextualContent(resource, token);
        
        List tooltips = resolveTooltips(resource, principal);
        return new PlaintextEditCommand(content, url, tooltips);
    }



    protected ModelAndView onSubmit(Object command, BindException errors)
        throws Exception {

        PlaintextEditCommand plaintextEditCommand =
            (PlaintextEditCommand) command;

        if (plaintextEditCommand.getCancelAction() == null) {
            return super.onSubmit(command, errors);
        }
        
        /** The user has selected "cancel". Unlock resource, return
         *  the cancelView. */

        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        String uri = requestContext.getResourceURI();
        this.repository.unlock(token, uri, null);
        
        return new ModelAndView(this.cancelView);    
    }
    


    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        PlaintextEditCommand plaintextEditCommand = (PlaintextEditCommand) command;

        Resource resource = this.repository.retrieve(token, uri, false);
        String storedEncoding = resource.getCharacterEncoding();
        String postedEncoding = getPostedEncoding(resource, plaintextEditCommand);

        /** 
         * When storing content, it has to be written as a byte
         * sequence, produced from the posted content using an
         * encoding decided by the following:
         * 
         * 1. storedEncoding == null, postedEncoding == null --> use defaultEncoding
         * 2. storedEncoding == null, postedEncoding != null --> use postedEncoding
         * 3. storedEncoding != null, postedEncoding == null --> keep storedEncoding
         * 4. storedEncoding != null, postedEncoding != null --> use postedEncoding
         */
        String characterEncoding = this.defaultCharacterEncoding;
        boolean maybeSetEncoding = false;

        if (storedEncoding == null && postedEncoding != null) {
            characterEncoding = postedEncoding;
            maybeSetEncoding = true;

        } else if (storedEncoding != null && postedEncoding == null) {
            characterEncoding = storedEncoding;

        } else if (storedEncoding != null && postedEncoding != null) {
            characterEncoding = postedEncoding;
            maybeSetEncoding = true;
        }
        if (this.updateEncodingProperty == null) {
            maybeSetEncoding = false;
        }
        if (maybeSetEncoding) {
            if (this.logger.isDebugEnabled())
                this.logger.debug("New character encoding for document "
                             + resource + " resolved to: " + characterEncoding);

            resource.setUserSpecifiedCharacterEncoding(characterEncoding);
            this.repository.store(token, resource);
        }

        String content = plaintextEditCommand.getContent();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Decoding posted string using encoding: " + characterEncoding);
        }
        this.repository.storeContent(token, uri, 
                new ByteArrayInputStream(content.getBytes(characterEncoding)));

    }
    


    private String getTextualContent(Resource resource, String token)
        throws IOException {

        String encoding = resource.getCharacterEncoding();
        InputStream is = this.repository.getInputStream(token, resource.getURI(),
                                                   false);
        byte[] bytes = StreamUtil.readInputStream(is);
        String content = new String(bytes, encoding);
        return content;
    }



    private String getPostedEncoding(Resource resource, PlaintextEditCommand command) {
        String postedEncoding = null;
        if (ContentTypeHelper.isXMLContentType(resource.getContentType())) {

            postedEncoding = this.textResourceContentHelper.getXMLCharacterEncoding(
                command.getContent());
            
        } else if (ContentTypeHelper.isHTMLContentType(resource.getContentType())) {

            postedEncoding = HtmlUtil.getCharacterEncodingFromBody(
                command.getContent().getBytes());
        } 
        return postedEncoding;
    }
    

    private List resolveTooltips(Resource resource, Principal principal) {
        List tooltips = new ArrayList();
        if (this.tooltipServices != null) {
            for (int i = 0; i < this.tooltipServices.length; i++) {
                Service service = this.tooltipServices[i];
                String url = null;
                try {
                    url = service.constructLink(resource, principal);
                    Map tooltip = new HashMap();
                    tooltip.put("url", url);
                    tooltip.put("messageKey", "plaintextEdit.tooltip." + service.getName());

//                     Tooltip tooltip = new Tooltip(url, "plaintextEdit.tooltip."
//                                                   + service.getName());
                    tooltips.add(tooltip);
                    System.out.println("___tooltip: " + tooltip);
                } catch (ServiceUnlinkableException e) {
                    // Ignore
                    System.out.println("___tooltip: " + e);
                }
            }
        }
        return tooltips;
    }
    

    private class Tooltip {

        private String url;
        private String messageKey;
        
        public Tooltip(String url, String messageKey) {
            this.url = url;
            this.messageKey = messageKey;
        }

        public String getUrl() {
            return this.url;
        }

        public String getMessageKey() {
            return this.messageKey;
        }
    }
    

}

