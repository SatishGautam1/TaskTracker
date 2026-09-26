package com.tasktracker.ui;

import com.tasktracker.model.Task;
import com.tasktracker.model.TaskStatus;
import com.tasktracker.util.DateTimeUtil;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;

/**

* A task's list row.
*
* <p>The node tree is created once in the constructor and reused when the
* ListView recycles this cell. The updateItem method only updates the
* displayed task data and styles.</p>
  */
public class TaskCard extends ListCell<Task> {

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

  public TaskCard(
      Consumer<Task> onComplete,
      Consumer<Task> onFlag,
      Consumer<Task> onEdit,
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
    deleteButton.getStyleClass().addAll(
        "secondary-button",
        "danger-button"
    );

    /*
     * Complete / reopen
     */
   completeButton.setOnAction(event -> {
       Task task = getItem();

       if (task != null) {
           onComplete.accept(task);
       }
   });

   /*
    * Flag / unflag
    */
   flagButton.setOnAction(event -> {
       Task task = getItem();

       if (task != null) {
           onFlag.accept(task);
       }
   });

   /*
    * Edit
    */
   editButton.setOnAction(event -> {
       Task task = getItem();

       if (task != null) {
           onEdit.accept(task);
       }
   });

   /*
    * Build the card layout BEFORE attaching any handlers that
    * reference the card.
    */
   HBox chipsRow = new HBox(
           8,
           priorityChip,
           statusChip,
           flagIndicator
   );

   chipsRow.setAlignment(Pos.CENTER_LEFT);

   HBox metaRow = new HBox(
           14,
           dueLabel,
           pendingAgeLabel
   );

   metaRow.setAlignment(Pos.CENTER_LEFT);

   HBox buttonsRow = new HBox(
           8,
           completeButton,
           flagButton,
           editButton,
           deleteButton
   );

   buttonsRow.setAlignment(Pos.CENTER_LEFT);

   Region spacer = new Region();

   HBox.setHgrow(
           spacer,
           javafx.scene.layout.Priority.ALWAYS
   );

   HBox headerRow = new HBox(
           10,
           subjectLabel,
           spacer,
           chipsRow
   );

   headerRow.setAlignment(Pos.CENTER_LEFT);

   card = new VBox(
           6,
           headerRow,
           bodyPreviewLabel,
           tagsBox,
           metaRow,
           buttonsRow
   );

   card.setPadding(new Insets(14));
   card.getStyleClass().add("task-card");
   card.setMaxWidth(Double.MAX_VALUE);

   /*
    * Delete animation.
    *
    * This handler is intentionally created AFTER card has been
    * initialized. This fixes the Java compiler error:
    *
    * "variable card might not have been initialized"
    */
   deleteButton.setOnAction(event -> {

       Task task = getItem();

       if (task == null) {
           return;
       }

       FadeTransition fade = new FadeTransition(
               Duration.millis(160),
               card
       );

       fade.setFromValue(1.0);
       fade.setToValue(0.0);

       ScaleTransition scale = new ScaleTransition(
               Duration.millis(160),
               card
       );

       scale.setFromX(1.0);
       scale.setFromY(1.0);
       scale.setToX(0.96);
       scale.setToY(0.96);

       fade.setOnFinished(fadeEvent -> {

           /*
            * Restore the card state before the ListView reuses
            * this cell for another task.
            */
           card.setOpacity(1.0);
           card.setScaleX(1.0);
           card.setScaleY(1.0);

           onDelete.accept(task);
       });

       fade.play();
       scale.play();
   });

   /*
    * Allow the card to stretch across the ListView.
    */
   setMaxWidth(Double.MAX_VALUE);

   /*
    * Keep the card width synchronized with the ListView.
    */
   listViewProperty().addListener(
           (observable, oldListView, newListView) -> {

               if (newListView != null) {

                   prefWidthProperty().bind(
                           newListView.widthProperty()
                                   .subtract(24)
                   );

               } else {

                   prefWidthProperty().unbind();
               }
           }
   );
  }

  @Override
  protected void updateItem(Task task, boolean empty) {

   super.updateItem(task, empty);

   if (empty || task == null) {

       setGraphic(null);
       setText(null);

       /*
        * Reset visual state because ListView cells are reused.
        */
       card.setOpacity(1.0);
       card.setScaleX(1.0);
       card.setScaleY(1.0);

       return;
   }

   boolean completed =
           task.getStatus() == TaskStatus.COMPLETED;

   /*
    * Subject
    */
   subjectLabel.setText(
           task.getSubject() == null
                   ? ""
                   : task.getSubject()
   );

   /*
    * Body preview
    */
   String body = task.getBody();

   String preview =
           body == null || body.isBlank()
                   ? ""
                   : firstLine(body);

   bodyPreviewLabel.setText(preview);

   boolean hasPreview = !preview.isEmpty();

   bodyPreviewLabel.setManaged(hasPreview);
   bodyPreviewLabel.setVisible(hasPreview);

   /*
    * Priority
    */
   priorityChip.setText(
           task.getPriority().toString()
   );

   priorityChip.getStyleClass().removeIf(
           style -> style.startsWith("priority-")
   );

   priorityChip.getStyleClass().add(
           "priority-"
                   + task.getPriority()
                   .toString()
                   .toLowerCase()
   );

   /*
    * Status
    */
   statusChip.setText(
           completed
                   ? "Completed"
                   : "Pending"
   );

   statusChip.getStyleClass().removeIf(
           style ->
                   style.equals("status-completed")
                           || style.equals("status-pending")
   );

   statusChip.getStyleClass().add(
           completed
                   ? "status-completed"
                   : "status-pending"
   );

   /*
    * Flag
    */
   boolean flagged = task.isFlagged();

   flagIndicator.setVisible(flagged);
   flagIndicator.setManaged(flagged);

   /*
    * Due date
    */
   String dueText =
           DateTimeUtil.formatDate(
                   task.getDueDate()
           );

   boolean hasDueDate = dueText != null;

   dueLabel.setText(
           hasDueDate
                   ? "Due " + dueText
                   : ""
   );

   dueLabel.setManaged(hasDueDate);
   dueLabel.setVisible(hasDueDate);

   /*
    * Pending age
    */
   if (completed) {

       pendingAgeLabel.setText("");
       pendingAgeLabel.setVisible(false);
       pendingAgeLabel.setManaged(false);

   } else {

       pendingAgeLabel.setText(
               "Pending for "
                       + DateTimeUtil.pendingAge(
                               task.getPendingSince()
                       )
       );

       pendingAgeLabel.setVisible(true);
       pendingAgeLabel.setManaged(true);
   }

   /*
    * Tags
    */
   tagsBox.getChildren().clear();

   for (String tag : task.getTags()) {

       if (tag == null || tag.isBlank()) {
           continue;
       }

       Label tagChip =
               new Label(tag.trim());

       tagChip.getStyleClass().add(
               "tag-chip"
       );

       tagsBox.getChildren().add(
               tagChip
       );
   }

   boolean hasTags =
           !tagsBox.getChildren().isEmpty();

   tagsBox.setManaged(hasTags);
   tagsBox.setVisible(hasTags);

   /*
    * Action buttons
    */
   completeButton.setText(
           completed
                   ? "Reopen"
                   : "Complete"
   );

   flagButton.setText(
           flagged
                   ? "Unflag"
                   : "Flag"
   );

   /*
    * Card state styles
    */
   card.getStyleClass().removeIf(
           style ->
                   style.equals("task-card-completed")
                           || style.equals("task-card-overdue")
                           || style.equals("task-card-due-today")
   );

   if (completed) {

       card.getStyleClass().add(
               "task-card-completed"
       );

   } else if (task.isOverdue()) {

       card.getStyleClass().add(
               "task-card-overdue"
       );

   } else if (task.isDueToday()) {

       card.getStyleClass().add(
               "task-card-due-today"
       );
   }

   /*
    * Make sure a recycled cell always starts from a clean
    * animation state.
    */
   card.setOpacity(1.0);
   card.setScaleX(1.0);
   card.setScaleY(1.0);

   setGraphic(card);
   setText(null);

  }

  private String firstLine(String text) {

   int newlineIndex =
           text.indexOf('\n');

   String line =
           newlineIndex >= 0
                   ? text.substring(
                           0,
                           newlineIndex
                   )
                   : text;

   return line.length() > 160
           ? line.substring(0, 160) + "…"
           : line;

  }
  }
