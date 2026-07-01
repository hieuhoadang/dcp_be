package com.dcpbe.controller;

import com.dcpbe.model.dto.request.UserUpsertRequest;
import com.dcpbe.model.dto.response.UserListItemResponse;
import com.dcpbe.model.dto.response.UserProfileResponse;
import com.dcpbe.model.dto.response.UserPageResponse;
import com.dcpbe.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping("/page")
    public ResponseEntity<UserPageResponse> pageUsers(
            HttpServletRequest request,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortOrder", required = false) String sortOrder,
            @RequestParam(value = "pageIndex", defaultValue = "1") int pageIndex,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email
    ) {
        List<String> sort = request.getParameterValues("sort") == null
                ? List.of()
                : List.of(request.getParameterValues("sort"));
        return ResponseEntity.ok(userService.pageUsers(search, sort, sortBy, sortOrder, pageIndex, pageSize, position,username, fullName, email));
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
