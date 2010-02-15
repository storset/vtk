package org.vortikal.security.web.saml;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.opensaml.xml.security.x509.BasicX509Credential;
import org.springframework.core.io.Resource;

public class CertificateManager {
    
    private Map<String, BasicX509Credential> privateKeys;
    
    public CertificateManager(Resource keystore, String keystorePassword, String privateKeyPassword) throws Exception {
        Map<String, BasicX509Credential> privateKeys = new HashMap<String, BasicX509Credential>();
        KeyStore ks = load(keystore, keystorePassword);
        Enumeration<String> aliases = ks.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                Key key = ks.getKey(alias, privateKeyPassword.toCharArray());
                if (!(key instanceof PrivateKey)) {
                    continue;
                }
                PrivateKey pk = (PrivateKey) key;
                Certificate c = ks.getCertificate(alias);
                if (!(c instanceof X509Certificate)) {
                    continue;
                }
                X509Certificate certificate = (X509Certificate) c;
                
                BasicX509Credential credential = new BasicX509Credential();
                credential.setPrivateKey(pk);
                credential.setEntityCertificate(certificate);
                credential.setPublicKey(certificate.getPublicKey());
                privateKeys.put(alias, credential);
            }
        }
        this.privateKeys = privateKeys;
    }

    public BasicX509Credential getCredential(String keyAlias) {
        return this.privateKeys.get(keyAlias);
    }
    
    private KeyStore load(Resource resource, String password) throws Exception {
        InputStream is = resource.getInputStream();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(is, password.toCharArray());
        is.close();
        return ks;
    }
}
