package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Manages fast in-memory behavioural state in Redis.
 *
 * Key schema:
 *   rr:user:{userId}:streak:morning_miss      → consecutive morning miss count
 *   rr:user:{userId}:streak:activity_miss     → consecutive activity miss count
 *   rr:user:{userId}:streak:evening_miss      → consecutive evening miss count
 *   rr:user:{userId}:rolling7:med_taken       → medication taken count (last 7 days)
 *   rr:user:{userId}:rolling7:meal_logged     → meal logged count (last 7 days)
 *   rr:user:{userId}:current_risk_score       → latest computed risk score
 *   rr:user:{userId}:current_state            → latest state string
 *   rr:user:{userId}:last_explanation         → latest explanation JSON
 *   rr:user:{userId}:reentry_mode             → "true" / "false"
 *   rr:user:{userId}:lock:intervention:{type} → dedup lock (TTL=60min)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStateService {

    private final StringRedisTemplate redis;

    private static final String PREFIX = "rr:user:";

    // ── Streak management ────────────────────────────────────────────────────

    public void incrementStreak(UUID userId, String streakName) {
        String key = key(userId, "streak:" + streakName);
        redis.opsForValue().increment(key);
        redis.expire(key, Duration.ofDays(30));
    }

    public void resetStreak(UUID userId, String streakName) {
        redis.opsForValue().set(key(userId, "streak:" + streakName), "0");
    }

    public int getStreak(UUID userId, String streakName) {
        String val = redis.opsForValue().get(key(userId, "streak:" + streakName));
        return val == null ? 0 : Integer.parseInt(val);
    }

    // ── Rolling 7-day counters ───────────────────────────────────────────────

    public void incrementRolling7(UUID userId, String signalName) {
        String key = key(userId, "rolling7:" + signalName);
        redis.opsForValue().increment(key);
        redis.expire(key, Duration.ofDays(8));
    }

    public int getRolling7Count(UUID userId, String signalName) {
        String val = redis.opsForValue().get(key(userId, "rolling7:" + signalName));
        return val == null ? 0 : Integer.parseInt(val);
    }

    public void setRolling7Count(UUID userId, String signalName, int count) {
        String key = key(userId, "rolling7:" + signalName);
        redis.opsForValue().set(key, String.valueOf(count));
        redis.expire(key, Duration.ofDays(8));
    }

    // ── Current risk state cache ─────────────────────────────────────────────

    public void cacheRiskState(UUID userId, int score, String state, String explanationJson) {
        redis.opsForValue().set(key(userId, "current_risk_score"), String.valueOf(score), Duration.ofHours(24));
        redis.opsForValue().set(key(userId, "current_state"), state, Duration.ofHours(24));
        redis.opsForValue().set(key(userId, "last_explanation"), explanationJson, Duration.ofHours(24));
    }

    public String getCachedState(UUID userId) {
        return redis.opsForValue().get(key(userId, "current_state"));
    }

    public Integer getCachedRiskScore(UUID userId) {
        String val = redis.opsForValue().get(key(userId, "current_risk_score"));
        return val == null ? null : Integer.parseInt(val);
    }

    // ── Re-entry mode ────────────────────────────────────────────────────────

    public void setReentryMode(UUID userId, boolean active) {
        redis.opsForValue().set(key(userId, "reentry_mode"), String.valueOf(active), Duration.ofDays(7));
    }

    public boolean isReentryModeActive(UUID userId) {
        String val = redis.opsForValue().get(key(userId, "reentry_mode"));
        return "true".equals(val);
    }

    // ── Intervention dedup lock ──────────────────────────────────────────────

    public boolean acquireInterventionLock(UUID userId, String interventionType, Duration ttl) {
        String key = key(userId, "lock:intervention:" + interventionType);
        Boolean set = redis.opsForValue().setIfAbsent(key, "locked", ttl);
        return Boolean.TRUE.equals(set);
    }

    // ── Signal streak update helper ──────────────────────────────────────────

    public void updateSignalStreaks(UUID userId,
                                    boolean morningDone,
                                    boolean activityDone,
                                    boolean eveningDone,
                                    boolean medDone,
                                    boolean mealDone) {
        if (morningDone) resetStreak(userId, "morning_miss");
        else             incrementStreak(userId, "morning_miss");

        if (activityDone) resetStreak(userId, "activity_miss");
        else              incrementStreak(userId, "activity_miss");

        if (eveningDone) resetStreak(userId, "evening_miss");
        else             incrementStreak(userId, "evening_miss");

        if (medDone)  incrementRolling7(userId, "med_taken");
        if (mealDone) incrementRolling7(userId, "meal_logged");
    }

    private String key(UUID userId, String suffix) {
        return PREFIX + userId + ":" + suffix;
    }
}
