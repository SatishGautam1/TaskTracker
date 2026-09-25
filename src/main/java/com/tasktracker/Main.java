package com.tasktracker;

import com.tasktracker.model.Task;
import com.tasktracker.model.TaskStatus;
import com.tasktracker.service.TaskService;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application {

    private final TaskService taskService = new TaskService();

    private final ObservableList<Task> visibleTasks =
            FXCollections.observableArrayList();

    private final ListView<Task> taskListView = new ListView<>();

    private final Label totalLabel = new Label("0");
    private final Label pendingLabel = new Label("0");
    private final Label completedLabel = new Label("0");

    private final TextField searchField = new TextField();

    @Override
    public void start(Stage stage) {

        BorderPane root = new BorderPane();

        root.setTop(createHeader());
        root.setLeft(createSidebar());
        root.setCenter(createTaskArea());

        updateDashboard();

        Scene scene = new Scene(root, 1100, 700);

        stage.setTitle("Task Tracker");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createHeader() {

        Label title = new Label("Task Tracker");
        title.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;"
        );

        Label subtitle = new Label("Manage your tasks and stay organized");
        subtitle.setStyle(
                "-fx-text-fill: #666666;" +
                "-fx-font-size: 13px;"
        );

        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(20));
        header.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-width: 0 0 1 0;"
        );

        return header;
    }

    private VBox createSidebar() {

        Button allButton = new Button("All Tasks");
        Button pendingButton = new Button("Pending");
        Button completedButton = new Button("Completed");
        Button flaggedButton = new Button("Flagged");

        allButton.setMaxWidth(Double.MAX_VALUE);
        pendingButton.setMaxWidth(Double.MAX_VALUE);
        completedButton.setMaxWidth(Double.MAX_VALUE);
        flaggedButton.setMaxWidth(Double.MAX_VALUE);

        allButton.setOnAction(e -> refreshTasks(
                taskService.getAllTasks()
        ));

        pendingButton.setOnAction(e -> refreshTasks(
                taskService.getPendingTasks()
        ));

        completedButton.setOnAction(e -> refreshTasks(
                taskService.getCompletedTasks()
        ));

        flaggedButton.setOnAction(e -> refreshTasks(
                taskService.getAllTasks()
                        .stream()
                        .filter(Task::isFlagged)
                        .toList()
        ));

        Label dashboardTitle = new Label("DASHBOARD");
        dashboardTitle.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #777777;"
        );

        VBox stats = new VBox(
                10,
                createStatBox("TOTAL", totalLabel),
                createStatBox("PENDING", pendingLabel),
                createStatBox("COMPLETED", completedLabel)
        );

        VBox sidebar = new VBox(
                12,
                dashboardTitle,
                stats,
                new Separator(),
                allButton,
                pendingButton,
                completedButton,
                flaggedButton
        );

        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);
        sidebar.setStyle(
                "-fx-background-color: #f5f6f8;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-width: 0 1 0 0;"
        );

        return sidebar;
    }

    private VBox createStatBox(String title, Label value) {

        Label titleLabel = new Label(title);

        titleLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: #777777;"
        );

        value.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;"
        );

        VBox box = new VBox(2, titleLabel, value);

        box.setPadding(new Insets(10));

        box.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;"
        );

        return box;
    }

    private VBox createTaskArea() {

        searchField.setPromptText("Search tasks...");

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        refreshTasks(taskService.search(newValue))
        );

        Button addButton = new Button("+ Add Task");

        addButton.setStyle(
                "-fx-background-color: #2563eb;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );

        addButton.setOnAction(e -> showAddTaskDialog());

        HBox toolbar = new HBox(
                10,
                searchField,
                addButton
        );

        HBox.setHgrow(searchField, Priority.ALWAYS);

        taskListView.setItems(visibleTasks);

        taskListView.setCellFactory(list ->
                new TaskCell()
        );

        VBox area = new VBox(
                15,
                toolbar,
                taskListView
        );

        area.setPadding(new Insets(20));

        VBox.setVgrow(taskListView, Priority.ALWAYS);

        return area;
    }

    private void showAddTaskDialog() {

        Dialog<Task> dialog = new Dialog<>();

        dialog.setTitle("Add Task");
        dialog.setHeaderText("Create a new task");

        ButtonType saveButton =
                new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                saveButton,
                ButtonType.CANCEL
        );

        TextField subjectField =
                new TextField();

        subjectField.setPromptText("Task subject");

        TextArea bodyArea =
                new TextArea();

        bodyArea.setPromptText(
                "Task description..."
        );

        bodyArea.setPrefRowCount(8);

        ComboBox<com.tasktracker.model.Priority> priorityBox =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                com.tasktracker.model.Priority.values()
                        )
                );

        priorityBox.setValue(
                com.tasktracker.model.Priority.MEDIUM
        );

        VBox content = new VBox(
                10,
                new Label("Subject"),
                subjectField,
                new Label("Description"),
                bodyArea,
                new Label("Priority"),
                priorityBox
        );

        content.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {

            if (button == saveButton) {

                String subject =
                        subjectField.getText().trim();

                String body =
                        bodyArea.getText().trim();

                if (subject.isEmpty()) {
                    return null;
                }

                Task task =
                        taskService.addTask(subject, body);

                task.setPriority(priorityBox.getValue());

                return task;
            }

            return null;
        });

        dialog.showAndWait().ifPresent(task -> {

            refreshTasks(taskService.getAllTasks());
            updateDashboard();
        });
    }

    private void refreshTasks(
            java.util.List<Task> tasks
    ) {

        visibleTasks.setAll(tasks);
    }

    private void updateDashboard() {

        int total =
                taskService.getAllTasks().size();

        int pending =
                taskService.getPendingTasks().size();

        int completed =
                taskService.getCompletedTasks().size();

        totalLabel.setText(
                String.valueOf(total)
        );

        pendingLabel.setText(
                String.valueOf(pending)
        );

        completedLabel.setText(
                String.valueOf(completed)
        );

        refreshTasks(
                taskService.getAllTasks()
        );
    }

    private class TaskCell extends ListCell<Task> {

        @Override
        protected void updateItem(
                Task task,
                boolean empty
        ) {

            super.updateItem(task, empty);

            if (empty || task == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            Label subject =
                    new Label(task.getSubject());

            subject.setStyle(
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;"
            );

            Label body =
                    new Label(task.getBody());

            body.setWrapText(true);

            Label priority =
                    new Label(
                            "Priority: "
                                    + task.getPriority()
                    );

            Label status =
                    new Label(
                            task.getStatus()
                                    == TaskStatus.COMPLETED
                                    ? "Completed"
                                    : "Pending"
                    );

            Button completeButton =
                    new Button(
                            task.getStatus()
                                    == TaskStatus.COMPLETED
                                    ? "Reopen"
                                    : "Complete"
                    );

            Button flagButton =
                    new Button(
                            task.isFlagged()
                                    ? "Unflag"
                                    : "Flag"
                    );

            Button deleteButton =
                    new Button("Delete");

            completeButton.setOnAction(e -> {

                if (task.getStatus()
                        == TaskStatus.COMPLETED) {

                    taskService.reopenTask(
                            task.getId()
                    );

                } else {

                    taskService.completeTask(
                            task.getId()
                    );
                }

                refreshTasks(
                        taskService.getAllTasks()
                );

                updateDashboard();
            });

            flagButton.setOnAction(e -> {

                taskService.toggleFlag(
                        task.getId()
                );

                refreshTasks(
                        taskService.getAllTasks()
                );
            });

            deleteButton.setOnAction(e -> {

                taskService.deleteTask(
                        task.getId()
                );

                refreshTasks(
                        taskService.getAllTasks()
                );

                updateDashboard();
            });

            HBox buttons =
                    new HBox(
                            8,
                            completeButton,
                            flagButton,
                            deleteButton
                    );

            VBox information =
                    new VBox(
                            6,
                            subject,
                            body,
                            priority,
                            status,
                            buttons
                    );

            information.setPadding(
                    new Insets(10)
            );

            setGraphic(information);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}