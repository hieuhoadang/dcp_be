package com.dcpbe.controller;

import com.dcpbe.model.dto.request.LoginRequest;
import com.dcpbe.model.dto.request.RefreshTokenRequest;
import com.dcpbe.model.dto.response.AuthResponse;
import com.dcpbe.model.dto.response.AuthTokenResponse;
import com.dcpbe.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/authorize")
    public RedirectView authorize(){
        return new RedirectView(authService.buildAuthorizationRedirectUrl());
    }

    @GetMapping("/callback")
    public RedirectView callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription
    ){
        String redirectUrl = authService.handleCallback(code,state,error,errorDescription);
        return new RedirectView(redirectUrl);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
