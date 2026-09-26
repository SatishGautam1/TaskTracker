package com.tasktracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Task {

    private long id;
    private String subject;
    private String body;

    private TaskStatus status;
    private Priority priority;

    private boolean flagged;
    private List<String> tags = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime pendingSince;
    private LocalDateTime dueDate;
    private LocalDateTime reminderAt;
    private LocalDateTime completedAt;

    public Task() {
        this.status = TaskStatus.PENDING;
        this.priority = Priority.MEDIUM;
        this.flagged = false;

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.pendingSince = LocalDateTime.now();
    }

    public Task(String subject, String body) {
        this();

        this.subject = subject;
        this.body = body;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
        touch();
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
        touch();
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        touch();
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
        touch();
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
        touch();
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        touch();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getPendingSince() {
        return pendingSince;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
        touch();
    }

    public LocalDateTime getReminderAt() {
        return reminderAt;
    }

    public void setReminderAt(LocalDateTime reminderAt) {
        this.reminderAt = reminderAt;
        touch();
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        touch();
    }

    public void reopen() {
        this.status = TaskStatus.PENDING;
        this.completedAt = null;
        this.pendingSince = LocalDateTime.now();
        touch();
    }

    /**
     * A task is overdue only while it is still pending; a completed task
     * is never overdue even if it was finished after its due date.
     */
    public boolean isOverdue() {
        return dueDate != null
                && status == TaskStatus.PENDING
                && dueDate.isBefore(LocalDateTime.now());
    }

    public boolean isDueToday() {
        if (dueDate == null || status == TaskStatus.COMPLETED) {
            return false;
        }

        LocalDate today = LocalDate.now();

        return dueDate.toLocalDate().isEqual(today);
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
