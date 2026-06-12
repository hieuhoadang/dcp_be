package com.dcpbe.services.impl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.keycloak")
public class KeyCloakProperties {
    private String baseUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
}
