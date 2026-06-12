package com.dcpbe.services.impl;

import com.dcpbe.config.KeycloakJwtRoles;
import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.model.entity.User;
import com.dcpbe.repository.UserRepository;
import com.dcpbe.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private static final String DEFAULT_POSITION = "USER";


    @Override
    @Transactional
    public UserProfileResponse getCurrentUser(Jwt jwt) {
        String username = getUsername(jwt);
        return userRepository.findByUsername(username)
                .map(dbUser -> createUserFromDatabase(jwt,dbUser))
                .orElseGet(()-> createUserFromToken(jwt, username));

    }

    private UserProfileResponse createUserFromDatabase(Jwt jwt, User dbUser) {
        return new UserProfileResponse(
                dbUser.getUsername(),
                dbUser.getFullname(),
                dbUser.getPosition(),
                getRoles(jwt, dbUser)
        );
    }

    private UserProfileResponse createUserFromToken(Jwt jwt, String username){
        return new UserProfileResponse(
                username,
                getFullName(jwt, username),
                DEFAULT_POSITION,
                KeycloakJwtRoles.extract(jwt)
        );
    }

    private String getUsername(Jwt jwt) {
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        return preferredUsername != null && !preferredUsername.isBlank()
                ? preferredUsername
                : jwt.getSubject();
    }

    private String getFullName(Jwt jwt, String username) {
        String fullName = jwt.getClaimAsString("name");
        return fullName != null && !fullName.isBlank() ? fullName : username;
    }

    private List<String> getRoles(Jwt jwt, User databaseUser) {
        List<String> jwtRoles = KeycloakJwtRoles.extract(jwt);
        if (!jwtRoles.isEmpty()) {
            return jwtRoles;
        }

        String databaseRoles = databaseUser.getRoles();
        if (databaseRoles == null || databaseRoles.isBlank()) {
            return List.of();
        }

        return Arrays.stream(databaseRoles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
    }
}
