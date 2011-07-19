package org.vortikal.repository.resourcetype.property;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Required;
import org.vortikal.repository.Property;
import org.vortikal.repository.PropertyEvaluationContext;
import org.vortikal.repository.resourcetype.PropertyEvaluator;
import org.vortikal.repository.resourcetype.PropertyType;
import org.vortikal.repository.store.Metadata;

public class JSONPropertyFieldEvaluratorImpl implements PropertyEvaluator {

    private Class<?> clazz;
    private String key;
    private String binaryMimeType = null;

    @Override
    public boolean evaluate(Property property, PropertyEvaluationContext ctx) throws PropertyEvaluationException {

        Metadata obj = null;
        try {
            obj = (Metadata) ctx.getContent().getContentRepresentation(clazz);
        } catch (Exception e) {
        }
        if (obj == null) {
            return false;
        }
        String value = (String) obj.getValue(key);
        if (value != null) {
            if (property.getType() == PropertyType.Type.BINARY) {
                property.setBinaryValue(Base64.decode(value), binaryMimeType);
            } else if (property.getType() == PropertyType.Type.INT) {
                try {
                    int intValue = Integer.parseInt(value);
                    property.setIntValue(intValue);
                } catch (Exception e) {
                    return false;
                }

            } else if (property.getType() == PropertyType.Type.STRING) {
                property.setStringValue(value);
            }
        } else {
            return false;
        }
        return true;
    }

    @Required
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Required
    public void setKey(String key) {
        this.key = key;
    }

    public void setBinaryMimeType(String binaryMimeType) {
        this.binaryMimeType = binaryMimeType;
    }

}