package org.vortikal.security.web.saml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.xml.schema.XSString;

public class UserData {

    private Map<String, UserAttributeFromSAML> attributes = new HashMap<String, UserAttributeFromSAML>();


    public UserData(Assertion assertion) {
        loadAttributesFromAssertion(assertion);
    }


    public String username() {
        return getAttributeValue("uid");
    }


    public String commonName() {
        return getAttributeValue("cn");
    }


    /* Extract from the constructor of UserAssertionImpl. */
    private void loadAttributesFromAssertion(Assertion assertion) {
        for (AttributeStatement attrStatement : assertion.getAttributeStatements()) {
            for (Attribute attr : attrStatement.getAttributes()) {
                attributes.put(attr.getName(), new UserAttributeFromSAML(attr.getName(), attr.getFriendlyName(),
                        extractAttributeValueValues(attr), attr.getNameFormat()));
            }
        }
    }


    /* The following method is from UserAssertionImpl. */

    private String getAttributeValue(String name) {
        UserAttributeFromSAML attr = attributes.get(name);
        if (attr != null) {
            List<String> values = attr.getValues();
            if (values.size() > 0) {
                return values.get(0);
            }
        }
        return null;
    }


    /* The following method is from AttributeUtil. */
    private List<String> extractAttributeValueValues(Attribute attribute) {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < attribute.getAttributeValues().size(); i++) {
            if (attribute.getAttributeValues().get(i) instanceof XSString) {
                XSString str = (XSString) attribute.getAttributeValues().get(i);
                if (AttributeValue.DEFAULT_ELEMENT_LOCAL_NAME.equals(str.getElementQName().getLocalPart())
                        && SAMLConstants.SAML20_NS.equals(str.getElementQName().getNamespaceURI())) {
                    values.add(str.getValue());
                }
            }
        }
        return values;
    }
}
