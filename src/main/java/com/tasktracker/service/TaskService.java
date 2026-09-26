package com.tasktracker.service;

import com.tasktracker.model.Task;
import com.tasktracker.model.TaskStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class TaskService {

    private final List<Task> tasks = new ArrayList<>();

    // Monotonically increasing counter. The previous implementation scanned
    // the whole list on every insert (O(n) per add, and it also reused an id
    // after every task with that id had been deleted). A counter is O(1)
    // and ids are never reused.
    private final AtomicLong idGenerator = new AtomicLong(0);

    public Task addTask(String subject, String body) {

        Task task = new Task(subject, body);

        task.setId(idGenerator.incrementAndGet());

        tasks.add(task);

        return task;
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public Task getTaskById(long id) {

        for (Task task : tasks) {

            if (task.getId() == id) {
                return task;
            }
        }

        return null;
    }

    public boolean updateTask(
            long id,
            String subject,
            String body
    ) {

        Task task = getTaskById(id);

        if (task == null) {
            return false;
        }

        task.setSubject(subject);
        task.setBody(body);

        return true;
    }

    public boolean deleteTask(long id) {

        Task task = getTaskById(id);

        if (task == null) {
            return false;
        }

        return tasks.remove(task);
    }

    public boolean completeTask(long id) {

        Task task = getTaskById(id);

        if (task == null) {
            return false;
        }

        task.complete();

        return true;
    }

    public boolean reopenTask(long id) {

        Task task = getTaskById(id);

        if (task == null) {
            return false;
        }

        task.reopen();

        return true;
    }

    public boolean toggleFlag(long id) {

        Task task = getTaskById(id);

        if (task == null) {
            return false;
        }

        task.setFlagged(
                !task.isFlagged()
        );

        return true;
    }

    public List<Task> search(String query) {

        if (query == null || query.isBlank()) {
            return getAllTasks();
        }

        String searchText = query.trim().toLowerCase();

        return tasks.stream()
                .filter(task -> matches(task, searchText))
                .toList();
    }

    public List<Task> getPendingTasks() {

        return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .toList();
    }

    public List<Task> getCompletedTasks() {

        return tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED)
                .toList();
    }

    public List<Task> getFlaggedTasks() {

        return tasks.stream()
                .filter(Task::isFlagged)
                .toList();
    }

    public List<Task> getOverdueTasks() {

        return tasks.stream()
                .filter(Task::isOverdue)
                .toList();
    }

    public List<Task> getDueTodayTasks() {

        return tasks.stream()
                .filter(Task::isDueToday)
                .toList();
    }

    /**
     * Applies a sidebar filter and a free-text search together, so the two
     * never have to be combined ad-hoc in the UI layer (the previous UI
     * duplicated this "which list, then filter by text" logic).
     */
    public List<Task> getTasks(FilterType filter, String query) {

        List<Task> base = switch (filter) {
            case ALL -> getAllTasks();
            case PENDING -> getPendingTasks();
            case COMPLETED -> getCompletedTasks();
            case FLAGGED -> getFlaggedTasks();
            case OVERDUE -> getOverdueTasks();
            case DUE_TODAY -> getDueTodayTasks();
        };

        if (query == null || query.isBlank()) {
            return base;
        }

        String searchText = query.trim().toLowerCase();

        return base.stream()
                .filter(task -> matches(task, searchText))
                .toList();
    }

    private boolean matches(Task task, String searchText) {

        return containsIgnoreCase(task.getSubject(), searchText)
                || containsIgnoreCase(task.getBody(), searchText)
                || task.getTags().stream()
                        .anyMatch(tag -> containsIgnoreCase(tag, searchText));
    }

    private boolean containsIgnoreCase(
            String text,
            String searchText
    ) {

        if (text == null) {
            return false;
        }

        return text.toLowerCase().contains(searchText);
    }
}
