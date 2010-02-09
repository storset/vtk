package org.vortikal.security.web.saml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.opensaml.xml.util.Base64;

public class UserAttributeFromSAML implements Serializable {

    private static final long serialVersionUID = 6763097557002471875L;

    private final String name;

    private final String userName;

    private final List<String> values;

    private final String format;


    public UserAttributeFromSAML(String name, String friendlyName, List<String> values, String format) {
        super();
        this.name = name;
        this.userName = friendlyName;
        this.values = values;
        this.format = format;
    }


    public String getName() {
        return name;
    }


    public String getFriendlyName() {
        return userName;
    }


    public List<String> getValues() {
        if (values == null) {
            return new ArrayList<String>();
        }
        return values;
    }


    /**
     * Gets the first value of all the AttributeValues
     * 
     * @return String
     */
    public String getValue() {
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }


    /**
     * Base64 decode the attribute value and retrieve it.
     * 
     * @return The decoded value. No checks are made to see if the string is actually encoded correctly.
     */
    public List<byte[]> getBase64Values() {
        List<byte[]> base64Values = new ArrayList<byte[]>();
        if (values == null) {
            return new ArrayList<byte[]>();
        }
        for (String str : values) {
            base64Values.add(Base64.decode(str));
        }
        return base64Values;
    }


    /**
     * Base64 decode the first attribute value and decode it
     * 
     * @return byte[]
     */
    public byte[] getBase64Value() {
        if (values == null || values.size() == 0) {
            return null;
        }
        return Base64.decode(values.get(0));
    }


    public String getFormat() {
        return format;
    }


    public static UserAttributeFromSAML create(String name, String format) {
        if (format != null && format.trim().equals("")) {
            format = null;
        }
        return new UserAttributeFromSAML(name, null, null, format);
    }


    @Override
    public String toString() {
        return name + " (" + userName + "): " + values;
    }
}
