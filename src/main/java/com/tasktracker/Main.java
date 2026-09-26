package com.tasktracker;

import com.tasktracker.model.Task;
import com.tasktracker.model.TaskStatus;
import com.tasktracker.service.TaskService;
import com.tasktracker.ui.MarkdownEditor;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private final TaskService taskService =
            new TaskService();

    private final ObservableList<Task> visibleTasks =
            FXCollections.observableArrayList();

    private final ListView<Task> taskListView =
            new ListView<>();

    private final Label totalLabel =
            new Label("0");

    private final Label pendingLabel =
            new Label("0");

    private final Label completedLabel =
            new Label("0");

    private final TextField searchField =
            new TextField();

    @Override
    public void start(Stage stage) {

        BorderPane root =
                new BorderPane();

        root.setTop(
                createHeader()
        );

        root.setLeft(
                createSidebar()
        );

        root.setCenter(
                createTaskArea()
        );

        updateDashboard();

        Scene scene =
                new Scene(
                        root,
                        1100,
                        700
                );

        Image appIcon =
                loadLogo();

        if (appIcon != null) {
            stage.getIcons().add(appIcon);
        }

        stage.setTitle("Task Tracker");
        stage.setScene(scene);
        stage.show();
    }

    private Image loadLogo() {

        var stream =
                getClass()
                        .getResourceAsStream(
                                "/tasktracker-logo.png"
                        );

        if (stream == null) {
            return null;
        }

        return new Image(stream);
    }

    private VBox createHeader() {

        Image logoImage =
                loadLogo();

        ImageView logo =
                new ImageView();

        if (logoImage != null) {

            logo.setImage(logoImage);

            logo.setFitWidth(48);
            logo.setFitHeight(48);
            logo.setPreserveRatio(true);
        }

        Label title =
                new Label("Task Tracker");

        title.setStyle(
                "-fx-font-size: 26px;" +
                "-fx-font-weight: bold;"
        );

        Label subtitle =
                new Label(
                        "Manage your tasks and stay organized"
                );

        subtitle.setStyle(
                "-fx-text-fill: #666666;" +
                "-fx-font-size: 13px;"
        );

        HBox titleRow =
                new HBox(
                        12,
                        logo,
                        title
                );

        titleRow.setAlignment(
                Pos.CENTER_LEFT
        );

        VBox header =
                new VBox(
                        4,
                        titleRow,
                        subtitle
                );

        header.setPadding(
                new Insets(20)
        );

        header.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-width: 0 0 1 0;"
        );

        return header;
    }

    private VBox createSidebar() {

        Button allButton =
                new Button("All Tasks");

        Button pendingButton =
                new Button("Pending");

        Button completedButton =
                new Button("Completed");

        Button flaggedButton =
                new Button("Flagged");

        allButton.setMaxWidth(
                Double.MAX_VALUE
        );

        pendingButton.setMaxWidth(
                Double.MAX_VALUE
        );

        completedButton.setMaxWidth(
                Double.MAX_VALUE
        );

        flaggedButton.setMaxWidth(
                Double.MAX_VALUE
        );

        allButton.setOnAction(
                event -> showAllTasks()
        );

        pendingButton.setOnAction(
                event ->
                        refreshTasks(
                                taskService.getPendingTasks()
                        )
        );

        completedButton.setOnAction(
                event ->
                        refreshTasks(
                                taskService.getCompletedTasks()
                        )
        );

        flaggedButton.setOnAction(
                event ->
                        refreshTasks(
                                taskService.getFlaggedTasks()
                        )
        );

        Label dashboardTitle =
                new Label("DASHBOARD");

        dashboardTitle.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #777777;"
        );

        VBox stats =
                new VBox(
                        10,
                        createStatBox(
                                "TOTAL",
                                totalLabel
                        ),
                        createStatBox(
                                "PENDING",
                                pendingLabel
                        ),
                        createStatBox(
                                "COMPLETED",
                                completedLabel
                        )
                );

        VBox sidebar =
                new VBox(
                        12,
                        dashboardTitle,
                        stats,
                        new Separator(),
                        allButton,
                        pendingButton,
                        completedButton,
                        flaggedButton
                );

        sidebar.setPadding(
                new Insets(20)
        );

        sidebar.setPrefWidth(220);

        sidebar.setStyle(
                "-fx-background-color: #f5f6f8;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-width: 0 1 0 0;"
        );

        return sidebar;
    }

    private VBox createStatBox(
            String title,
            Label value
    ) {

        Label titleLabel =
                new Label(title);

        titleLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: #777777;"
        );

        value.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;"
        );

        VBox box =
                new VBox(
                        2,
                        titleLabel,
                        value
                );

        box.setPadding(
                new Insets(10)
        );

        box.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #dddddd;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;"
        );

        return box;
    }

    private VBox createTaskArea() {

        searchField.setPromptText(
                "Search tasks..."
        );

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        refreshTasks(
                                taskService.search(newValue)
                        )
        );

        Button addButton =
                new Button("+ Add Task");

        addButton.setStyle(
                "-fx-background-color: #2563eb;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );

        addButton.setOnAction(
                event ->
                        showAddTaskDialog()
        );

        HBox toolbar =
                new HBox(
                        10,
                        searchField,
                        addButton
                );

        HBox.setHgrow(
                searchField,
                javafx.scene.layout.Priority.ALWAYS
        );

        taskListView.setItems(
                visibleTasks
        );

        taskListView.setCellFactory(
                list ->
                        new TaskCell()
        );

        VBox area =
                new VBox(
                        15,
                        toolbar,
                        taskListView
                );

        area.setPadding(
                new Insets(20)
        );

        return area;
    }

    private void showAllTasks() {

        String search =
                searchField
                        .getText()
                        .trim();

        if (search.isEmpty()) {

            refreshTasks(
                    taskService.getAllTasks()
            );

        } else {

            refreshTasks(
                    taskService.search(search)
            );
        }
    }

    private void showAddTaskDialog() {
        showTaskEditorDialog(null);
    }

    private void showEditTaskDialog(
            Task task
    ) {

        showTaskEditorDialog(task);
    }

    private void showTaskEditorDialog(
            Task existingTask
    ) {

        boolean editing =
                existingTask != null;

        Dialog<Void> dialog =
                new Dialog<>();

        dialog.setTitle(
                editing
                        ? "Edit Task"
                        : "Add Task"
        );

        dialog.setHeaderText(
                editing
                        ? "Edit task details"
                        : "Create a new task"
        );

        ButtonType saveButton =
                new ButtonType(
                        editing
                                ? "Save Changes"
                                : "Create Task",
                        ButtonBar.ButtonData.OK_DONE
                );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        saveButton,
                        ButtonType.CANCEL
                );

        TextField subjectField =
                new TextField();

        subjectField.setPromptText(
                "Task subject"
        );

        ComboBox<com.tasktracker.model.Priority> priorityBox =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                com.tasktracker.model.Priority.values()
)
                );

        priorityBox.setValue(
                com.tasktracker.model.Priority.MEDIUM
        );

        MarkdownEditor markdownEditor =
                new MarkdownEditor();

        if (editing) {

            subjectField.setText(
                    existingTask.getSubject()
            );

            priorityBox.setValue(
                    existingTask.getPriority()
            );

            markdownEditor.setMarkdown(
                    existingTask.getBody()
            );
        }

        VBox content =
                new VBox(
                        10,
                        new Label("Subject"),
                        subjectField,
                        new Label("Description"),
                        markdownEditor.getView(),
                        new Label("Priority"),
                        priorityBox
                );

        content.setPadding(
                new Insets(10)
        );

        content.setPrefWidth(900);
        content.setPrefHeight(600);

        VBox.setVgrow(
                markdownEditor.getView(),
                javafx.scene.layout.Priority.ALWAYS
        );

        dialog.getDialogPane()
                .setContent(content);

        /*
         * Prevent the dialog from closing when the
         * subject is empty.
         */
        var saveNode =
                dialog.getDialogPane()
                        .lookupButton(saveButton);

        saveNode.addEventFilter(
                javafx.event.ActionEvent.ACTION,
                event -> {

                    String subject =
                            subjectField
                                    .getText()
                                    .trim();

                    if (subject.isEmpty()) {

                        showError(
                                "Subject is required."
                        );

                        event.consume();
                    }
                }
        );

        dialog.setResultConverter(
                button -> {

                    if (button != saveButton) {
                        return null;
                    }

                    String subject =
                            subjectField
                                    .getText()
                                    .trim();

                    String body =
                            markdownEditor
                                    .getMarkdown();

                    com.tasktracker.model.Priority priority =
                        priorityBox.getValue();

                    if (editing) {

                        taskService.updateTask(
                                existingTask.getId(),
                                subject,
                                body
                        );

                        existingTask.setPriority(
                                priority
                        );

                    } else {

                        Task task =
                                taskService.addTask(
                                        subject,
                                        body
                                );

                        task.setPriority(
                                priority
                        );
                    }

                    updateDashboard();

                    return null;
                }
        );

        dialog.showAndWait();
    }

    private void refreshTasks(
            java.util.List<Task> tasks
    ) {

        visibleTasks.setAll(tasks);
    }

    private void showError(
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(
                "Task Tracker"
        );

        alert.setHeaderText(
                "Invalid Task"
        );

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }

    private void updateDashboard() {

        int total =
                taskService
                        .getAllTasks()
                        .size();

        int pending =
                taskService
                        .getPendingTasks()
                        .size();

        int completed =
                taskService
                        .getCompletedTasks()
                        .size();

        totalLabel.setText(
                String.valueOf(total)
        );

        pendingLabel.setText(
                String.valueOf(pending)
        );

        completedLabel.setText(
                String.valueOf(completed)
        );

        showAllTasks();
    }

    private class TaskCell
            extends ListCell<Task> {

        @Override
        protected void updateItem(
                Task task,
                boolean empty
        ) {

            super.updateItem(
                    task,
                    empty
            );

            if (empty || task == null) {

                setGraphic(null);
                setText(null);

                return;
            }

            Label subject =
                    new Label(
                            task.getSubject()
                    );

            subject.setStyle(
                    "-fx-font-size: 16px;" +
                    "-fx-font-weight: bold;"
            );

            Label body =
                    new Label(
                            task.getBody() == null
                                    ? ""
                                    : task.getBody()
                    );

            body.setWrapText(true);

            body.setMaxWidth(
                    Double.MAX_VALUE
            );

            Label priority =
                    new Label(
                            "Priority: "
                                    + task.getPriority()
                    );

            Label status =
                    new Label(
                            task.getStatus()
                                    == TaskStatus.COMPLETED
                                    ? "Status: Completed"
                                    : "Status: Pending"
                    );

            Label flag =
                    new Label(
                            task.isFlagged()
                                    ? "★ Flagged"
                                    : ""
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

            Button editButton =
                    new Button("Edit");

            Button deleteButton =
                    new Button("Delete");

            completeButton.setOnAction(
                    event -> {

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

                        updateDashboard();
                    }
            );

            flagButton.setOnAction(
                    event -> {

                        taskService.toggleFlag(
                                task.getId()
                        );

                        updateDashboard();
                    }
            );

            editButton.setOnAction(
                    event ->
                            showEditTaskDialog(task)
            );

            deleteButton.setOnAction(
                    event ->
                            deleteTask(task)
            );

            HBox buttons =
                    new HBox(
                            8,
                            completeButton,
                            flagButton,
                            editButton,
                            deleteButton
                    );

            VBox information =
                    new VBox(
                            6,
                            subject,
                            body,
                            priority,
                            status,
                            flag,
                            buttons
                    );

            information.setPadding(
                    new Insets(12)
            );

            information.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-border-color: #dddddd;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;"
            );

            setGraphic(
                    information
            );
        }
    }

    private void deleteTask(
            Task task
    ) {

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Delete Task"
        );

        confirmation.setHeaderText(
                "Delete this task?"
        );

        confirmation.setContentText(
                task.getSubject()
        );

        ButtonType result =
                confirmation.showAndWait()
                        .orElse(
                                ButtonType.CANCEL
                        );

        if (result ==
                ButtonType.OK) {

            taskService.deleteTask(
                    task.getId()
            );

            updateDashboard();
        }
    }

    public static void main(
            String[] args
    ) {

        launch(args);
    }
}