package net.disburse.controller;

import lombok.Getter;
import net.disburse.dto.AuthResponse;
import net.disburse.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        String accessToken = jwtUtil.generateToken(authentication.getName(), 1000 * 60 * 60);
        String refreshToken = jwtUtil.generateToken(authentication.getName(), 1000L * 60 * 60 * 24 * 7);

        AuthResponse authResponse = new AuthResponse(accessToken, refreshToken);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshTokens(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired refresh token"));
        }

        String username = jwtUtil.extractUsername(refreshToken);

        String accessToken = jwtUtil.generateToken(username, 15 * 60 * 1000); // 15 minutes
        String newRefreshToken = jwtUtil.generateToken(username, 7 * 24 * 60 * 60 * 1000); // 7 days

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", newRefreshToken
        ));
    }
}

@Getter
class AuthRequest {
    private String username;
    private String password;
}
