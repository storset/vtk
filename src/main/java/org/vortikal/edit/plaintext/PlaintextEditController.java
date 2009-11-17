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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.vortikal.repository.Namespace;
import org.vortikal.repository.Path;
import org.vortikal.repository.Property;
import org.vortikal.repository.Repository;
import org.vortikal.repository.Resource;
import org.vortikal.repository.TypeInfo;
import org.vortikal.repository.Repository.Depth;
import org.vortikal.repository.resourcetype.PropertyType;
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
    
    private String manageView;
    private Repository repository;
    private int lockTimeoutSeconds = 300;

    private String defaultCharacterEncoding = "utf-8";
    private TextResourceContentHelper textResourceContentHelper;
    
    private Service[] tooltipServices;
    

    @Required
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setLockTimeoutSeconds(int lockTimeoutSeconds) {
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    @Required
    public void setManageView(String manageView) {
        this.manageView = manageView;
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
        this.textResourceContentHelper = new TextResourceContentHelper(
            this.repository, this.defaultCharacterEncoding);
    }
    


    protected Object formBackingObject(HttpServletRequest request)
        throws Exception {
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Service service = requestContext.getService();
        
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();
        Principal principal = securityContext.getPrincipal();
        
        this.repository.lock(token, uri, principal.getQualifiedName(), 
                Depth.ZERO, this.lockTimeoutSeconds, null);

        Resource resource = this.repository.retrieve(token, uri, false);
        String url = service.constructLink(resource, principal);
        String content = getTextualContent(resource, token);
        
        List<Map<String, String>> tooltips = resolveTooltips(resource, principal);
        return new PlaintextEditCommand(content, url, tooltips);
    }



    protected ModelAndView onSubmit(Object command, BindException errors)
        throws Exception {

        PlaintextEditCommand plaintextEditCommand =
            (PlaintextEditCommand) command;

        if (plaintextEditCommand.getSaveAction() != null) {
            return super.onSubmit(command, errors);
        }
        
        /** The user has selected "cancel" or "save and quit". Unlock resource, return
         *  the manage view. */
        
        if(plaintextEditCommand.getSaveQuitAction() != null) {
        	doSubmitAction(command);
        }
        
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        String token = securityContext.getToken();
        RequestContext requestContext = RequestContext.getRequestContext();
        Path uri = requestContext.getResourceURI();
        this.repository.unlock(token, uri, null);
        
        return new ModelAndView(this.manageView);    
    }
    


    protected void doSubmitAction(Object command) throws Exception {        
        RequestContext requestContext = RequestContext.getRequestContext();
        SecurityContext securityContext = SecurityContext.getSecurityContext();
        Path uri = requestContext.getResourceURI();
        String token = securityContext.getToken();

        PlaintextEditCommand plaintextEditCommand = (PlaintextEditCommand) command;

        Resource resource = this.repository.retrieve(token, uri, false);
        TypeInfo typeInfo = this.repository.getTypeInfo(token, uri);
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
        try {
            Charset.forName(characterEncoding);
        } catch (Throwable t) {
            characterEncoding = this.defaultCharacterEncoding;
        }
        
        if (this.updateEncodingProperty == null) {
            maybeSetEncoding = false;
        }
        if (maybeSetEncoding) {
            Property prop = typeInfo.createProperty(Namespace.DEFAULT_NAMESPACE, 
                    PropertyType.CHARACTERENCODING_USER_SPECIFIED_PROP_NAME);
            prop.setStringValue(characterEncoding);
            resource.addProperty(prop);
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
        throws Exception {

        String encoding = resource.getCharacterEncoding();
        try {
            Charset.forName(encoding);
        } catch (Throwable t) {
            encoding = this.defaultCharacterEncoding;
        }
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
    

    private List<Map<String, String>> resolveTooltips(Resource resource, Principal principal) {
        List<Map<String, String>> tooltips = new ArrayList<Map<String, String>>();
        if (this.tooltipServices != null) {
            for (Service service: this.tooltipServices) {
                String url = null;
                try {
                    url = service.constructLink(resource, principal);
                    Map<String, String> tooltip = new HashMap<String, String>();
                    tooltip.put("url", url);
                    tooltip.put("messageKey", "plaintextEdit.tooltip." + service.getName());
                    tooltips.add(tooltip);
                } catch (ServiceUnlinkableException e) {
                    // Ignore
                }
            }
        }
        return tooltips;
    }
}

