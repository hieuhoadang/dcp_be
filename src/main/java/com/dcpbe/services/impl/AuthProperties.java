package com.dcpbe.services.impl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private String backendRedirectUri;
    private String frontendRedirectUri;
}
