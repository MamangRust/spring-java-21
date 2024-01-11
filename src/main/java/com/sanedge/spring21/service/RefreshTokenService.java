package com.sanedge.spring21.service;

import java.util.Optional;

import com.sanedge.spring21.models.RefreshToken;

public interface RefreshTokenService {
    Optional<RefreshToken> findByToken(String token);

    RefreshToken createRefreshToken(Long userId);

    RefreshToken verifyExpiration(RefreshToken token);

    int deleteByUserId(Long userId);
}
