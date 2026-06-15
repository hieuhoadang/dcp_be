package com.dcpbe.services;

import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.model.dto.request.UserUpsertRequest;
import com.dcpbe.model.dto.response.UserListItemResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface UserService {
    UserProfileResponse getCurrentUser(Jwt jwt);
    List<UserListItemResponse> listUsers();
    UserListItemResponse createUser(UserUpsertRequest request);
    UserListItemResponse updateUser(String username, UserUpsertRequest request);
    void deleteUser(String username);
}
