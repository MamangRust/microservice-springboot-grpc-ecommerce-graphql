package com.sanedge.ecommerce.auth.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis-backed storage for the email verification codes.
 *
 * <p>Port of the blueprint's use of {@code RedisService} in {@code register} /
 * {@code verifyEmailByCode}. Two keys are written per registration, both with the
 * same 15-minute TTL:
 * <ul>
 *   <li>{@code verification:<email>} → the code (lets a user re-request or a
 *       later flow look up the outstanding code by address),</li>
 *   <li>{@code verification_code:<code>} → the email (what {@code verifyCode}
 *       reverses, since the gRPC request only carries the code).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class VerificationCodeStore {

    private static final String EMAIL_KEY_PREFIX = "verification:";
    private static final String CODE_KEY_PREFIX = "verification_code:";

    /** Matches the blueprint's 900 seconds. */
    static final Duration TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;

    /** Stores both lookups for a freshly issued code. */
    public void save(String email, String code) {
        redis.opsForValue().set(EMAIL_KEY_PREFIX + email, code, TTL);
        redis.opsForValue().set(CODE_KEY_PREFIX + code, email, TTL);
    }

    /** Returns the email the code was issued for, or {@code null} when unknown/expired. */
    public String resolveEmail(String code) {
        return redis.opsForValue().get(CODE_KEY_PREFIX + code);
    }

    /** Consumes a code: removes the code lookup and the outstanding code for the email. */
    public void consume(String email, String code) {
        redis.delete(CODE_KEY_PREFIX + code);
        redis.delete(EMAIL_KEY_PREFIX + email);
    }
}
