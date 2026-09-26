package com.tasktracker.ui;

import com.tasktracker.service.FilterType;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarPanel extends VBox {

    private final DashboardPanel dashboardPanel = new DashboardPanel();
    private final Map<FilterType, Button> filterButtons = new LinkedHashMap<>();

    public SidebarPanel(Consumer<FilterType> onFilterSelected) {

        setSpacing(12);
        setPadding(new Insets(20));
        setPrefWidth(240);
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

        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("nav-button");

        button.setOnAction(event -> {
            selectFilter(filter);
            onFilterSelected.accept(filter);
        });

        filterButtons.put(filter, button);
    }

    public void selectFilter(FilterType filter) {

        filterButtons.forEach((type, button) ->
                button.getStyleClass().removeAll("nav-button-selected")
        );

        Button selected = filterButtons.get(filter);

        if (selected != null) {
            selected.getStyleClass().add("nav-button-selected");
        }
    }

    public DashboardPanel getDashboardPanel() {
        return dashboardPanel;
    }
}
