package com.sanedge.spring21.service;

import com.sanedge.spring21.domain.request.LoginRequest;
import com.sanedge.spring21.domain.request.RegisterRequest;
import com.sanedge.spring21.domain.response.MessageResponse;
import com.sanedge.spring21.domain.response.TokenRefreshResponse;

public interface AuthService {
    public MessageResponse login(LoginRequest loginRequest);

    public MessageResponse register(RegisterRequest registerRequest);

    public TokenRefreshResponse refreshToken(String refreshToken);

    public MessageResponse logout();
}
