package com.dcpbe.services.impl;

import com.dcpbe.config.KeycloakJwtRoles;
import com.dcpbe.model.dto.request.UserUpsertRequest;
import com.dcpbe.model.dto.response.UserListItemResponse;
import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.model.dto.response.UserPageResponse;
import com.dcpbe.model.entity.User;
import com.dcpbe.repository.UserRepository;
import com.dcpbe.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
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
    @Transactional(readOnly = true)
    public UserPageResponse pageUsers(String search, List<String> sort, String sortBy, String sortOrder,
                                      int pageIndex, int pageSize, String position, String username, String fullName, String email) {
        int safePageIndex = Math.max(pageIndex - 1, 0);
        int safePageSize = Math.max(pageSize, 1);

        Pageable pageable = PageRequest.of(safePageIndex, safePageSize, buildSort(sort, sortBy, sortOrder));

        String searchParam = (search == null || search.isBlank()) ? null : search.trim();
        String positionParam = (position == null || position.isBlank()) ? null : position;
        String usernameParam = (username == null || username.isBlank()) ? null : username.trim();
        String fullNameParam = (fullName == null || fullName.isBlank()) ? null : fullName.trim();
        String emailParam = (email == null || email.isBlank()) ? null : email.trim();

        Page<User> page = userRepository.searchUsers(searchParam, positionParam, usernameParam, fullNameParam, emailParam, pageable);

        List<UserListItemResponse> content = page.getContent()
                .stream()
                .map(this::toListItemResponse)
                .toList();

        return new UserPageResponse(content, page.getTotalElements());
    }

    private Sort buildSort(List<String> sorts, String sortBy, String sortOrder) {
        // fe không truyền sort thì tạo list rỗng
        List<Sort.Order> orders = (sorts == null ? List.<String>of() : sorts).stream()
                .map(this::parseSortOrder)
                .filter(order -> order.getProperty() != null && !order.getProperty().isBlank())
                .toList();

        if (!orders.isEmpty()) {
            return Sort.by(orders);
        }

        String property = (sortBy == null || sortBy.isBlank()) ? "username" : sortBy.trim();
        return Sort.by(resolveDirection(sortOrder), property);
    }

    private Sort.Order parseSortOrder(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.split(",", 2);
        String property = parts[0] == null ? "" : parts[0].trim();
        if(property.isBlank()) return null;
        String directionRaw = parts.length > 1 ? parts[1] : null;
        Sort.Direction direction = resolveDirection(directionRaw);
        return new Sort.Order(direction, property);
    }

    private Sort.Direction resolveDirection(String value) {
        return "descend".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
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
        user.setFullName(safeText(request.getFullName(), request.getUsername()));
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
            user.setFullName(request.getFullName().trim());
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
                dbUser.getFullName(),
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
                user.getFullName(),
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

    private boolean matchesSearch(User user, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String term = normalize(search);
        return normalize(user.getUsername()).contains(term)
                || normalize(user.getFullName()).contains(term)
                || normalize(user.getEmail()).contains(term)
                || normalize(user.getPosition()).contains(term)
                || normalize(user.getRoles()).contains(term);
    }

    private boolean matchesPosition(User user, String position) {
        if (position == null || position.isBlank()) {
            return true;
        }
        return normalize(user.getPosition()).contains(normalize(position));
    }

    private Comparator<User> buildComparator(String sortBy, String sortOrder) {
        Comparator<User> comparator = switch (sortBy == null ? "" : sortBy) {
            case "username" -> Comparator.comparing(User::getUsername, this::compareNullable);
            case "fullName" -> Comparator.comparing(User::getFullName, this::compareNullable);
            case "email" -> Comparator.comparing(User::getEmail, this::compareNullable);
            case "position" -> Comparator.comparing(User::getPosition, this::compareNullable);
            default -> Comparator.comparing(User::getId, this::compareNullable);
        };

        if ("descend".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private int compareNullable(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        return a.compareToIgnoreCase(b);
    }

    private <T extends Comparable<T>> int compareNullable(T left, T right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
