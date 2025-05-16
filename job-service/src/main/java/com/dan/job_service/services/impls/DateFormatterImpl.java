package com.dan.job_service.services.impls;

import org.springframework.stereotype.Component;

import com.dan.job_service.services.DateFormatter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class DateFormatterImpl implements DateFormatter {
    private Map<Long, Function<LocalDateTime, String>> strategyMap = new LinkedHashMap<>();

    public DateFormatterImpl() {
        strategyMap.put(60L, this::getSecondsAgo);
        strategyMap.put(3600L, this::getMinutesAgo);
        strategyMap.put(86400L, this::getHoursAgo);
        strategyMap.put(604800L, this::getDaysAgo);
        strategyMap.put(Long.MAX_VALUE, this::getTimePost);
    }

    private String getSecondsAgo(LocalDateTime createdAt) {
        long spreadTime = getSpreadTime(createdAt);
        return spreadTime + " giây trước";
    }

    private String getMinutesAgo(LocalDateTime createdAt) {
        long spreadTime = getSpreadTime(createdAt);
        return spreadTime / 60 + " phút trước";
    }

    private String getHoursAgo(LocalDateTime createdAt) {
        long spreadTime = getSpreadTime(createdAt);
        return spreadTime / 3600 + " giờ trước";
    }

    private String getDaysAgo(LocalDateTime createdAt) {
        long spreadTime = getSpreadTime(createdAt);
        return spreadTime / 86400 + " ngày trước";
    }

    private String getTimePost(LocalDateTime createdAt) {
        return createdAt.toString().substring(0, 10);
    }

    private Long getSpreadTime(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        return ChronoUnit.SECONDS.between(createdAt, now);
    }

    @Override
    public String formatDate(LocalDateTime createdAt) {
        for (Map.Entry<Long, Function<LocalDateTime, String>> entry : strategyMap.entrySet()) {
            if (getSpreadTime(createdAt) < entry.getKey()) {
                return entry.getValue().apply(createdAt);
            }
        }
        return null;
    }
}
