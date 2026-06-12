package com.dcpbe.services;

import com.dcpbe.model.dto.request.LoginRequest;
import com.dcpbe.model.dto.request.RefreshTokenRequest;
import com.dcpbe.model.dto.response.AuthResponse;
import com.dcpbe.model.dto.response.AuthTokenResponse;

public interface AuthService {
    String buildAuthorizationRedirectUrl();
    String buildCallbackErrorRedirectUrl(String error, String errorDescription);
    String exchangeCodeAndBuildFrontendRedirectUrl(String code, String state);
    String handleCallback(String code, String state, String error, String errorDescription);
    AuthTokenResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
