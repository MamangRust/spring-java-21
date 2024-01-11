package com.sanedge.spring21.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.spring21.domain.request.LoginRequest;
import com.sanedge.spring21.domain.request.RegisterRequest;
import com.sanedge.spring21.domain.response.AuthResponse;
import com.sanedge.spring21.domain.response.MessageResponse;
import com.sanedge.spring21.domain.response.TokenRefreshResponse;
import com.sanedge.spring21.enums.ERole;
import com.sanedge.spring21.exception.ResourceNotFoundException;
import com.sanedge.spring21.exception.TokenRefreshException;
import com.sanedge.spring21.models.RefreshToken;
import com.sanedge.spring21.models.Role;
import com.sanedge.spring21.models.User;
import com.sanedge.spring21.repository.RoleRepository;
import com.sanedge.spring21.repository.UserRepository;
import com.sanedge.spring21.security.JwtProvider;
import com.sanedge.spring21.security.UserDetailsImpl;
import com.sanedge.spring21.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenServiceImpl refreshTokenService;

    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
            RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
            RefreshTokenServiceImpl refreshTokenServiceImpl) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenServiceImpl;
    }

    @Override
    public MessageResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtProvider.generateAccessToken(authentication);

        long expiresAt = jwtProvider.getjwtExpirationMs();
        Date date = new Date();
        date.setTime(expiresAt);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        AuthResponse authResponse = AuthResponse.builder().access_token(jwt).refresh_token(refreshToken.getToken())
                .expiresAt(dateFormat.format(date))
                .username(userDetails.getUsername()).build();

        return MessageResponse.builder().message("Berhasil login").data(authResponse).build();
    }

    @Override
    public MessageResponse register(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        Set<String> strRoles = registerRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));
                        roles.add(adminRole);

                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                                .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));
                        roles.add(modRole);

                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new ResourceNotFoundException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        this.userRepository.save(user);

        return MessageResponse.builder().message("Successs create user").data(user).statusCode(200).build();

    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return this.userRepository.findByUsername(authentication.getName())
                .orElseThrow(
                        () -> new UsernameNotFoundException("User name not found - " + authentication.getName()));
    }

    @Override
    public TokenRefreshResponse refreshToken(String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccessToken = jwtProvider.generateTokenFromUsername(user.getUsername());

                    return new TokenRefreshResponse(newAccessToken, refreshToken);
                })
                .orElseThrow(() -> new TokenRefreshException(refreshToken,
                        "Invalid or expired refresh token"));
    }

    @Override
    public MessageResponse logout() {
        refreshTokenService.deleteByUserId(getCurrentUser().getId());
        return MessageResponse.builder().message("Logout success").statusCode(200).build();
    }

}
