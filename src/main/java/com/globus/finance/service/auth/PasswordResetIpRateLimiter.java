package com.globus.finance.service.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class PasswordResetIpRateLimiter {

    private static final int MAX_REQUESTS = 5;

    private static final Duration WINDOW =
            Duration.ofMinutes(10);

    private static final int MAX_TRACKED_IPS = 10_000;

    private final Map<String, RequestWindow> requests =
            new HashMap<>();

    public synchronized boolean allow(String ipAddress) {

        String ip = (ipAddress == null || ipAddress.isBlank())
                ? "unknown"
                : ipAddress;

        Instant now = Instant.now();

        RequestWindow window = requests.get(ip);

        if (window != null && now.isBefore(window.expiresAt())) {

            if (window.count() >= MAX_REQUESTS) {
                return false;
            }

            requests.put(
                    ip,
                    new RequestWindow(
                            window.count() + 1,
                            window.expiresAt()
                    )
            );

            return true;
        }

        if (window == null && requests.size() >= MAX_TRACKED_IPS) {

            requests.entrySet().removeIf(entry ->
                    !now.isBefore(entry.getValue().expiresAt())
            );

            if (requests.size() >= MAX_TRACKED_IPS) {
                return false;
            }
        }

        requests.put(
                ip,
                new RequestWindow(
                        1,
                        now.plus(WINDOW)
                )
        );

        return true;
    }

    private record RequestWindow(
            int count,
            Instant expiresAt
    ) {
    }
}