package com.dcpbe.services.impl;

import com.dcpbe.model.dto.response.AuthTokenResponse;
import com.dcpbe.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String CODE_CHALLENGE_METHOD = "S256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final RestTemplate restTemplate;
    private final KeyCloakProperties keycloakProps;
    private final AuthProperties authProps;
    private final ConcurrentMap<String, String> pkceVerifiers = new ConcurrentHashMap<>();


    private String getRealmBaseUrl() {
        return keycloakProps.getBaseUrl()
                + "/realms/" + keycloakProps.getRealm()
                + "/protocol/openid-connect";
    }

    private String getAuthorizationUrl() {
        return getRealmBaseUrl() + "/auth";
    }

    private String getTokenUrl() {
        return getRealmBaseUrl() + "/token";
    }

    private String getLogoutUrl() {
        return getRealmBaseUrl() + "/logout";
    }

    @Override
    public String buildAuthorizationRedirectUrl() {
        String state = createUrlSafeToken(32);
        String codeVerifier = createUrlSafeToken(64);
        pkceVerifiers.put(state, codeVerifier);

        return UriComponentsBuilder.fromUriString(getAuthorizationUrl())
                .queryParam("client_id", keycloakProps.getClientId())
                .queryParam("redirect_uri", authProps.getBackendRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid profile")
                .queryParam("prompt", "login")
                .queryParam("state", state)
                .queryParam("code_challenge", createCodeChallenge(codeVerifier))
                .queryParam("code_challenge_method", CODE_CHALLENGE_METHOD)
                .build()
                .toUriString();
    }

    @Override
    public String buildCallbackErrorRedirectUrl(String error, String errorDescription) {
        return UriComponentsBuilder.fromUriString(authProps.getFrontendRedirectUri())
                .fragment("error=" + error + "&error_description=" + errorDescription)
                .build()
                .toUriString();
    }

    @Override
    public String exchangeCodeAndBuildFrontendRedirectUrl(String code, String state) {
        String codeVerifier = state != null ? pkceVerifiers.remove(state) : null;

        if (codeVerifier == null) {
            return buildCallbackErrorRedirectUrl("invalid_state", "Missing PKCE code verifier");
        }

        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", authProps.getBackendRedirectUri());
        form.add("code_verifier", codeVerifier);

        AuthTokenResponse token = toTokenResponse(postForm(getTokenUrl(), form));

        return UriComponentsBuilder.fromUriString(authProps.getFrontendRedirectUri())
                .fragment("access_token=" + token.getAccessToken()
                        + "&refresh_token=" + token.getRefreshToken()
                        + "&expires_in=" + token.getExpiresIn()
                        + "&refresh_expires_in=" + token.getRefreshExpiresIn()
                        + "&token_type=" + token.getTokenType())
                .build()
                .toUriString();
    }

    @Override
    public String handleCallback(String code, String state, String error, String errorDescription) {
        if (error != null) {
            return buildErrorRedirect(error, errorDescription);
        }
        if (code == null || code.isBlank()) {
            return buildErrorRedirect(
                    "missing_code",
                    "Keycloak did not return authorization code"
            );
        }
        if (state == null || state.isBlank()) {
            return buildErrorRedirect(
                    "invalid_state",
                    "Missing state parameter"
            );
        }
        try {
            return exchangeCodeAndBuildFrontendRedirectUrl(code, state);
        } catch (Exception e) {
            return buildErrorRedirect("internal_error", e.getMessage());
        }
    }

    @Override
    public AuthTokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return toTokenResponse(postForm(getTokenUrl(), form));
    }

    @Override
    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = baseClientForm();
        form.add("refresh_token", refreshToken);
        postForm(getLogoutUrl(), form);
    }

    // ==============================
    // HELPER
    // ==============================

    private MultiValueMap<String, String> baseClientForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProps.getClientId());
        form.add("client_secret", keycloakProps.getClientSecret());
        return form;
    }

    private Map<String, Object> postForm(String url, MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    private AuthTokenResponse toTokenResponse(Map<String, Object> token) {
        if (token == null) {
            throw new IllegalStateException("Keycloak did not return token response");
        }

        return new AuthTokenResponse(
                (String) token.get("access_token"),
                (String) token.get("refresh_token"),
                ((Number) token.getOrDefault("expires_in", 0)).longValue(),
                ((Number) token.getOrDefault("refresh_expires_in", 0)).longValue(),
                (String) token.getOrDefault("token_type", "Bearer")
        );
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create code challenge", e);
        }
    }

    private String createUrlSafeToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildErrorRedirect(String error, String description) {
        String safeDescription = (description != null && !description.isBlank())
                ? description
                : "Unknown error";

        return buildCallbackErrorRedirectUrl(error, safeDescription);
    }
}
