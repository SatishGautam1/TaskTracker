package com.tasktracker.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    private DateTimeUtil() {
    }

    public static String formatDate(LocalDateTime dateTime) {

        if (dateTime == null) {
            return null;
        }

        return dateTime.format(DATE_FORMAT);
    }

    /**
     * Human-readable "pending since" age, e.g. "2 hours", "3 days".
     */
    public static String pendingAge(LocalDateTime since) {

        if (since == null) {
            return "";
        }

        Duration duration = Duration.between(since, LocalDateTime.now());

        long minutes = duration.toMinutes();

        if (minutes < 1) {
            return "just now";
        }

        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }

        long hours = duration.toHours();

        if (hours < 24) {
            return hours + (hours == 1 ? " hour" : " hours");
        }

        long days = duration.toDays();

        return days + (days == 1 ? " day" : " days");
    }
}
