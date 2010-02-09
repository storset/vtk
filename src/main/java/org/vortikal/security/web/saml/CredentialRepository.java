package org.vortikal.security.web.saml;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.x509.BasicX509Credential;

/**
 * Class for managing credentials.
 * 
 * Credentials can be loaded from the file system. When loaded, credentials are cached, so they are only loaded once.
 * 
 * This class is thread-safe, and can be shared across threads.
 * 
 */
public class CredentialRepository {

    private final Map<Key, BasicX509Credential> credentials = new ConcurrentHashMap<Key, BasicX509Credential>();


    /**
     * Load credentials from a keystore.
     * 
     * The first private key is loaded from the keystore.
     * 
     * @param location
     *            keystore file location
     * @param password
     *            Keystore and private key password.
     */
    public BasicX509Credential getCredential(String location, String password) throws IOException,
            GeneralSecurityException {
        Key key = new Key(location, password);
        BasicX509Credential credential = credentials.get(key);
        if (credential == null) {

            FileInputStream is = new FileInputStream(location);
            credential = createCredential(is, password);
            is.close();
            credentials.put(key, credential);

        }
        return credential;
    }


    public Collection<BasicX509Credential> getCredentials() {
        return credentials.values();
    }


    /**
     * Read credentials from a inputstream.
     * 
     * The stream can either point to a PKCS12 keystore or a JKS keystore. The store is converted into a
     * {@link Credential} including the private key.
     * 
     * @param input
     *            Stream pointing to the certificate store.
     * @param password
     *            Password for the store. The same password is also used for the certificate.
     * 
     * @return The {@link Credential}
     */
    public static BasicX509Credential createCredential(InputStream input, String password)
            throws GeneralSecurityException, IOException {
        BasicX509Credential credential = new BasicX509Credential();

        KeyStore ks = loadKeystore(input, password);

        Enumeration<String> eAliases = ks.aliases();
        while (eAliases.hasMoreElements()) {
            String strAlias = eAliases.nextElement();

            if (ks.isKeyEntry(strAlias)) {
                PrivateKey privateKey = (PrivateKey) ks.getKey(strAlias, password.toCharArray());
                credential.setPrivateKey(privateKey);
                credential.setEntityCertificate((X509Certificate) ks.getCertificate(strAlias));
                PublicKey publicKey = ks.getCertificate(strAlias).getPublicKey();
                credential.setPublicKey(publicKey);
            }
        }

        return credential;
    }


    private static KeyStore loadKeystore(InputStream input, String password) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        input = new BufferedInputStream(input);
        input.mark(1024 * 1024);
        KeyStore ks;
        try {
            ks = loadStore(input, password, "PKCS12");
        } catch (IOException e) {
            input.reset();
            ks = loadStore(input, password, "JKS");
        }
        return ks;
    }


    /**
     * Get a x509certificate from a keystore.
     * 
     * @param location
     *            Keystore file location.
     * @param password
     *            Password for the keystore.
     * @param alias
     *            Alias to retrieve. If <code>null</code>, the first certificate in the keystore is retrieved.
     * @return The certificate.
     */
    public X509Certificate getCertificate(String location, String password, String alias) throws IOException,
            GeneralSecurityException {
        Key key = new Key(location, password, alias);
        BasicX509Credential credential = credentials.get(key);
        if (credential == null) {

            FileInputStream is = new FileInputStream(location);
            KeyStore keystore = loadKeystore(is, password);
            is.close();

            if (alias == null) {
                Enumeration<String> eAliases = keystore.aliases();
                while (eAliases.hasMoreElements()) {
                    String strAlias = eAliases.nextElement();

                    if (keystore.isCertificateEntry(strAlias)) {
                        X509Certificate certificate = (X509Certificate) keystore.getCertificate(strAlias);
                        credential = new BasicX509Credential();
                        credential.setEntityCertificate(certificate);
                        credentials.put(new Key(location, password, strAlias), credential);
                        alias = strAlias;
                    }
                }
            }

            credential = credentials.get(new Key(location, password, alias));
            if (credential == null) {
                throw new NullPointerException("Unable to find certificate for " + alias);
            }

        }
        return credential.getEntityCertificate();
    }


    private static KeyStore loadStore(InputStream input, String password, String type) throws KeyStoreException,
            IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(type);
        char[] jksPassword = password.toCharArray();
        ks.load(input, jksPassword);
        input.close();
        return ks;
    }

    private static class Key {
        private final String location;

        private final String password;

        private final String alias;


        public Key(String location, String password) {
            this.location = location;
            this.password = password;
            this.alias = null;
        }


        public Key(String location, String password, String alias) {
            this.location = location;
            this.password = password;
            this.alias = alias;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((alias == null) ? 0 : alias.hashCode());
            result = prime * result + ((location == null) ? 0 : location.hashCode());
            result = prime * result + ((password == null) ? 0 : password.hashCode());
            return result;
        }


        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (alias == null) {
                if (other.alias != null)
                    return false;
            } else if (!alias.equals(other.alias))
                return false;
            if (location == null) {
                if (other.location != null)
                    return false;
            } else if (!location.equals(other.location))
                return false;
            if (password == null) {
                if (other.password != null)
                    return false;
            } else if (!password.equals(other.password))
                return false;

            return true;
        }

    }

}
