package com.banking.ledger.reconciliation.security;

import com.banking.ledger.reconciliation.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory token-bucket limiter backed by Bucket4j. Buckets are keyed by
 * endpoint family and caller (IP for anonymous requests, username otherwise).
 * <p>
 * This is per-instance state: with more than one application instance, move the
 * limits to a shared store or enforce them at the gateway.
 */
@Service
public class RateLimitService {

    private static final long IDLE_EVICTION_NANOS = Duration.ofMinutes(10).toNanos();
    private static final long CLEANUP_INTERVAL = 1024;

    private final Map<String, Entry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong attempts = new AtomicLong();

    public boolean tryConsume(String key, AppProperties.RateLimit.Bucket limit) {
        Entry entry = buckets.computeIfAbsent(key, ignored -> new Entry(limit));
        entry.lastAccess = System.nanoTime();
        boolean allowed = entry.bucket.tryConsume(1);
        evictIdleOccasionally();
        return allowed;
    }

    private void evictIdleOccasionally() {
        if (attempts.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        long cutoff = System.nanoTime() - IDLE_EVICTION_NANOS;
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess < cutoff);
    }

    private static Bucket newBucket(AppProperties.RateLimit.Bucket limit) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.capacity())
                .refillGreedy(limit.capacity(), limit.period())
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private static final class Entry {

        private final Bucket bucket;
        private volatile long lastAccess = System.nanoTime();

        private Entry(AppProperties.RateLimit.Bucket limit) {
            this.bucket = newBucket(limit);
        }
    }
}