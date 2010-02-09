package org.vortikal.security.web.saml;

import org.opensaml.saml2.core.Issuer;

public class SamlConfiguration {

    private String authenticationUrl = "https://auth-test1.uio.no/simplesaml/saml2/idp/SSOService.php";

    private String logoutUrl = "https://auth-test1.uio.no/simplesaml/saml2/idp/SingleLogoutService.php";

    private final String protocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

    private final String serviceLoginUrl;

    private final String serviceIdentifier;


    public SamlConfiguration(String serviceUrl, String serviceIdentifier) {
        this.serviceLoginUrl = serviceUrl;
        this.serviceIdentifier = serviceIdentifier;
    }


    public Issuer issuer() {
        return OpenSAMLUtilites.createIssuer(serviceIdentifier);
    }


    public String protocolBinding() {
        return "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    }


    public String serviceLoginUrl() {
        return serviceLoginUrl;
    }


    public String serviceLogoutUrl() {
        return logoutUrl;
    }


    public String serviceIdentifier() {
        return serviceIdentifier;
    }


    public String authenticationUrl() {
        return authenticationUrl;
    }

}
