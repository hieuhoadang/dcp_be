package com.dcpbe.services;

import com.dcpbe.model.dto.request.LoginRequest;
import com.dcpbe.model.dto.request.RefreshTokenRequest;
import com.dcpbe.model.dto.response.AuthResponse;
import com.dcpbe.model.dto.response.AuthTokenResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthTokenResponse refresh(RefreshTokenRequest request);
}
