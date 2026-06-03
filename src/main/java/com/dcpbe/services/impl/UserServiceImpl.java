package com.dcpbe.services.impl;

import com.dcpbe.config.KeycloakJwtClaimHelper;
import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.model.entity.User;
import com.dcpbe.repository.UserRepository;
import com.dcpbe.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final KeycloakJwtClaimHelper keycloakJwtClaimHelper;
    private static final String DEFAULT_POSITION = "USER";


    @Override
    @Transactional
    public UserProfileResponse getCurrentUser(Jwt jwt) {
        String username = keycloakJwtClaimHelper.getUsername(jwt);
        User user = userRepository.findByUsername(username).orElseGet(() -> createUserFromToken(jwt, username));
        return new UserProfileResponse(
                user.getUsername(),
                user.getFullname(),
                user.getEmail(),
                user.getPosition(),
                keycloakJwtClaimHelper.extractRoles(jwt)
        );
    }

    private User createUserFromToken(Jwt jwt, String username){
        return userRepository.save(new User(
                username,
                keycloakJwtClaimHelper.getFullName(jwt, username),
                keycloakJwtClaimHelper.getEmail(jwt),
                DEFAULT_POSITION
        ));
    }
}
