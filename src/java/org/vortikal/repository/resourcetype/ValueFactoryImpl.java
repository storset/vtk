/* Copyright (c) 2006, University of Oslo, Norway
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;
import org.vortikal.security.PrincipalFactory;

/**
 * Implementation for interface <code>ValueFactory</code>.
 * 
 */
public class ValueFactoryImpl implements ValueFactory, InitializingBean {

    private PrincipalFactory principalFactory;
    
    private List dateFormats;
    
    protected Log logger = LogFactory.getLog(this.getClass());

    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (this.principalFactory == null) {
            throw new BeanInitializationException("Property 'principalFactory' not set.");
        }
        if (this.dateFormats == null || this.dateFormats.size() == 0) {
            throw new BeanInitializationException(
                    "Property 'dateformats' not set or empty list.");
        }
    }
    
    public Value[] createValues(String[] stringValues, int type) 
        throws ValueFormatException {
        
        if (stringValues == null) {
            throw new IllegalArgumentException("stringValues cannot be null.");
        }

        Value[] values = new Value[stringValues.length];
        for (int i=0; i<values.length; i++) {
            values[i] = createValue(stringValues[i], type);
        }
        
        return values;
        
    }
    
    public Value createValue(String stringValue, int type)
            throws ValueFormatException {
        
        if (stringValue == null) {
            throw new IllegalArgumentException("stringValue cannot be null");
        }
        
        switch (type) {
        
        case PropertyType.TYPE_STRING:
            if ("".equals(stringValue.trim())) {
                throw new ValueFormatException(
                    "Illegal string value: '" + stringValue + "'");
            }
            return new Value(stringValue);

        case PropertyType.TYPE_BOOLEAN:
            return new Value("true".equalsIgnoreCase(stringValue));

        case PropertyType.TYPE_DATE:
            // old: Dates are represented as number of milliseconds since January 1, 1970, 00:00:00 GMT
            // Dates are represented as described in the configuration for this bean in the List stringFormats
            Date date = getDateFromStringValue(stringValue);
            return new Value(date);
            
        case PropertyType.TYPE_INT:
            try {
                return new Value(Integer.parseInt(stringValue));
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(nfe.getMessage());
            }

        case PropertyType.TYPE_LONG:
            try {
                return new Value(Long.parseLong(stringValue));
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(nfe.getMessage());
            }
            
        case PropertyType.TYPE_PRINCIPAL:
            try {
                Principal principal = this.principalFactory.getUserPrincipal(stringValue);
                return new Value(principal);
            } catch (InvalidPrincipalException e) {
                throw new ValueFormatException(e.getMessage(), e);
            }
        }
                    
        throw new IllegalArgumentException("Cannot convert '" + stringValue 
                + "' to unknown type '" + type+ "'");
            
    }

    private Date getDateFromStringValue(String stringValue) throws ValueFormatException {
        
        try {
            return new Date(Long.parseLong(stringValue));
        } catch (NumberFormatException nfe) {}
        
        SimpleDateFormat format;
        Date date;
        for (Iterator iter = this.dateFormats.iterator(); iter.hasNext(); ) {
            String dateFormat = (String) iter.next();
            format = new SimpleDateFormat(dateFormat);
            format.setLenient(false);
            try {
                date = format.parse(stringValue);
                return date;
            } catch (ParseException e) {
                this.logger.debug("Failed to parse date using format '" + dateFormat
                                  + "', input '" + stringValue + "'", e);
            }
        }
        throw new ValueFormatException(
            "Unable to parse date value for input string: '" + stringValue + "'");
    }

    public void setDateFormats(List dateformats) {
        this.dateFormats = dateformats;
    }

    public void setPrincipalFactory(PrincipalFactory principalFactory) {
        this.principalFactory = principalFactory;
    }

}
