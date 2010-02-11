package org.vortikal.security.web.saml;

import org.opensaml.saml2.core.Issuer;

public class SamlConfiguration {


    private String authenticationUrl = null;
    private String logoutUrl = null;

    private String serviceLoginUrl = null;
    private String serviceIdentifier = null;

    private String protocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    public SamlConfiguration(String authenticationUrl, String logoutUrl,
            String serviceLoginUrl, String serviceIdentifier) {
        this.authenticationUrl = authenticationUrl;
        this.logoutUrl = logoutUrl;
        this.serviceLoginUrl = serviceLoginUrl;
        this.serviceIdentifier = serviceIdentifier;
    }

    public Issuer issuer() {
        return OpenSAMLUtilites.createIssuer(serviceIdentifier);
    }

    public String getAuthenticationUrl() {
        return authenticationUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public String getServiceLoginUrl() {
        return serviceLoginUrl;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public String getProtocolBinding() {
        return protocolBinding;
    }

}
