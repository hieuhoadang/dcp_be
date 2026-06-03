package com.dcpbe.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class KeycloakJwtClaimHelper {
    private final KeycloakProperties keycloakProperties;

    public KeycloakJwtClaimHelper(KeycloakProperties keycloakProperties) {
        this.keycloakProperties = keycloakProperties;
    }

    public String getUsername(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return jwt.getSubject();
    }

    public String getFullName(Jwt jwt, String fallbackUsername) {
        String fullName = jwt.getClaimAsString("name");
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return fallbackUsername;
    }

    public String getEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        return "hello@gmail.com";
    }

    public List<String> extractRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> realmRoles) {
            for (Object role : realmRoles) {
                if (role != null) {
                    roles.add(role.toString());
                }
            }
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Object clientAccess = resourceAccess.get(keycloakProperties.getClientId());
            if (clientAccess instanceof Map<?, ?> clientMap) {
                Object clientRoles = clientMap.get("roles");
                if (clientRoles instanceof List<?> roleList) {
                    for (Object role : roleList) {
                        if (role != null) {
                            roles.add(role.toString());
                        }
                    }
                }
            }
        }

        return new ArrayList<>(roles);
    }
}
