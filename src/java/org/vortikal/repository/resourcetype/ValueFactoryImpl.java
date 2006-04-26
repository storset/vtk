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
import org.vortikal.security.PrincipalManager;

/**
 * Implementation for interface <code>ValueFactory</code>.
 * 
 * 
 * @author oyviste
 *
 */
public class ValueFactoryImpl implements ValueFactory, InitializingBean {

    private PrincipalManager principalManager;
    
    private List dateFormats;
    
    protected Log logger = LogFactory.getLog(this.getClass());

    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
        }
        if (dateFormats == null || dateFormats.size() < 1) {
            throw new BeanInitializationException(
                    "Property 'dateformats' not set or has not atleast one dateformat in list.");
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
        
        Value value = new Value();
        switch (type) {
        
        case PropertyType.TYPE_STRING:
            value.setValue(stringValue);
            break;
            
        case PropertyType.TYPE_BOOLEAN:
            value.setBooleanValue("true".equalsIgnoreCase(stringValue));
            break;
        
        case PropertyType.TYPE_DATE:
            // old: Dates are represented as number of milliseconds since January 1, 1970, 00:00:00 GMT
            // Dates are represented in the following format dd.MM.yyyy
            Date date = getDateFromStringValue(stringValue);
            value.setDateValue(date);
            break;
        
        case PropertyType.TYPE_INT:
            try {
                value.setIntValue(Integer.parseInt(stringValue));
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(nfe.getMessage());
            }
            break;
            
        case PropertyType.TYPE_LONG:
            try {
                value.setLongValue(Long.parseLong(stringValue));
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(nfe.getMessage());
            }
            break;
            
        case PropertyType.TYPE_PRINCIPAL:
            try {
                value.setPrincipalValue(principalManager.getUserPrincipal(stringValue));
            } catch (InvalidPrincipalException ipe) {
                throw new ValueFormatException("Unable to convert string to valid principal");
            }
            break;
            
        default:
            throw new IllegalArgumentException("Cannot convert to unknown type '" + type+ "'");
            
        }
        
        return value;
    }

    private Date getDateFromStringValue(String stringValue) throws ValueFormatException {
        SimpleDateFormat format;
        Date date;
        for (Iterator iter = dateFormats.iterator(); iter.hasNext(); ) {
            String dateFormat = (String) iter.next();
            format = new SimpleDateFormat(dateFormat);
            try {
                date = format.parse(stringValue);
                return date;
            } catch (ParseException e) {
                logger.debug("Dateformat not ok for dateformat \'" + dateFormat
                        + "\' and stringValue \'" + stringValue + "\': " + e.getMessage());
            }
        }
        if (stringValue.equals("")) {
            return null;
        } else {
            throw new ValueFormatException("Illegal date format");
        }
    }

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    public void setDateFormats(List dateformats) {
        this.dateFormats = dateformats;
    }

}
