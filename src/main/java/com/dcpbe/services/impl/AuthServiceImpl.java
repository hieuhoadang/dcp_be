package com.dcpbe.services.impl;

import com.dcpbe.config.KeycloakProperties;
import com.dcpbe.model.dto.request.LoginRequest;
import com.dcpbe.model.dto.request.RefreshTokenRequest;
import com.dcpbe.model.dto.response.AuthResponse;
import com.dcpbe.model.dto.response.AuthTokenResponse;
import com.dcpbe.model.dto.response.KeycloakTokenResponse;
import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.services.AuthService;
import com.dcpbe.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final KeycloakProperties keycloakProperties;
    private final JwtDecoder jwtDecoder;
    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public AuthResponse login(LoginRequest request) {
        KeycloakTokenResponse keycloakToken = requestToken(buildPasswordGrant(request));
        Jwt jwt = jwtDecoder.decode(keycloakToken.getAccessToken());
        UserProfileResponse profile = userService.getCurrentUser(jwt);
        return new AuthResponse(toTokenResponse(keycloakToken), profile);
    }

    @Override
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        KeycloakTokenResponse keycloakToken = requestToken(buildRefreshGrant(request));
        return toTokenResponse(keycloakToken);
    }

    private KeycloakTokenResponse requestToken(MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            var response = restTemplate.postForEntity(
                    keycloakProperties.getTokenUri(),
                    new HttpEntity<>(form, headers),
                    KeycloakTokenResponse.class
            );

            KeycloakTokenResponse body = response.getBody();
            if (body == null || body.getAccessToken() == null || body.getRefreshToken() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Keycloak did not return token data");
            }
            return body;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Keycloak authentication failed", ex);
        }
    }

    private MultiValueMap<String, String> buildPasswordGrant(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("username", request.getUsername());
        form.add("password", request.getPassword());
        return form;
    }

    private MultiValueMap<String, String> buildRefreshGrant(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", request.getRefreshToken());
        return form;
    }

    private AuthTokenResponse toTokenResponse(KeycloakTokenResponse tokenResponse) {
        return new AuthTokenResponse(
                tokenResponse.getAccessToken(),
                tokenResponse.getRefreshToken(),
                tokenResponse.getExpiresIn(),
                tokenResponse.getRefreshExpiresIn(),
                tokenResponse.getTokenType()
        );
    }
}
