package com.tasktracker;

import com.tasktracker.model.Task;
import com.tasktracker.service.FilterType;
import com.tasktracker.service.TaskService;
import com.tasktracker.ui.SidebarPanel;
import com.tasktracker.ui.TaskCard;
import com.tasktracker.ui.TaskEditorDialog;
import com.tasktracker.ui.TaskFormData;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public class Main extends Application {

    private final TaskService taskService = new TaskService();

    private final ObservableList<Task> visibleTasks = FXCollections.observableArrayList();
    private final ListView<Task> taskListView = new ListView<>();
    private final TextField searchField = new TextField();
    private final Label emptyStateLabel = new Label();

    private SidebarPanel sidebarPanel;

    private FilterType currentFilter = FilterType.ALL;

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        root.setTop(createHeader());
        root.setLeft(createSidebar());
        root.setCenter(createTaskArea());

        refresh();

        Scene scene = new Scene(root, 1150, 720);

        String stylesheet = getClass().getResource("/styles.css") == null
                ? null
                : getClass().getResource("/styles.css").toExternalForm();

        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
        }

        Image appIcon = loadLogo();

        if (appIcon != null) {
            stage.getIcons().add(appIcon);
        }

        stage.setTitle("Task Tracker");
        stage.setScene(scene);
        stage.show();
    }

    private Image loadLogo() {

        var stream = getClass().getResourceAsStream("/tasktracker-logo.png");

        if (stream == null) {
            return null;
        }

        return new Image(stream);
    }

    private VBox createHeader() {

        ImageView logo = new ImageView();
        Image logoImage = loadLogo();

        if (logoImage != null) {
            logo.setImage(logoImage);
            logo.setFitWidth(44);
            logo.setFitHeight(44);
            logo.setPreserveRatio(true);
        }

        Label title = new Label("Task Tracker");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Manage your tasks and stay organized");
        subtitle.getStyleClass().add("app-subtitle");

        HBox titleRow = new HBox(12, logo, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(4, titleRow, subtitle);
        header.setPadding(new Insets(20));
        header.getStyleClass().add("app-header");

        return header;
    }

    private SidebarPanel createSidebar() {

        sidebarPanel = new SidebarPanel(filter -> {
            currentFilter = filter;
            refresh();
        });

        return sidebarPanel;
    }

    private VBox createTaskArea() {

        searchField.setPromptText("Search tasks...");
        searchField.getStyleClass().add("search-field");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> refresh());

        Button addButton = new Button("+ Add Task");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> openEditor(null));

        HBox toolbar = new HBox(10, searchField, addButton);
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);

        taskListView.setItems(visibleTasks);
        taskListView.getStyleClass().add("task-list");

        taskListView.setCellFactory(list -> new TaskCard(
                this::toggleComplete,
                task -> {
                    taskService.toggleFlag(task.getId());
                    refresh();
                },
                this::openEditor,
                this::confirmDelete,
                this::performDelete
        ));

        emptyStateLabel.getStyleClass().add("empty-state");
        taskListView.setPlaceholder(emptyStateLabel);

        VBox area = new VBox(15, toolbar, taskListView);
        area.setPadding(new Insets(20));
        VBox.setVgrow(taskListView, javafx.scene.layout.Priority.ALWAYS);

        return area;
    }

    private void openEditor(Task existingTask) {

        TaskEditorDialog dialog = new TaskEditorDialog(existingTask);

        Optional<TaskFormData> result = dialog.showAndWait();

        result.ifPresent(formData -> applyFormData(existingTask, formData));
    }

    private void applyFormData(Task existingTask, TaskFormData formData) {

        Task task = existingTask;

        if (task == null) {
            task = taskService.addTask(formData.subject(), formData.body());
        } else {
            taskService.updateTask(task.getId(), formData.subject(), formData.body());
        }

        task.setPriority(formData.priority());
        task.setFlagged(formData.flagged());
        task.setDueDate(formData.dueDate());
        task.setReminderAt(formData.reminderAt());
        task.setTags(formData.tags());

        refresh();
    }

    private void toggleComplete(Task task) {

        if (task.getStatus() == com.tasktracker.model.TaskStatus.COMPLETED) {
            taskService.reopenTask(task.getId());
        } else {
            taskService.completeTask(task.getId());
        }

        refresh();
    }

    /**
     * Only asks the question - it never mutates task data itself. This is
     * called by {@link com.tasktracker.ui.TaskCard} before its delete
     * animation starts, so the confirmation dialog is never nested inside an
     * animation's finished-handler (see the comment on TaskCard's delete
     * button for why that ordering matters).
     */
    private boolean confirmDelete(Task task) {

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Task");
        confirmation.setHeaderText("Delete this task?");
        confirmation.setContentText(task.getSubject());

        ButtonType result = confirmation.showAndWait().orElse(ButtonType.CANCEL);

        return result == ButtonType.OK;
    }

    /**
     * Performs the actual removal, once the caller has already confirmed and
     * (if applicable) let any deletion animation finish.
     */
    private void performDelete(Task task) {
        taskService.deleteTask(task.getId());
        refresh();
    }

    private void refresh() {

        sidebarPanel.getDashboardPanel().update(taskService);
        sidebarPanel.updateCounts(taskService);

        visibleTasks.setAll(
                taskService.getTasks(currentFilter, searchField.getText())
        );

        emptyStateLabel.setText(emptyStateMessage());
    }

    private String emptyStateMessage() {

        boolean noTasksAtAll = taskService.getAllTasks().isEmpty();

        if (noTasksAtAll) {
            return "No tasks yet — create your first task to get started.";
        }

        String query = searchField.getText();

        if (query != null && !query.isBlank()) {
            return "No tasks match \"" + query.trim() + "\".";
        }

        if (currentFilter != FilterType.ALL) {
            return "No tasks in this filter right now.";
        }

        return "No tasks here yet.";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
