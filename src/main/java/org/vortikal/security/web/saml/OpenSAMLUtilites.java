package org.vortikal.security.web.saml;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.opensaml.Configuration;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.util.Base64;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OpenSAMLUtilites {

    private final static Map<Class<?>, QName> elementCache = new ConcurrentHashMap<Class<?>, QName>();


    public static <T> QName getElementQName(Class<T> type) {
        if (elementCache.containsKey(type)) {
            return elementCache.get(type);
        }

        try {
            Field typeField;
            try {
                typeField = type.getDeclaredField("DEFAULT_ELEMENT_NAME");
            } catch (NoSuchFieldException ex) {
                typeField = type.getDeclaredField("ELEMENT_NAME");
            }

            QName objectQName = (QName) typeField.get(null);
            elementCache.put(type, objectQName);
            return objectQName;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Create an issuer with a given value.
     * 
     * @param value
     *            The value
     * @return The SAML Issuer with the given value
     */
    public static Issuer createIssuer(String value) {
        if (value == null) {
            return null;
        }

        Issuer issuer = buildXMLObject(Issuer.class);
        issuer.setValue(value);
        return issuer;
    }


    /**
     * Build a new empty object of the requested type.
     * 
     * The requested type must have a DEFAULT_ELEMENT_NAME attribute describing the element type as a QName.
     * 
     * @param <T>
     *            SAML Object type
     */
    @SuppressWarnings("unchecked")
    public static <T extends XMLObject> T buildXMLObject(Class<T> type) {
        try {
            QName objectQName = getElementQName(type);
            XMLObjectBuilder<T> builder = Configuration.getBuilderFactory().getBuilder(objectQName);
            if (builder == null) {
                throw new InvalidParameterException("No builder exists for object: " + objectQName.getLocalPart());
            }
            return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(), objectQName
                    .getPrefix());
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Build Java certificate from base64 encoding.
     * 
     * @param base64Cert
     *            base64-encoded certificate
     * @return assertion native Java X509 certificate
     * @throws CertificateException
     *             thrown if there is an error constructing certificate
     */
    public static java.security.cert.X509Certificate buildJavaX509Cert(String base64Cert) throws CertificateException {
        CertificateFactory cf = null;
        cf = CertificateFactory.getInstance("X.509");

        ByteArrayInputStream input = new ByteArrayInputStream(Base64.decode(base64Cert));
        java.security.cert.X509Certificate newCert = null;
        newCert = (java.security.cert.X509Certificate) cf.generateCertificate(input);
        return newCert;
    }


    /**
     * Check that assertion given object has been signed correctly with assertion specific {@link PublicKey}.
     * 
     * @return true, if the signableObject has been signed correctly with the given key. Returns <code>false</code> if
     *         the object is not signed at all.
     */
    public static boolean verifySignature(PublicKey publicKey, Assertion assertion) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        Signature signature = null;

        SignableSAMLObject signableObject = (SignableSAMLObject) assertion;

        signature = signableObject.getSignature();

        if (signature == null) {

            return false;
        }
        BasicX509Credential credential = new BasicX509Credential();
        credential.setPublicKey(publicKey);
        SignatureValidator validator = new SignatureValidator(credential);
        try {
            validator.validate(signature);
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }



    /**
     * Parse an XML string.
     * 
     * @param elementString
     *            The String to parse
     * @return The corresponding document {@link Element}.
     */
    public static Element loadElementFromString(String elementString) throws Exception {

        DocumentBuilderFactory newFactory = DocumentBuilderFactory.newInstance();
        newFactory.setNamespaceAware(true);

        DocumentBuilder builder = newFactory.newDocumentBuilder();

        Document doc = builder.parse(new ByteArrayInputStream(elementString.getBytes("UTF-8")));
        Element samlElement = doc.getDocumentElement();

        return samlElement;
    }

}
