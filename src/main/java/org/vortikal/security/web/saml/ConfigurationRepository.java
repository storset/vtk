package org.vortikal.security.web.saml;

public class ConfigurationRepository {

    public static SamlConfiguration configurationForDomain(String domain) {
        /* Only dummy-implementation. */
        if (!"davide-laptop.uio.no".equals(domain)) {
            throw new RuntimeException("Configuration for domain not found: " + domain);
        }

        return new SamlConfiguration("http://davide-laptop.uio.no:9322/login",
                "http://testservice.davide-laptop.uio.no");
    }
}
