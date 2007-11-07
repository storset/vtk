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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vortikal.repository.resourcetype.PropertyType.Type;
import org.vortikal.security.InvalidPrincipalException;
import org.vortikal.security.Principal;

/**
 * Interface for a <code>Value</code> "factory". It currently only does
 * value creation from string representation.
 */
public class ValueFactory {

    private static final ValueFactory INSTANCE = new ValueFactory();
    
    private static final String[] dateFormats = new String[] {        
        "dd.MM.yyyy HH:mm:ss",
        "dd.MM.yyyy HH:mm",
        "dd.MM.yyyy",
        "yyyy-MM-dddd HH:mm:ss",
        "yyyy-MM-dddd HH:mm",
        "yyyy-MM-dddd"
    };

    private Log logger = LogFactory.getLog(this.getClass());

    
    
    private ValueFactory() {
    }

    public static ValueFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * 
     * @param stringValues An array of String representation
     * @param type The type of the Value, see {@link PropertyType}
     * @return An array of Values 
     * @throws ValueFormatException
     */
    public Value[] createValues(String[] stringValues, Type type) 
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

    /**
     * Create a <code>Value</code> object from the given string
     * representation and type.
     * @param stringValue The String representation of the value
     * @param type The type of the Value, see {@link PropertyType}
     * @return The Value based on the stringValue and type
     * @throws ValueFormatException
     */
    public Value createValue(String stringValue, Type type)
    throws ValueFormatException {

        if (stringValue == null) {
            throw new IllegalArgumentException("stringValue cannot be null");
        }

        switch (type) {

        case STRING:
            if (stringValue.length() == 0) {
                throw new ValueFormatException("Illegal string value: empty");
            }
            return new Value(stringValue);

        case BOOLEAN:
            return new Value("true".equalsIgnoreCase(stringValue));

        case DATE:
            // old: Dates are represented as number of milliseconds since January 1, 1970, 00:00:00 GMT
            // Dates are represented as described in the configuration for this bean in the List stringFormats
            Date date = getDateFromStringValue(stringValue);
            return new Value(date);

        case INT:
            try {
                return new Value(Integer.parseInt(stringValue));
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(nfe.getMessage());
            }

        case LONG:
            try {
                return new Value(Long.parseLong(stringValue));
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(nfe.getMessage());
            }

        case PRINCIPAL:
            try {
                Principal principal = new Principal(stringValue, Principal.Type.USER);
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
        for (String dateFormat: dateFormats) {
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
}