/* Copyright (c) 2005, University of Oslo, Norway
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

package org.vortikal.repositoryimpl.index.dms;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.output.DOMOutputter;
import org.vortikal.security.SecurityContext;


/**
 * 
 * @author oyviste
 */
public class DMSXslQuery {
    
    private static Log logger = LogFactory.getLog(DMSXslQuery.class);
    
    private DMSXmlQuery dmsQueryHelper;
    
    /** Creates a new instance of FolderEvaluationXslQuery */
    public DMSXslQuery(DMSXmlQuery helper) {
        this.dmsQueryHelper = helper;
    }
 

    public org.w3c.dom.NodeList executeQuery(String input) {
        return executeQuery(input, null, new Integer(-1));
    }
    

    // Handles non-numerical 'maxResults' input:
    public org.w3c.dom.NodeList executeQuery(String input, String sort,
                                             String maxResults) {
        try {
            int i = Integer.parseInt(maxResults);
            return executeQuery(input, sort, new Integer(i));
        } catch (NumberFormatException e) {
            return executeQuery(input, sort, new Integer(-1));
        }
    }
    

    public org.w3c.dom.NodeList executeQuery(String input, String sort,
                                             Number maxResults) {
        if (input == null) {
            logger.warn("Input string is null.");
            return null;
        }
        String securityToken = SecurityContext.getSecurityContext().getToken();
        Document doc = this.dmsQueryHelper.executeQuery(input, securityToken, maxResults.intValue(),
                                                   sort);
        
        return getW3CNodeList(doc);
    }
    
    private org.w3c.dom.NodeList getW3CNodeList(org.jdom.Document jdomDocument) {
        org.w3c.dom.Document domDoc = null;

        try {
            DOMOutputter oupt = new DOMOutputter();
            domDoc = oupt.output(jdomDocument);
        }
        catch (Exception e) {}
        
        return domDoc != null ? domDoc.getDocumentElement().getChildNodes() : null;
    }
}
