package com.dcpbe.controller;

import com.dcpbe.model.dto.request.UserUpsertRequest;
import com.dcpbe.model.dto.response.UserListItemResponse;
import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserListItemResponse>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(userService.getCurrentUser(jwt));
    }

    @PostMapping
    public ResponseEntity<UserListItemResponse> createUser(@RequestBody UserUpsertRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PutMapping("/{username}")
    public ResponseEntity<UserListItemResponse> updateUser(
            @PathVariable String username,
            @RequestBody UserUpsertRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(username, request));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}
