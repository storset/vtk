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
package org.vortikal.xml;


import java.io.IOException;
import java.util.Date;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;


/**
 * StylesheetResolver interface. The responsibility of the stylesheet
 * resolver is to map between abstract stylesheet references, such as
 * URLs, and physical resources.
 *
 * <p>In addition to the standard URI resolver method, this interface
 * defines a <code>matches()</code> method, to be able to chain
 * several resolvers operating on different type of abstract paths.
 *
 * <p>Also, in order to achieve caching of compiled templates, a
 * <code>getLastModified()</code> is defined.
 *
 * @version $Id$
 */
public interface StylesheetURIResolver extends URIResolver {


    /**
     * Resolves a URI.
     *
     * @param href An href attribute, which may be relative or
     * absolute.
     * @param base The base URI in effect when the href attribute was
     * encountered.
     * @return a <code>Source</code>
     * @exception TransformerException if an error occurs
     */
    public Source resolve(String href, String base) throws TransformerException;
    


    /**
     * Decides whether this stylesheet resolver recognizes a given
     * stylesheet identifier.
     *
     * @param stylesheetIdentifier the identifier to match
     * @return a <code>boolean</code>
     */
    public boolean matches(String stylesheetIdentifier);



    /**
     * Gets the last modified date of a stylesheet resource having a
     * given identifier.
     *
     * @param stylesheetIdentifier the identifier of the stylesheet
     * @return a <code>Date</code>, representing the last modified
     * value of the stylesheet, or <code>null</code> if this resolver
     * does not recognize the identifier.
     * @exception IOException if an error occurs
     */
    public Date getLastModified(String stylesheetIdentifier)
        throws IOException;
    
        
}
