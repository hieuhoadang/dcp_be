package com.dcpbe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String authServerUrl;
    private String realm;
    private String clientId;
    private String clientSecret;

    public String getTokenUri() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }
}
