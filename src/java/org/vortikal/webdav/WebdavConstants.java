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

import org.jdom.Namespace;
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
    
    /** Date format used by properties containing date values (HTTP-compliant) */
    public static final String WEBDAV_PROPERTY_DATE_VALUE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
    
    /** Timezone of dates presented from properties containing date values. */
    public static final String WEBDAV_PROPERTY_DATE_VALUE_TIMEZONE = "GMT";
    
    /**
     * The default XML namespace used in all elements of WebDAV
     * request/response bodies.
     *
     */
    public static final Namespace DAV_NAMESPACE =
        Namespace.getNamespace("d", "DAV:");

    /**
     * Namespace for representation of multiple property values in XML-list-format.
     */
    public static final Namespace VORTIKAL_PROPERTYVALUES_XML_NAMESPACE =
        Namespace.getNamespace("vx", "http://vortikal.org/xml-value-list");

    /**
     * Namespace for representation of multiple property values in CSV-list-format.
     */
    public static final Namespace VORTIKAL_PROPERTYVALUES_CSV_NAMESPACE =
        Namespace.getNamespace("vc", "http://vortikal.org/csv-value-list");
    
    
//    /**
//     * Describe <code>buildLockDiscovery</code> method here.
//     *
//     * @param content a <code>String</code> value
//     * @return an <code>Element</code>
//     * @deprecated Should be in LockRenderer
//     */
//    public static Element buildLockOwnerElement(String content) {
//        Element ownerElement = new Element("owner", DAV_NAMESPACE);
//        
//        try {
//            if (!content.startsWith("<")) {
//                // Simple content:
//                ownerElement.addContent(content);
//
//            } else {
//                // XML content:
//                String xmlContent =
//                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + content;
//                SAXBuilder builder = new SAXBuilder();
//                org.jdom.Document doc = builder.build(
//                    new ByteArrayInputStream(xmlContent.getBytes()));
//
//                Element rootElement = doc.getRootElement();
//
//                rootElement.setNamespace(DAV_NAMESPACE);
//
//                ownerElement.addContent(rootElement);
//            }
//            
//        } catch (RuntimeException e) {
//            // FIXME:
//            ownerElement.addContent(content);
//
//        } catch (JDOMException e) {
//            // FIXME:
//            ownerElement.addContent(content);
//
//        } catch (IOException e) {
//            // FIXME:
//            ownerElement.addContent(content);
//        } 
//        return ownerElement;
//    }
}
