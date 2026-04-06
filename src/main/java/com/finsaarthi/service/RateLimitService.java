package com.finsaarthi.service;

import com.finsaarthi.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class RateLimitService {

    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public void assertWithinLimit(String key, int maxAttempts, Duration window, String message) {
        int currentAttempts = currentAttemptCount(key, window);
        if (currentAttempts >= maxAttempts) {
            log.warn("Rate limit exceeded for key {}", key);
            throw new RateLimitExceededException(message);
        }
    }

    public int recordAttempt(String key, Duration window) {
        Deque<Instant> timestamps = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            pruneOldEntries(timestamps, window);
            timestamps.addLast(Instant.now());
            return timestamps.size();
        }
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private int currentAttemptCount(String key, Duration window) {
        Deque<Instant> timestamps = attempts.get(key);
        if (timestamps == null) {
            return 0;
        }

        synchronized (timestamps) {
            pruneOldEntries(timestamps, window);
            if (timestamps.isEmpty()) {
                attempts.remove(key);
                return 0;
            }
            return timestamps.size();
        }
    }

    private void pruneOldEntries(Deque<Instant> timestamps, Duration window) {
        Instant cutoff = Instant.now().minus(window);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.removeFirst();
        }
    }
}
