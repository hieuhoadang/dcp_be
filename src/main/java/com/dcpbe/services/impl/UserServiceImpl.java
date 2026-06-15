package com.dcpbe.services.impl;

import com.dcpbe.config.KeycloakJwtRoles;
import com.dcpbe.model.dto.request.UserUpsertRequest;
import com.dcpbe.model.dto.response.UserListItemResponse;
import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.model.entity.User;
import com.dcpbe.repository.UserRepository;
import com.dcpbe.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Override
    @Transactional(readOnly = true)
    public List<UserListItemResponse> listUsers() {
        return userRepository.findAllByOrderByIdDesc()
                .stream()
                .map(this::toListItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserListItemResponse createUser(UserUpsertRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username is required");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setFullname(safeText(request.getFullName(), request.getUsername()));
        user.setEmail(safeText(request.getEmail(), request.getUsername() + "@local"));
        user.setPosition(safeText(request.getPosition(), DEFAULT_POSITION));
        user.setRoles(joinRoles(request.getRoles()));

        return toListItemResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserListItemResponse updateUser(String username, UserUpsertRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String requestedUsername = request.getUsername() == null ? null : request.getUsername().trim();
        if (requestedUsername != null && !requestedUsername.isBlank() && !requestedUsername.equals(username)) {
            if (userRepository.existsByUsername(requestedUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
            }
            user.setUsername(requestedUsername);
        }
        if (request.getFullName() != null) {
            user.setFullname(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition().trim());
        }
        if (request.getRoles() != null) {
            user.setRoles(joinRoles(request.getRoles()));
        }

        return toListItemResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(user);
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

    private UserListItemResponse toListItemResponse(User user) {
        return new UserListItemResponse(
                user.getUsername(),
                user.getFullname(),
                user.getEmail(),
                user.getPosition(),
                splitRoles(user.getRoles())
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

    private List<String> splitRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }

        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
    }

    private String joinRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "";
        }

        return roles.stream()
                .map(role -> role == null ? "" : role.trim())
                .filter(role -> !role.isBlank())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
