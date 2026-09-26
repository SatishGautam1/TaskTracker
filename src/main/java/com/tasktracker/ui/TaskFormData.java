package com.tasktracker.ui;

import com.tasktracker.model.Priority;

import java.time.LocalDateTime;
import java.util.List;

public record TaskFormData(
        String subject,
        String body,
        Priority priority,
        boolean flagged,
        LocalDateTime dueDate,
        LocalDateTime reminderAt,
        List<String> tags
) {
}
