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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.vortikal.util.web.HttpUtil;

/**
 * Utility class defining a set of constants that are convenient when
 * building MVC models, plus additional utility methods.
 *
 * @version $Id: VortexResponse.java,v 1.6 2004/03/16 20:27:08 storset Exp $
 */
public class WebdavConstants {

    public static final String HTTP_VERSION_USED = "1.1";


    public static final String WEBDAVMODEL_REQUESTED_RESOURCE = "resource";

    public static final String WEBDAVMODEL_REQUESTED_RESOURCES = "resources";

    public static final String WEBDAVMODEL_REQUESTED_PROPERTIES =
        "requestedProperties";

    public static final String WEBDAVMODEL_REQUESTED_PROPERTIES_APPEND_VALUES = 
        "appendValuesToRequestedProperties";

    public static final String WEBDAVMODEL_CREATED_RESOURCE = "createdResource";

    public static final String WEBDAVMODEL_RESOURCE_STREAM = "resourceStream";

    public static final String WEBDAVMODEL_ERROR = "errorObject";

    public static final String WEBDAVMODEL_HTTP_STATUS_CODE = "httpStatusCode";

    public static final String WEBDAVMODEL_HTTP_MULTI_STATUS_CODE =
        "httpMultiStatusCode";
    

    /**
     * Gets the HTTP "status message" for the status codes defined in
     * this class, i.e. <code>SC_MULTI_STATUS</code> will map to
     * <code>207 Multi-Status</code>, etc.
     *
     * @param statusCode an <code>int</code> value
     * @return a <code>String</code>
     */
    public static String getStatusMessage(int statusCode) {

        String message = "HTTP/" + HTTP_VERSION_USED + " " + 
            String.valueOf(statusCode) + " "
            + HttpUtil.getStatusMessage(statusCode);
        return message;
    }




    /**
     * The default XML namespace used in all elements of WebDAV
     * request/response bodies.
     *
     */
    public static final Namespace DAV_NAMESPACE =
        Namespace.getNamespace("d", "DAV:");



    /**
     * Describe <code>buildLockDiscovery</code> method here.
     *
     * @param content a <code>String</code> value
     * @return an <code>Element</code>
     * @deprecated Should be in LockRenderer
     */
    public static Element buildLockOwnerElement(String content) {
        Element ownerElement = new Element("owner", DAV_NAMESPACE);
        
        try {
            if (!content.startsWith("<")) {
                // Simple content:
                ownerElement.addContent(content);

            } else {
                // XML content:
                String xmlContent =
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + content;
                SAXBuilder builder = new SAXBuilder();
                org.jdom.Document doc = builder.build(
                    new ByteArrayInputStream(xmlContent.getBytes()));

                Element rootElement = doc.getRootElement();

                rootElement.setNamespace(DAV_NAMESPACE);

                ownerElement.addContent(rootElement);
            }
            
        } catch (RuntimeException e) {
            // FIXME:
            ownerElement.addContent(content);

        } catch (JDOMException e) {
            // FIXME:
            ownerElement.addContent(content);

        } catch (IOException e) {
            // FIXME:
            ownerElement.addContent(content);
        } 
        return ownerElement;
    }
}
