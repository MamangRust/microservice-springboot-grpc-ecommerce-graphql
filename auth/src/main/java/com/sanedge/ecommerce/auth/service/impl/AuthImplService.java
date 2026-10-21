package com.sanedge.ecommerce.auth.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanedge.ecommerce.auth.domain.requests.AuthRequest;
import com.sanedge.ecommerce.auth.domain.requests.RegisterRequest;
import com.sanedge.ecommerce.auth.domain.requests.ResetPasswordRequest;
import com.sanedge.ecommerce.auth.domain.responses.TokenResponse;
import com.sanedge.ecommerce.auth.domain.responses.UserResponse;
import com.sanedge.ecommerce.auth.models.AuthOutbox;
import com.sanedge.ecommerce.auth.models.RefreshToken;
import com.sanedge.ecommerce.auth.models.ResetToken;
import com.sanedge.ecommerce.auth.repository.AuthOutboxRepository;
import com.sanedge.ecommerce.auth.repository.RefreshTokenCommandRepository;
import com.sanedge.ecommerce.auth.repository.RefreshTokenQueryRepository;
import com.sanedge.ecommerce.auth.repository.ResetTokenRepository;
import com.sanedge.ecommerce.auth.service.AuthService;
import com.sanedge.ecommerce.auth.service.VerificationCodeStore;
import com.sanedge.ecommerce.common.adapter.model.User;
import com.sanedge.ecommerce.common.adapter.model.VerifyPasswordResult;
import com.sanedge.ecommerce.common.adapter.role.RolePort;
import com.sanedge.ecommerce.common.adapter.support.AdapterException;
import com.sanedge.ecommerce.common.adapter.user.UserPort;
import com.sanedge.ecommerce.common.domain.responses.api.ApiResponse;
import com.sanedge.ecommerce.common.exception.ResourceNotFoundException;
import com.sanedge.ecommerce.common.observability.TracingMetrics;
import com.sanedge.ecommerce.common.outbox.OutboxEventFactory;
import com.sanedge.ecommerce.common.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Auth domain logic. Password verification and user/role lookups are delegated
 * to the {@code user} and {@code role} services through their ports — this
 * module owns no {@code AuthenticationManager}/{@code UserDetailsService} and
 * stores only the refresh tokens locally.
 *
 * <p>Email side effects (welcome, forgot-password, verify-code confirmation) are
 * never sent inline: each is written to {@code auth_outbox} in the same
 * transaction as the state change, then relayed to Kafka by
 * {@link com.sanedge.ecommerce.auth.service.AuthOutboxPublisher}. Topic names are
 * the exact strings the email-service consumes.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AuthImplService implements AuthService {

    private static final String DEFAULT_ROLE = "ROLE_ADMIN";

    private static final String TOPIC_AUTH_REGISTER = "email-service-topic-auth-register";
    private static final String TOPIC_AUTH_FORGOT_PASSWORD = "email-service-topic-auth-forgot-password";
    private static final String TOPIC_AUTH_VERIFY_CODE_SUCCESS = "email-service-topic-auth-verify-code-success";

    private static final long RESET_TOKEN_TTL_MS = 900_000L; // 15 minutes

    private final UserPort userPort;
    private final RolePort rolePort;
    private final JwtProvider jwtProvider;
    private final RefreshTokenQueryRepository refreshTokenQueryRepository;
    private final RefreshTokenCommandRepository refreshTokenCommandRepository;
    private final ResetTokenRepository resetTokenRepository;
    private final AuthOutboxRepository authOutboxRepository;
    private final VerificationCodeStore verificationCodeStore;
    private final TracingMetrics tracingMetrics;

    @Override
    public ApiResponse<TokenResponse> login(AuthRequest loginRequest) {
        return tracingMetrics.traceAndMeasure("loginUser", "login", () -> {
            VerifyPasswordResult verification = userPort.verifyPassword(
                    loginRequest.getEmail(), loginRequest.getPassword());

            if (!verification.valid() || verification.user() == null) {
                throw new IllegalArgumentException("Invalid email or password");
            }

            User user = verification.user();
            String email = user.email();
            Long userId = (long) user.id();

            String accessToken = jwtProvider.generateAccessToken(email);
            String refreshTokenStr = jwtProvider.generateRefreshToken(email);

            RefreshToken refreshTokenEntity = refreshTokenQueryRepository.findByUserId(userId)
                    .orElseGet(RefreshToken::new);
            refreshTokenEntity.setUserId(userId);
            refreshTokenEntity.setToken(refreshTokenStr);
            refreshTokenEntity.setExpiration(
                    new Timestamp(System.currentTimeMillis() + jwtProvider.getJwtRefreshExpirationMs()));
            refreshTokenEntity = refreshTokenCommandRepository.save(refreshTokenEntity);

            TokenResponse tokenResponse = TokenResponse.builder()
                    .access_token(accessToken)
                    .refresh_token(refreshTokenEntity.getToken())
                    .build();

            return ApiResponse.<TokenResponse>builder()
                    .status("success")
                    .message("Login successful")
                    .data(tokenResponse)
                    .build();
        });
    }

    @Override
    public ApiResponse<UserResponse> register(RegisterRequest registerRequest) {
        return tracingMetrics.traceAndMeasure("registerUser", "register", () -> {
            if (emailAlreadyRegistered(registerRequest.getEmail())) {
                log.error("❌ [REGISTER] Email already taken | Email: {}", registerRequest.getEmail());
                throw new IllegalArgumentException("Email already registered");
            }

            // Memastikan role default ada; melempar ResourceNotFoundException bila tidak.
            rolePort.findByName(DEFAULT_ROLE);

            User created;
            try {
                created = userPort.createUser(new UserPort.RegisterData(
                        registerRequest.getFirstname(),
                        registerRequest.getLastname(),
                        registerRequest.getEmail(),
                        registerRequest.getPassword(),
                        registerRequest.getConfirmPassword()));
            } catch (AdapterException e) {
                throw new IllegalArgumentException(e.getMessage());
            }

            // Issue the verification code + enqueue the welcome email. Both happen
            // inside the register transaction so the code and the outbox row either
            // both land or neither does.
            String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            verificationCodeStore.save(created.email(), verificationCode);
            sendWelcomeEmail(created, verificationCode);

            return ApiResponse.<UserResponse>builder()
                    .status("success")
                    .message("User registered successfully")
                    .data(UserResponse.from(created))
                    .build();
        });
    }

    @Override
    public ApiResponse<TokenResponse> refreshToken(String refreshToken) {
        return tracingMetrics.traceAndMeasure("refreshToken", "refresh", () -> {
            RefreshToken storedToken = refreshTokenQueryRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

            if (storedToken.getExpiration().before(new Date())) {
                log.warn("❌ [REFRESH TOKEN] Token expired | Token: {}", refreshToken);
                throw new IllegalArgumentException("Refresh token expired");
            }

            User user = userPort.findById(storedToken.getUserId().intValue());

            String newAccessToken = jwtProvider.generateAccessToken(user.email());

            Date newExpiration = Date.from(Instant.now().plus(30, ChronoUnit.DAYS));
            storedToken.setExpiration(new Timestamp(newExpiration.getTime()));
            refreshTokenCommandRepository.save(storedToken);

            TokenResponse tokenResponse = TokenResponse.builder()
                    .access_token(newAccessToken)
                    .refresh_token(refreshToken)
                    .build();

            return ApiResponse.<TokenResponse>builder()
                    .status("success")
                    .message("Access token refreshed successfully")
                    .data(tokenResponse)
                    .build();
        });
    }

    @Override
    public ApiResponse<UserResponse> getCurrentUser(Integer userId) {
        return tracingMetrics.traceAndMeasure("getMe", "get_me", () -> {
            User user = userPort.findById(userId);

            return ApiResponse.<UserResponse>builder()
                    .status("success")
                    .message("Current user fetched successfully")
                    .data(UserResponse.from(user))
                    .build();
        });
    }

    private boolean emailAlreadyRegistered(String email) {
        try {
            userPort.findByEmail(email);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> forgotPassword(String email) {
        return tracingMetrics.traceAndMeasure("forgotPassword", "forgot_password", () -> {
            User user;
            try {
                user = userPort.findByEmail(email);
            } catch (ResourceNotFoundException e) {
                throw new IllegalArgumentException("User not found");
            }

            String token = UUID.randomUUID().toString();

            // One outstanding token per user: drop the previous row before issuing.
            resetTokenRepository.deleteByUserId((long) user.id());
            ResetToken resetToken = new ResetToken();
            resetToken.setUserId((long) user.id());
            resetToken.setToken(token);
            resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + RESET_TOKEN_TTL_MS));
            resetTokenRepository.save(resetToken);

            sendForgotPasswordEmail(user, token);

            return ApiResponse.<Void>builder()
                    .status("success")
                    .message("Password reset email sent successfully")
                    .build();
        });
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        return tracingMetrics.traceAndMeasure("resetPassword", "reset_password", () -> {
            if (!resetPasswordRequest.getPassword().equals(resetPasswordRequest.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }

            ResetToken resetToken = resetTokenRepository.findByToken(resetPasswordRequest.getToken())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

            if (resetToken.getExpiration().before(new Date())) {
                throw new IllegalArgumentException("Invalid or expired reset token");
            }

            userPort.updatePassword(resetToken.getUserId().intValue(), resetPasswordRequest.getPassword());
            resetTokenRepository.delete(resetToken);

            return ApiResponse.<Void>builder()
                    .status("success")
                    .message("Password reset successfully")
                    .build();
        });
    }

    @Override
    public ApiResponse<Void> verifyCode(String code) {
        return tracingMetrics.traceAndMeasure("verifyEmailByCode", "verify_email", () -> {
            String email = verificationCodeStore.resolveEmail(code);
            if (email == null) {
                throw new IllegalArgumentException("Invalid or expired verification code");
            }

            verificationCodeStore.consume(email, code);
            sendVerifyCodeSuccessEmail(email);

            return ApiResponse.<Void>builder()
                    .status("success")
                    .message("Email verified successfully")
                    .build();
        });
    }

    /** Enqueues the welcome email carrying the verification code. */
    private void sendWelcomeEmail(User user, String code) {
        String subject = "Welcome to Quarkus Modular Monolith";
        String body = String.format(
                "Hello %s %s,\n\nWelcome to our platform! Use the following code to verify your email address:\n\n%s\n\nRegards,\nSupport Team",
                user.firstname(), user.lastname(), code);

        enqueueEmail(TOPIC_AUTH_REGISTER, user.email(), subject, body);
    }

    /** Enqueues the password-reset email carrying the one-time token. */
    private void sendForgotPasswordEmail(User user, String token) {
        String subject = "Reset Password Verification";
        String body = String.format(
                "Hello %s %s,\n\nYou have requested a password reset. Use the following token to reset your password:\n\n%s\n\nThis token will expire in 15 minutes.\n\nRegards,\nSupport Team",
                user.firstname(), user.lastname(), token);

        enqueueEmail(TOPIC_AUTH_FORGOT_PASSWORD, user.email(), subject, body);
    }

    /**
     * Enqueues the post-verification confirmation. The blueprint never produced
     * this topic, so the payload is the shared envelope built from the email the
     * code belonged to.
     */
    private void sendVerifyCodeSuccessEmail(String email) {
        String subject = "Email Verified Successfully";
        String body = "Hello,\n\nYour email address has been verified successfully. "
                + "You can now use all features of your account.\n\nRegards,\nSupport Team";

        enqueueEmail(TOPIC_AUTH_VERIFY_CODE_SUCCESS, email, subject, body);
    }

    /** Writes one email event to the outbox; the relay publishes it to Kafka. */
    private void enqueueEmail(String topic, String email, String subject, String body) {
        OutboxEventFactory.Envelope envelope = OutboxEventFactory.email(topic, email, subject, body);

        AuthOutbox event = new AuthOutbox();
        event.setEventId(envelope.eventId());
        event.setTopic(topic);
        event.setEventKey(email);
        event.setPayload(envelope.payload());
        authOutboxRepository.enqueue(event);
    }
}
