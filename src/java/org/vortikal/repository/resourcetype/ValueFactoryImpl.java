package org.vortikal.repository.resourcetype;

import java.util.Date;

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
    
    public void afterPropertiesSet() throws BeanInitializationException {
        if (principalManager == null) {
            throw new BeanInitializationException("Property 'principalManager' not set.");
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
            // Dates are represented as number of milliseconds since January 1, 1970, 00:00:00 GMT
            try {
                value.setDateValue(new Date(Long.parseLong(stringValue)));
            } catch (NumberFormatException nfe) {
                throw new ValueFormatException(nfe.getMessage());
            }
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

    public void setPrincipalManager(PrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

}
