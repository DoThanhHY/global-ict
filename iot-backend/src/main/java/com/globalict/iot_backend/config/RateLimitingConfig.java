package com.globalict.iot_backend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitingConfig {

    public enum Tier {
        AUTH(5, Duration.ofMinutes(1)),
        COMMAND(10, Duration.ofMinutes(1)),
        WRITE(30, Duration.ofMinutes(1)),
        READ(100, Duration.ofMinutes(1));

        private final int capacity;
        private final Duration duration;

        Tier(int capacity, Duration duration) {
            this.capacity = capacity;
            this.duration = duration;
        }

        public Bucket newBucket() {
            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, duration));
            return Bucket.builder().addLimit(limit).build();
        }
    }

    public static class BucketRegistry {
        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        public Bucket resolve(String key, Tier tier) {
            return buckets.computeIfAbsent(key + ":" + tier.name(), k -> tier.newBucket());
        }

        public void clear() {
            buckets.clear();
        }
    }

    @Bean
    public BucketRegistry bucketRegistry() {
        return new BucketRegistry();
    }

    @FunctionalInterface
    public interface PathTierResolver {
        Tier resolve(String method, String path);
    }

    public static final PathTierResolver TIER_RESOLVER = (method, path) -> {
        if (path.matches("/api/auth/(login|register)")) {
            return Tier.AUTH;
        }
        if (path.matches("/api/devices/\\d+/command") && "POST".equalsIgnoreCase(method)) {
            return Tier.COMMAND;
        }
        if (path.matches("/api/devices(?:/\\d+)?") && isWrite(method)) {
            return Tier.WRITE;
        }
        if (path.matches("/api/thresholds(?:/\\d+)?") && isWrite(method)) {
            return Tier.WRITE;
        }
        if (path.matches("/api/devices") && "POST".equalsIgnoreCase(method)) {
            return Tier.WRITE;
        }
        if (path.matches("/api/thresholds/alerts/\\d+/resolve") && "POST".equalsIgnoreCase(method)) {
            return Tier.WRITE;
        }
        return Tier.READ;
    };

    private static boolean isWrite(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method);
    }
}
