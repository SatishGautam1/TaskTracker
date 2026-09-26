package com.tasktracker.ui;

import com.tasktracker.service.FilterType;
import com.tasktracker.service.TaskService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarPanel extends VBox {

    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final Map<FilterType, Button> filterButtons = new LinkedHashMap<>();
    private final Map<FilterType, Label> filterCounts = new LinkedHashMap<>();

    public SidebarPanel(Consumer<FilterType> onFilterSelected) {

        setSpacing(12);
        setPadding(new Insets(20));
        setPrefWidth(240);
        setMinWidth(180);
        getStyleClass().add("sidebar");

        addFilterButton("All Tasks", FilterType.ALL, onFilterSelected);
        addFilterButton("Pending", FilterType.PENDING, onFilterSelected);
        addFilterButton("Completed", FilterType.COMPLETED, onFilterSelected);
        addFilterButton("Flagged", FilterType.FLAGGED, onFilterSelected);
        addFilterButton("Overdue", FilterType.OVERDUE, onFilterSelected);
        addFilterButton("Due Today", FilterType.DUE_TODAY, onFilterSelected);

        getChildren().addAll(
                dashboardPanel,
                new Separator()
        );

        getChildren().addAll(filterButtons.values());

        selectFilter(FilterType.ALL);
    }

    private void addFilterButton(String label, FilterType filter, Consumer<FilterType> onFilterSelected) {

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("nav-button-label");

        Label countLabel = new Label("0");
        countLabel.getStyleClass().add("nav-button-count");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox content = new HBox(6, nameLabel, spacer, countLabel);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);

        Button button = new Button();
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");

        button.setOnAction(event -> {
            selectFilter(filter);
            onFilterSelected.accept(filter);
        });

        filterButtons.put(filter, button);
        filterCounts.put(filter, countLabel);
    }

    public void selectFilter(FilterType filter) {

        filterButtons.values().forEach(button ->
                button.getStyleClass().removeAll("nav-button-selected")
        );

        Button selected = filterButtons.get(filter);

        if (selected != null) {
            selected.getStyleClass().add("nav-button-selected");
        }
    }

    /**
     * Refreshes the small count badge next to each filter. Cheap: each
     * count is just the size of the already-computed filtered list, so this
     * does not duplicate TaskService's filtering logic.
     */
    public void updateCounts(TaskService taskService) {

        filterCounts.get(FilterType.ALL).setText(String.valueOf(taskService.getAllTasks().size()));
        filterCounts.get(FilterType.PENDING).setText(String.valueOf(taskService.getPendingTasks().size()));
        filterCounts.get(FilterType.COMPLETED).setText(String.valueOf(taskService.getCompletedTasks().size()));
        filterCounts.get(FilterType.FLAGGED).setText(String.valueOf(taskService.getFlaggedTasks().size()));
        filterCounts.get(FilterType.OVERDUE).setText(String.valueOf(taskService.getOverdueTasks().size()));
        filterCounts.get(FilterType.DUE_TODAY).setText(String.valueOf(taskService.getDueTodayTasks().size()));
    }

    public DashboardPanel getDashboardPanel() {
        return dashboardPanel;
    }
}
