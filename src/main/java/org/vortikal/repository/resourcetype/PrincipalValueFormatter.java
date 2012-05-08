/* Copyright (c) 2007, University of Oslo, Norway
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
package org.vortikal.repository.resourcetype;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.repository.store.Metadata;
import org.vortikal.repository.store.PrincipalMetadata;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;
import org.vortikal.text.html.HtmlUtil;

/**
 * Value formatter for {@link Principal} values.
 * <p>
 * Principals can be formatted to string in the following formats:
 * <ul><li>default: {@link Principal#getName()}
 * <li>'name': {@link Principal#getDescription()}
 * <li>'link' and {@link Principal} has url: &lt;a href="{@link Principal#getURL()}">{@link Principal#getDescription()}&lt;/a>
 * <li>'name-link' and {@link Principal} has url: &lt;a href="{@link Principal#getURL()}">{@link Principal#getName()}&lt;/a>
 * <li>'document-link' and {@link Principal} has url to a person document, if it exists: &lt;a href="{@link Principal#getURL()}">{@link Principal#getDescription()}&lt;/a>
 */
public class PrincipalValueFormatter implements ValueFormatter {

    // Possible formats for principal objects
    public static final String NAME_FORMAT = "name";
    public static final String LINK_FORMAT = "link";
    public static final String NAME_LINK_FORMAT = "name-link";
    public static final String DOCUMENT_LINK_FORMAT = "document-link";

    private PrincipalFactory principalFactory;

    /*
     * Defaults to return the principal description. Also supports the "name",
     * "link", "name-link" and "document-link" formats, the "link" and
     * "name-link" latter returning an html <a> tag if the principal has a url,
     * and the "document-link" returning an html <a> tag if the principal has an
     * existing document (defaults to "link" format if not).
     */
    public String valueToString(Value value, String format, Locale locale) throws IllegalValueTypeException {

        if (value.getType() != Type.PRINCIPAL) {
            throw new IllegalValueTypeException(Type.PRINCIPAL, value.getType());
        }

        Principal principal = value.getPrincipalValue();

        // Will only yield results id principal factory is configured for
        // document search
        if (DOCUMENT_LINK_FORMAT.equals(format)) {
            Principal principalDocument = this.principalFactory.getPrincipalDocument(principal.getQualifiedName(),
                    locale);
            if (principalDocument != null) {
                PrincipalMetadata pm = principalDocument.getMetadata();
                Object urlObj = pm.getValue(Metadata.URL_ATTRIBUTE);
                if (urlObj != null) {
                    StringBuilder sb = new StringBuilder("<a href=\"");
                    sb.append(HtmlUtil.escapeHtmlString(urlObj.toString()));
                    sb.append("\">");
                    sb.append(principal.getDescription());
                    sb.append("</a>");
                    return sb.toString();
                }
            }
            // If no document is found, fall back to LINK_FORMAT
            format = LINK_FORMAT;
        }

        String url = principal.getURL();
        if (LINK_FORMAT.equals(format) && url != null) {
            return "<a href=\"" + HtmlUtil.escapeHtmlString(url) + "\">" + principal.getDescription() + "</a>";
        }

        if (NAME_LINK_FORMAT.equals(format) && url != null) {
            return "<a href=\"" + HtmlUtil.escapeHtmlString(url) + "\">" + principal.getName() + "</a>";
        }

        if (NAME_FORMAT.equals(format)) {
            return principal.getDescription();
        }

        return principal.getName();
    }

    public Value stringToValue(String string, String format, Locale locale) throws InvalidPrincipalException {
        Principal principal = principalFactory.getPrincipal(string, Principal.Type.USER);
        return new Value(principal);
    }

    @Required
    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}
