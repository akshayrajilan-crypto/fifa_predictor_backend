package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.AuthRequest;
import com.fifaworldcup.Fifa.dto.AuthResponse;
import com.fifaworldcup.Fifa.dto.RegisterRequest;
import com.fifaworldcup.Fifa.model.InviteCode;
import com.fifaworldcup.Fifa.model.User;
import com.fifaworldcup.Fifa.repository.InviteCodeRepository;
import com.fifaworldcup.Fifa.repository.UserRepository;
import com.fifaworldcup.Fifa.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate invite code — must exist, not used
        InviteCode inviteCode = inviteCodeRepository.findByCode(request.getInviteCode().trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Invalid invite code."));

        if (inviteCode.isUsed()) {
            throw new RuntimeException("This invite code has already been used.");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .avatar(request.getAvatar())
                .role(User.Role.USER)
                .build();
        userRepository.save(user);

        // Mark invite code as used
        inviteCode.setUsed(true);
        inviteCode.setUsedByUsername(user.getUsername());
        inviteCode.setUsedAt(LocalDateTime.now());
        inviteCodeRepository.save(inviteCode);

        String token = jwtUtils.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getAvatar());
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtils.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), user.getAvatar());
    }
}
