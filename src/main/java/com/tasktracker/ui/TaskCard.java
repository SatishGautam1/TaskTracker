package com.tasktracker.ui;

import com.tasktracker.model.Task;
import com.tasktracker.model.TaskStatus;
import com.tasktracker.util.DateTimeUtil;
import com.tasktracker.util.MarkdownPreview;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A task's list row.
 * <p>
 * The node tree is built once, in the constructor, and only text/visibility/
 * style are updated on cell reuse - {@code ListView} calls {@code updateItem}
 * very frequently (scroll, resize, any list mutation), so rebuilding the
 * whole row every time is both wasteful and what makes cell-reuse bugs easy
 * to introduce.
 */
public class TaskCard extends ListCell<Task> {

    private static final double HOVER_LIFT = -3;

    private final Label subjectLabel = new Label();
    private final Label bodyPreviewLabel = new Label();
    private final Label priorityChip = new Label();
    private final Label statusChip = new Label();
    private final Label dueLabel = new Label();
    private final Label flagIndicator = new Label("★");
    private final Label pendingAgeLabel = new Label();
    private final FlowPane tagsBox = new FlowPane(6, 4);

    private final Button completeButton = new Button();
    private final Button flagButton = new Button();
    private final Button editButton = new Button("Edit");
    private final Button deleteButton = new Button("Delete");

    private final VBox card;

    // Tracks which task this cell is currently rendering so the entrance
    // animation only plays when the cell is handed a genuinely different
    // task, not on every incidental updateItem call for the same one.
    private long lastRenderedTaskId = Long.MIN_VALUE;
    private boolean hasRenderedBefore = false;

    // Kept so an in-flight animation can be stopped before a new one starts
    // on the same node - otherwise a cell recycled mid-animation can end up
    // with two transitions fighting over opacity/scale.
    private FadeTransition entranceAnimation;
    private FadeTransition deleteFade;
    private ScaleTransition deleteScale;
    private ScaleTransition popAnimation;

    public TaskCard(
            Consumer<Task> onComplete,
            Consumer<Task> onFlag,
            Consumer<Task> onEdit,
            Predicate<Task> confirmDelete,
            Consumer<Task> onDelete
    ) {

        subjectLabel.getStyleClass().add("task-subject");
        bodyPreviewLabel.getStyleClass().add("task-body-preview");
        bodyPreviewLabel.setWrapText(true);
        bodyPreviewLabel.setMaxWidth(Double.MAX_VALUE);

        priorityChip.getStyleClass().add("chip");
        statusChip.getStyleClass().add("chip");
        dueLabel.getStyleClass().add("task-meta");
        pendingAgeLabel.getStyleClass().add("task-meta");
        flagIndicator.getStyleClass().add("flag-indicator");

        completeButton.getStyleClass().add("secondary-button");
        flagButton.getStyleClass().add("secondary-button");
        editButton.getStyleClass().add("secondary-button");
        deleteButton.getStyleClass().addAll("secondary-button", "danger-button");
        deleteButton.setTooltip(new Tooltip("Delete this task"));

        completeButton.setOnAction(e -> {

            Task item = getItem();

            if (item == null) {
                return;
            }

            playCompletionPulse();
            onComplete.accept(item);
        });

        flagButton.setOnAction(e -> {

            Task item = getItem();

            if (item != null) {
                onFlag.accept(item);
            }
        });

        editButton.setOnAction(e -> {

            Task item = getItem();

            if (item != null) {
                onEdit.accept(item);
            }
        });

        // Deletion flow: confirm FIRST (a plain, synchronous dialog call from
        // a button handler), THEN play the fade/scale-down animation, THEN -
        // once the animation has actually finished - remove the task.
        //
        // The previous version played the animation first and only asked for
        // confirmation inside the animation's onFinished handler. That meant
        // a blocking modal dialog was nested inside a Transition callback,
        // which is a known JavaFX footgun: the dialog's own nested event loop
        // runs while the FadeTransition/ScaleTransition machinery is still
        // unwinding on the same call stack, and the ObservableList mutation
        // that followed could land on a pulse where the ListView's virtual
        // flow hadn't caught up - resulting in a task that was actually
        // deleted from TaskService but stayed visible until something else
        // (a resize or scroll) forced a fresh layout pass. Asking first
        // removes the nested dialog entirely, and deferring the actual
        // removal to Platform.runLater guarantees it happens on a clean
        // pulse, decoupled from the animation's own completion handling.
        deleteButton.setOnAction(e -> {

            Task item = getItem();

            if (item == null) {
                return;
            }

            if (!confirmDelete.test(item)) {
                return;
            }

            playDeleteAnimation(() -> Platform.runLater(() -> onDelete.accept(item)));
        });

        HBox chipsRow = new HBox(8, priorityChip, statusChip, flagIndicator);
        chipsRow.setAlignment(Pos.CENTER_LEFT);

        HBox metaRow = new HBox(14, dueLabel, pendingAgeLabel);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        HBox buttonsRow = new HBox(8, completeButton, flagButton, editButton, deleteButton);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox headerRow = new HBox(10, subjectLabel, spacer, chipsRow);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        card = new VBox(6, headerRow, bodyPreviewLabel, tagsBox, metaRow, buttonsRow);
        card.setPadding(new Insets(14));
        card.getStyleClass().add("task-card");
        card.setMaxWidth(Double.MAX_VALUE);

        card.setOnMouseEntered(e -> playHover(true));
        card.setOnMouseExited(e -> playHover(false));

        setMaxWidth(Double.MAX_VALUE);

        // Let the card stretch to the width of the ListView instead of
        // shrinking to its content (the default for a ListCell's graphic).
        listViewProperty().addListener((observable, oldListView, newListView) -> {

            if (newListView != null) {
                prefWidthProperty().bind(newListView.widthProperty().subtract(24));
            } else {
                prefWidthProperty().unbind();
            }
        });
    }

    @Override
    protected void updateItem(Task task, boolean empty) {

        super.updateItem(task, empty);

        if (empty || task == null) {
            stopAllAnimations();
            setGraphic(null);
            setText(null);
            hasRenderedBefore = false;
            return;
        }

        boolean isDifferentTask = !hasRenderedBefore || task.getId() != lastRenderedTaskId;

        boolean completed = task.getStatus() == TaskStatus.COMPLETED;

        subjectLabel.setText(task.getSubject());

        bodyPreviewLabel.setText(MarkdownPreview.toPreviewText(task.getBody()));
        bodyPreviewLabel.setManaged(!bodyPreviewLabel.getText().isEmpty());
        bodyPreviewLabel.setVisible(!bodyPreviewLabel.getText().isEmpty());

        priorityChip.setText(task.getPriority().toString());
        priorityChip.getStyleClass().removeIf(c -> c.startsWith("priority-"));
        priorityChip.getStyleClass().add("priority-" + task.getPriority().toString().toLowerCase());

        statusChip.setText(completed ? "Completed" : "Pending");
        statusChip.getStyleClass().removeIf(c -> c.equals("status-completed") || c.equals("status-pending"));
        statusChip.getStyleClass().add(completed ? "status-completed" : "status-pending");

        flagIndicator.setVisible(task.isFlagged());
        flagIndicator.setManaged(task.isFlagged());

        String dueText = DateTimeUtil.formatDate(task.getDueDate());
        dueLabel.setText(dueText == null ? "" : "Due " + dueText);
        dueLabel.setManaged(dueText != null);
        dueLabel.setVisible(dueText != null);

        if (completed) {
            pendingAgeLabel.setVisible(false);
            pendingAgeLabel.setManaged(false);
        } else {
            pendingAgeLabel.setText("Pending for " + DateTimeUtil.pendingAge(task.getPendingSince()));
            pendingAgeLabel.setVisible(true);
            pendingAgeLabel.setManaged(true);
        }

        tagsBox.getChildren().clear();

        for (String tag : task.getTags()) {

            if (tag == null || tag.isBlank()) {
                continue;
            }

            Label chip = new Label(tag.trim());
            chip.getStyleClass().add("tag-chip");
            tagsBox.getChildren().add(chip);
        }

        tagsBox.setManaged(!tagsBox.getChildren().isEmpty());
        tagsBox.setVisible(!tagsBox.getChildren().isEmpty());

        completeButton.setText(completed ? "Reopen" : "Complete");
        flagButton.setText(task.isFlagged() ? "Unflag" : "Flag");

        card.getStyleClass().removeIf(c ->
                c.equals("task-card-completed")
                        || c.equals("task-card-overdue")
                        || c.equals("task-card-due-today")
        );

        if (completed) {
            card.getStyleClass().add("task-card-completed");
        } else if (task.isOverdue()) {
            card.getStyleClass().add("task-card-overdue");
        } else if (task.isDueToday()) {
            card.getStyleClass().add("task-card-due-today");
        }

        setGraphic(card);

        lastRenderedTaskId = task.getId();
        hasRenderedBefore = true;

        if (isDifferentTask) {
            // This cell is being repurposed for an unrelated task (recycled
            // by the ListView), so any leftover delete/pop animation state
            // from whatever it was previously showing must not carry over.
            // A same-task update (e.g. just toggling flagged) deliberately
            // skips this, so a completion "pop" that's mid-flight is not
            // cut short by the refresh() it itself triggered.
            if (deleteFade != null) {
                deleteFade.stop();
            }
            if (deleteScale != null) {
                deleteScale.stop();
            }
            if (popAnimation != null) {
                popAnimation.stop();
            }
            card.setScaleX(1);
            card.setScaleY(1);
            card.setTranslateY(0);
            card.getStyleClass().remove("task-card-hover");

            playEntrance();
        }
    }

    private void playEntrance() {

        if (entranceAnimation != null) {
            entranceAnimation.stop();
        }

        card.setOpacity(0);

        entranceAnimation = new FadeTransition(Duration.millis(160), card);
        entranceAnimation.setFromValue(0);
        entranceAnimation.setToValue(1);
        entranceAnimation.play();
    }

    private void playHover(boolean hovering) {

        // A short, subtle lift - skipped entirely while a delete animation
        // is in flight, so the two never fight over the same node.
        if (deleteFade != null && deleteFade.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return;
        }

        TranslateTransition lift = new TranslateTransition(Duration.millis(120), card);
        lift.setToY(hovering ? HOVER_LIFT : 0);
        lift.play();

        if (hovering) {
            card.getStyleClass().add("task-card-hover");
        } else {
            card.getStyleClass().remove("task-card-hover");
        }
    }

    private void playCompletionPulse() {

        if (popAnimation != null) {
            popAnimation.stop();
        }

        card.setScaleX(1);
        card.setScaleY(1);

        popAnimation = new ScaleTransition(Duration.millis(110), card);
        popAnimation.setToX(1.02);
        popAnimation.setToY(1.02);
        popAnimation.setAutoReverse(true);
        popAnimation.setCycleCount(2);
        popAnimation.play();
    }

    private void playDeleteAnimation(Runnable onFinished) {

        stopAllAnimations();

        deleteFade = new FadeTransition(Duration.millis(160), card);
        deleteFade.setFromValue(card.getOpacity());
        deleteFade.setToValue(0);

        deleteScale = new ScaleTransition(Duration.millis(160), card);
        deleteScale.setToX(0.96);
        deleteScale.setToY(0.96);

        // onFinished only triggers the removal callback - it does not itself
        // touch TaskService or the ObservableList, so there is no dialog and
        // no application-state mutation running on the Transition's own
        // callback frame.
        deleteFade.setOnFinished(event -> onFinished.run());

        deleteFade.play();
        deleteScale.play();
    }

    private void stopAllAnimations() {

        if (entranceAnimation != null) {
            entranceAnimation.stop();
        }

        if (deleteFade != null) {
            deleteFade.stop();
        }

        if (deleteScale != null) {
            deleteScale.stop();
        }

        if (popAnimation != null) {
            popAnimation.stop();
        }

        card.setOpacity(1);
        card.setScaleX(1);
        card.setScaleY(1);
        card.setTranslateY(0);
    }
}
