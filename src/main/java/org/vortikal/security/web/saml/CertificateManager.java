/* Copyright (c) 2010, University of Oslo, Norway
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
package org.vortikal.security.web.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.util.Base64;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

public class CertificateManager implements InitializingBean {

    private Map<String, BasicX509Credential> privateKeys;

    private Resource keystore;

    private String keystorePassword;

    private String privateKeyPassword;

    private Map<String, X509Certificate> idpCertificates;


    public Map<String, X509Certificate> getIdpCertificates() {
        return idpCertificates;
    }


    public void setIdpCertificates(Map<String, String> idpCertificateMap) throws CertificateException {
        Map<String, X509Certificate> map = new HashMap<String, X509Certificate>();
        for (Map.Entry<String, String> entry : idpCertificateMap.entrySet()) {
            map.put(entry.getKey().toString(), createIdpCertificate(entry.getValue().toString()));
        }
        this.idpCertificates = Collections.unmodifiableMap(map);
    }


    public BasicX509Credential getCredential(String keyAlias) {
        return this.privateKeys.get(keyAlias);
    }


    public X509Certificate getIDPCertificate(String key) {
        return this.idpCertificates.get(key);
    }


    private X509Certificate createIdpCertificate(String idpCertificate) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream input = new ByteArrayInputStream(Base64.decode(idpCertificate));
        return (X509Certificate) cf.generateCertificate(input);
    }


    @Required
    public void setKeystore(Resource keystore) {
        if (keystore == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        this.keystore = keystore;
    }


    @Required
    public void setKeystorePassword(String keystorePassword) {
        if (keystorePassword == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        this.keystorePassword = keystorePassword;
    }


    @Required
    public void setPrivateKeyPassword(String privateKeyPassword) {
        if (privateKeyPassword == null) {
            throw new IllegalArgumentException("Argument is NULL");
        }
        this.privateKeyPassword = privateKeyPassword;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, BasicX509Credential> privateKeys = new HashMap<String, BasicX509Credential>();
        KeyStore ks = load(this.keystore, this.keystorePassword);
        Enumeration<String> aliases = ks.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                Key key = ks.getKey(alias, this.privateKeyPassword.toCharArray());
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


    private KeyStore load(Resource resource, String password) throws Exception {
        InputStream is = resource.getInputStream();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(is, password.toCharArray());
        is.close();
        return ks;
    }
}
