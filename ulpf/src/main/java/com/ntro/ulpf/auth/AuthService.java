package com.ntro.ulpf.auth;

import com.ntro.ulpf.entity.UserAccount;
import com.ntro.ulpf.repository.UserAccountRepository;
import com.ntro.ulpf.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository=userRepository; this.passwordEncoder=passwordEncoder; this.jwtService=jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user=userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(()->new IllegalArgumentException("Invalid email or password."));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token=jwtService.generateToken(user);
        return new LoginResponse(token,"Bearer",jwtService.getExpirationSeconds(),toResponse(user));
    }

    public AuthUserResponse getCurrentUser(String email) {
        return toResponse(userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(()->new IllegalArgumentException("User not found.")));
    }

    private AuthUserResponse toResponse(UserAccount user) {
        return new AuthUserResponse(user.getId(),user.getName(),user.getEmail(),user.getRole());
    }
}
