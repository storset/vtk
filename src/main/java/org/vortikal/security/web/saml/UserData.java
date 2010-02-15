package org.vortikal.security.web.saml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AttributeValue;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSString;


public class UserData {

    private Map<String, List<String>> attrs = new HashMap<String, List<String>>();

    public UserData(Assertion assertion) {
        for (AttributeStatement attrStatement : assertion.getAttributeStatements()) {
            for (Attribute attr : attrStatement.getAttributes()) {
                this.attrs.put(attr.getName(), extractValues(attr));
            }
        }
    }


    public String getUsername() {
        return getSimpleAttribute("uid");
    }


    public String getCommonName() {
        return getSimpleAttribute("cn");
    }

    
    public List<String> getAttribute(String name) {
        return Collections.unmodifiableList(this.attrs.get(name));
    }
    
    public String getSimpleAttribute(String name) {
        List<String> list = this.attrs.get(name);
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private List<String> extractValues(Attribute attribute) {
        
        List<XMLObject> values = attribute.getAttributeValues();
        List<String> result = new ArrayList<String>();

        for (XMLObject val : values) {
            if (!(val instanceof XSString)) {
                continue;
            }
            XSString s = (XSString) val;
            QName qname = s.getElementQName();
            if (!SAMLConstants.SAML20_NS.equals(qname.getNamespaceURI())) {
                continue;
            }
            if (!AttributeValue.DEFAULT_ELEMENT_LOCAL_NAME.equals(qname.getLocalPart())) {
                continue;
            }
            result.add(s.getValue());
        }
        return result;
    }
}
