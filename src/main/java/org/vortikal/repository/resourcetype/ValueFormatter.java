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
import java.util.Map;

import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Required;

public class ValueFormatter {

    private FastDateFormat defaultDateFormat;
    private Map<String, FastDateFormat> namedDateFormats;

    @Required 
    public void setDefaultDateFormat(FastDateFormat defaultDateFormat) {
        this.defaultDateFormat = defaultDateFormat;
    }
    
    @Required 
    public void setNamedDateFormats(Map<String, FastDateFormat> namedDateFormats) {
        this.namedDateFormats = namedDateFormats;
    }
    

    public String valueToString(Value value, String format, Locale locale) {
        switch (value.getType()) {
            case PropertyType.TYPE_DATE:
                FastDateFormat f = null;

                if (format == null) {
                    f = this.defaultDateFormat;
                }
                    
                // Check if format refers to any of the
                // predefined (named) formats:
                if (f == null) {
                    f = this.namedDateFormats.get(format + "_" + locale.getLanguage());
                }

                if (f == null) {
                    f = this.namedDateFormats.get(format);
                }
                
                try {
                    if (f == null) {
                        // Parse the given format
                        // XXX: formatter instances should be cached
                        f = FastDateFormat.getInstance(format, locale);
                    }
                    return f.format(value.getDateValue());
                } catch (Throwable t) {
                    return "Error: " + t.getMessage();
                }

            case PropertyType.TYPE_PRINCIPAL:
                return value.getPrincipalValue().getName();
            default:
                return value.toString();
        }
    }

}
