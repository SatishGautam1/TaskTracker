package com.tasktracker.service;

import com.tasktracker.model.Task;
import com.tasktracker.model.TaskStatus;

import java.util.ArrayList;
import java.util.List;

public class TaskService {

    private final List<Task> tasks = new ArrayList<>();

    public Task addTask(String subject, String body) {
        Task task = new Task(subject, body);
        task.setId(generateId());
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

    public boolean updateTask(long id, String subject, String body) {
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

        task.setFlagged(!task.isFlagged());
        return true;
    }

    public List<Task> search(String query) {

        if (query == null || query.isBlank()) {
            return getAllTasks();
        }

        String searchText = query.toLowerCase();

        return tasks.stream()
                .filter(task ->
                        task.getSubject().toLowerCase().contains(searchText)
                        || task.getBody().toLowerCase().contains(searchText)
                )
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

    private long generateId() {
        return tasks.stream()
                .mapToLong(Task::getId)
                .max()
                .orElse(0) + 1;
    }
}