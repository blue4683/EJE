package com.skala.miniproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application-analysis.yml 바인딩. Step 0 이후 B 단독 소유. */
@ConfigurationProperties(prefix = "analysis")
public record AnalysisProperties(
        String engine,
        String algorithmVersion,
        String engineVersion,
        int slots,
        int uploadSlots,
        int leaseSeconds,
        int heartbeatSeconds,
        int executionDeadlineSeconds,
        int callTimeoutSeconds,
        int autoRetryMax,
        String ffmpegPath,
        int decodeTimeoutSeconds,
        int uploadReceiveTimeoutSeconds
) {}
