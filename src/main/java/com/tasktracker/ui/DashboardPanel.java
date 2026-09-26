package com.tasktracker.ui;

import com.tasktracker.service.TaskService;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Read-only summary of the current task set. Kept deliberately small: total,
 * pending, completed, flagged and overdue counts plus a completion bar -
 * enough to orient the user without turning the sidebar into a report.
 */
public class DashboardPanel extends VBox {

    private final Label totalValue = new Label("0");
    private final Label pendingValue = new Label("0");
    private final Label completedValue = new Label("0");
    private final Label flaggedValue = new Label("0");
    private final Label overdueValue = new Label("0");

    private final ProgressBar completionBar = new ProgressBar(0);
    private final Label completionLabel = new Label("0% complete");

    public DashboardPanel() {

        setSpacing(10);
        getStyleClass().add("dashboard-panel");

        Label heading = new Label("DASHBOARD");
        heading.getStyleClass().add("panel-heading");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(statCard("TOTAL", totalValue), 0, 0);
        grid.add(statCard("PENDING", pendingValue), 1, 0);
        grid.add(statCard("COMPLETED", completedValue), 0, 1);
        grid.add(statCard("FLAGGED", flaggedValue), 1, 1);
        grid.add(statCard("OVERDUE", overdueValue), 0, 2, 2, 1);

        completionBar.setMaxWidth(Double.MAX_VALUE);
        completionBar.getStyleClass().add("completion-bar");

        VBox completionBox = new VBox(4, completionLabel, completionBar);
        completionBox.setPadding(new Insets(2, 0, 0, 0));

        getChildren().addAll(heading, grid, completionBox);
    }

    private VBox statCard(String title, Label valueLabel) {

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        valueLabel.getStyleClass().add("stat-value");

        VBox box = new VBox(2, titleLabel, valueLabel);
        box.getStyleClass().add("stat-card");
        box.setPadding(new Insets(10));

        return box;
    }

    public void update(TaskService taskService) {

        int total = taskService.getAllTasks().size();
        int pending = taskService.getPendingTasks().size();
        int completed = taskService.getCompletedTasks().size();
        int flagged = taskService.getFlaggedTasks().size();
        int overdue = taskService.getOverdueTasks().size();

        setValue(totalValue, total);
        setValue(pendingValue, pending);
        setValue(completedValue, completed);
        setValue(flaggedValue, flagged);
        setValue(overdueValue, overdue);

        double ratio = total == 0 ? 0 : (double) completed / total;

        completionBar.setProgress(ratio);
        completionLabel.setText(Math.round(ratio * 100) + "% complete");
    }

    /**
     * Updates a stat label's text and, only when the number actually
     * changed, plays a small pulse so the dashboard visibly acknowledges the
     * change instead of just silently swapping digits.
     */
    private void setValue(Label label, int newValue) {

        String newText = String.valueOf(newValue);

        if (newText.equals(label.getText())) {
            return;
        }

        label.setText(newText);

        ScaleTransition pulse = new ScaleTransition(Duration.millis(110), label);
        pulse.setToX(1.18);
        pulse.setToY(1.18);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        pulse.play();
    }
}
