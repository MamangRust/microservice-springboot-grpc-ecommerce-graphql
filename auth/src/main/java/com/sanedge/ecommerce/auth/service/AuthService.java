package com.sanedge.ecommerce.auth.service;

import com.sanedge.ecommerce.auth.domain.requests.AuthRequest;
import com.sanedge.ecommerce.auth.domain.requests.RegisterRequest;
import com.sanedge.ecommerce.auth.domain.requests.ResetPasswordRequest;
import com.sanedge.ecommerce.auth.domain.responses.TokenResponse;
import com.sanedge.ecommerce.auth.domain.responses.UserResponse;
import com.sanedge.ecommerce.common.domain.responses.api.ApiResponse;

public interface AuthService {
    ApiResponse<TokenResponse> login(AuthRequest loginRequest);

    ApiResponse<UserResponse> register(RegisterRequest registerRequest);

    ApiResponse<TokenResponse> refreshToken(String refreshToken);

    ApiResponse<UserResponse> getCurrentUser(Integer userId);

    /**
     * Issues a 15-minute reset token, stores it, and enqueues the reset email on
     * {@code email-service-topic-auth-forgot-password}.
     */
    ApiResponse<Void> forgotPassword(String email);

    /**
     * Consumes a reset token and sets the new password.
     */
    ApiResponse<Void> resetPassword(ResetPasswordRequest resetPasswordRequest);

    /**
     * Validates an email verification code (Redis) and consumes it; enqueues the
     * confirmation on {@code email-service-topic-auth-verify-code-success}.
     */
    ApiResponse<Void> verifyCode(String code);
}
