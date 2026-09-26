package com.tasktracker.service;

import com.tasktracker.model.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskServiceTest {

    @Test
    void addTaskAssignsIncreasingUniqueIds() {

        TaskService service = new TaskService();

        Task first = service.addTask("First", "body");
        Task second = service.addTask("Second", "body");

        assertNotEquals(first.getId(), second.getId());
        assertTrue(second.getId() > first.getId());
    }

    @Test
    void idsAreNotReusedAfterDeletion() {

        TaskService service = new TaskService();

        Task first = service.addTask("First", "body");
        service.deleteTask(first.getId());

        Task second = service.addTask("Second", "body");

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void completeAndReopenRoundTrip() {

        TaskService service = new TaskService();
        Task task = service.addTask("Task", "body");

        assertTrue(service.completeTask(task.getId()));
        assertEquals(1, service.getCompletedTasks().size());

        assertTrue(service.reopenTask(task.getId()));
        assertEquals(1, service.getPendingTasks().size());
        assertNull(task.getCompletedAt());
    }

    @Test
    void toggleFlagFlipsState() {

        TaskService service = new TaskService();
        Task task = service.addTask("Task", "body");

        assertFalse(task.isFlagged());

        service.toggleFlag(task.getId());
        assertTrue(task.isFlagged());

        service.toggleFlag(task.getId());
        assertFalse(task.isFlagged());
    }

    @Test
    void searchMatchesSubjectBodyAndTags() {

        TaskService service = new TaskService();

        Task task = service.addTask("Buy groceries", "milk and eggs");
        task.setTags(List.of("errands"));

        service.addTask("Write report", "quarterly numbers");

        assertEquals(1, service.search("groceries").size());
        assertEquals(1, service.search("eggs").size());
        assertEquals(1, service.search("errands").size());
        assertEquals(0, service.search("nonexistent").size());
    }

    @Test
    void overdueTasksAreOnlyPendingOnesPastDueDate() {

        TaskService service = new TaskService();

        Task overdue = service.addTask("Overdue", "");
        overdue.setDueDate(LocalDateTime.now().minusDays(1));

        Task completedButPastDue = service.addTask("Completed but past due", "");
        completedButPastDue.setDueDate(LocalDateTime.now().minusDays(1));
        service.completeTask(completedButPastDue.getId());

        Task future = service.addTask("Future", "");
        future.setDueDate(LocalDateTime.now().plusDays(1));

        List<Task> overdueTasks = service.getOverdueTasks();

        assertEquals(1, overdueTasks.size());
        assertEquals(overdue.getId(), overdueTasks.get(0).getId());
    }

    @Test
    void getTasksCombinesFilterAndSearch() {

        TaskService service = new TaskService();

        Task pendingMatch = service.addTask("Renew passport", "");
        Task completedMatch = service.addTask("Renew license", "");
        service.completeTask(completedMatch.getId());

        List<Task> result = service.getTasks(FilterType.PENDING, "renew");

        assertEquals(1, result.size());
        assertEquals(pendingMatch.getId(), result.get(0).getId());
    }
}
