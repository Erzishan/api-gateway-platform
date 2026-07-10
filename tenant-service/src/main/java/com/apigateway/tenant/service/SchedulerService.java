package com.apigateway.tenant.service;

import com.apigateway.tenant.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final ApiKeyRepository apiKeyRepository;

    // Runs every day at 2:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredApiKeys() {
        log.info("Scheduler: starting expired API key cleanup");

        List<com.apigateway.tenant.entity.ApiKey> allKeys =
                apiKeyRepository.findAll();

        AtomicInteger count = new AtomicInteger(0);

        allKeys.stream()
                .filter(key -> key.getExpiresAt() != null &&
                        key.getExpiresAt()
                                .isBefore(LocalDateTime.now()) &&
                        key.getRevokedAt() == null)
                .forEach(key -> {
                    key.setRevokedAt(LocalDateTime.now());
                    apiKeyRepository.save(key);
                    count.incrementAndGet();
                });

        log.info("Scheduler: revoked {} expired API keys",
                count.get());
    }

    // Runs every hour — logs system health
    @Scheduled(fixedRate = 3600000)
    public void logSystemHealth() {
        log.info("System health check — application running " +
                "at: {}", LocalDateTime.now());
    }
}
