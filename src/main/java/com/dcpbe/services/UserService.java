package com.dcpbe.services;

import com.dcpbe.model.dto.response.UserProfileResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserService {
    UserProfileResponse getCurrentUser(Jwt jwt);
}
