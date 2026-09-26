package com.tasktracker.ui;

import com.tasktracker.model.Priority;
import com.tasktracker.model.Task;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The add/edit task dialog. Due date and reminder are captured as plain
 * dates (defaulting to 9:00 AM) rather than a full date-time picker, since
 * JavaFX has no built-in date-time control and a custom one would add a lot
 * of complexity for little practical benefit here.
 */
public class TaskEditorDialog {

    private static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);

    private final Dialog<TaskFormData> dialog = new Dialog<>();

    private final TextField subjectField = new TextField();
    private final MarkdownEditor markdownEditor = new MarkdownEditor();
    private final ComboBox<Priority> priorityBox =
            new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
    private final CheckBox flaggedCheckBox = new CheckBox("Flagged");
    private final DatePicker dueDatePicker = new DatePicker();
    private final DatePicker reminderDatePicker = new DatePicker();
    private final TextField tagsField = new TextField();

    public TaskEditorDialog(Task existingTask) {

        boolean editing = existingTask != null;

        dialog.setTitle(editing ? "Edit Task" : "Add Task");
        dialog.setHeaderText(editing ? "Edit task details" : "Create a new task");
        dialog.getDialogPane().getStyleClass().add("task-editor-dialog");

        ButtonType saveButtonType = new ButtonType(
                editing ? "Save Changes" : "Create Task",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        subjectField.setPromptText("Task subject");
        priorityBox.setValue(Priority.MEDIUM);
        tagsField.setPromptText("comma, separated, tags");
        dueDatePicker.setPromptText("No due date");
        reminderDatePicker.setPromptText("No reminder");

        if (editing) {

            subjectField.setText(existingTask.getSubject());
            priorityBox.setValue(existingTask.getPriority());
            markdownEditor.setMarkdown(existingTask.getBody());
            flaggedCheckBox.setSelected(existingTask.isFlagged());
            tagsField.setText(String.join(", ", existingTask.getTags()));

            if (existingTask.getDueDate() != null) {
                dueDatePicker.setValue(existingTask.getDueDate().toLocalDate());
            }

            if (existingTask.getReminderAt() != null) {
                reminderDatePicker.setValue(existingTask.getReminderAt().toLocalDate());
            }
        }

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(16);
        detailsGrid.setVgap(8);

        detailsGrid.addRow(0, new Label("Priority"), priorityBox, flaggedCheckBox);
        detailsGrid.addRow(1, new Label("Due date"), dueDatePicker, new Label("Reminder"), reminderDatePicker);

        VBox content = new VBox(
                10,
                new Label("Subject"),
                subjectField,
                new Label("Description"),
                markdownEditor.getView(),
                detailsGrid,
                new Label("Tags"),
                tagsField
        );

        content.setPadding(new Insets(10));
        content.setPrefWidth(900);
        content.setPrefHeight(640);

        VBox.setVgrow(markdownEditor.getView(), javafx.scene.layout.Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);

        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {

            if (subjectField.getText().trim().isEmpty()) {

                showValidationError("Subject is required.");
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {

            if (button != saveButtonType) {
                return null;
            }

            return new TaskFormData(
                    subjectField.getText().trim(),
                    markdownEditor.getMarkdown(),
                    priorityBox.getValue(),
                    flaggedCheckBox.isSelected(),
                    toDateTime(dueDatePicker.getValue()),
                    toDateTime(reminderDatePicker.getValue()),
                    parseTags(tagsField.getText())
            );
        });
    }

    public Optional<TaskFormData> showAndWait() {
        return dialog.showAndWait();
    }

    private LocalDateTime toDateTime(LocalDate date) {

        if (date == null) {
            return null;
        }

        return LocalDateTime.of(date, DEFAULT_TIME);
    }

    private List<String> parseTags(String rawTags) {

        List<String> tags = new ArrayList<>();

        if (rawTags == null || rawTags.isBlank()) {
            return tags;
        }

        for (String tag : rawTags.split(",")) {

            String trimmed = tag.trim();

            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }

        return tags;
    }

    private void showValidationError(String message) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Task Tracker");
        alert.setHeaderText("Invalid Task");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
