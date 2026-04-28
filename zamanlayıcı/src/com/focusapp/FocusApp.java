package com.focusapp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class FocusApp extends Application {

    private enum SessionType {
        WORK, SHORT_BREAK, LONG_BREAK
    }

    public static class TaskItem {
        private final String text;
        private final BooleanProperty completed = new SimpleBooleanProperty(false);

        public TaskItem(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public BooleanProperty completedProperty() {
            return completed;
        }

        public boolean isCompleted() {
            return completed.get();
        }

        public void setCompleted(boolean completed) {
            this.completed.set(completed);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private int workMinutes = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes = 15;
    private int dailyTarget = 6;

    private int remainingSeconds = workMinutes * 60;
    private boolean isRunning = false;
    private SessionType currentSessionType = SessionType.WORK;

    private int completedWorkSessions = 0;
    private int completedBreakSessions = 0;
    private int completedTasks = 0;
    private int totalWorkedSeconds = 0;

    private Timeline timeline;

    private Stage primaryStage;
    private Scene scene;

    private Label appTitleLabel;
    private Label subtitleLabel;
    private Label sessionLabel;
    private Label timerLabel;
    private Label miniStatusLabel;

    private Label pomodoroCountLabel;
    private Label workedTodayLabel;
    private Label completedTasksLabel;
    private Label targetLabel;

    private Spinner<Integer> workSpinner;
    private Spinner<Integer> shortBreakSpinner;
    private Spinner<Integer> longBreakSpinner;
    private Spinner<Integer> targetSpinner;

    private Button startPauseButton;
    private Button resetButton;
    private Button skipButton;
    private Button addTaskButton;
    private Button removeTaskButton;

    private ToggleButton alwaysOnTopToggle;
    private ToggleButton themeToggle;
    private ToggleButton compactModeToggle;

    private CheckBox autoStartCheckBox;

    private ProgressBar targetProgressBar;

    private TextField taskField;
    private ListView<TaskItem> taskListView;
    private ObservableList<TaskItem> tasks;

    private VBox leftPanel;
    private VBox rightPanel;
    private VBox statsCard;
    private VBox chartCard;
    private VBox taskCard;
    private GridPane settingsGrid;
    private HBox rootLayout;

    private BarChart<String, Number> statsChart;
    private XYChart.Series<String, Number> statsSeries;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        buildUI();
        setupTimeline();
        setupActions();
        setupTaskCellFactory();

        refreshStats();
        updateChart();
        applyDarkTheme();

        stage.setTitle("Focus");
        stage.setScene(scene);
        stage.setMinWidth(1080);
        stage.setMinHeight(760);
        stage.show();
    }

    private void buildUI() {
        appTitleLabel = new Label("Focus");
        appTitleLabel.getStyleClass().add("app-title");

        subtitleLabel = new Label("Minimalist Pomodoro + Görev Takibi");
        subtitleLabel.getStyleClass().add("subtitle-label");

        sessionLabel = new Label("Çalışma Zamanı");
        sessionLabel.getStyleClass().add("session-label");

        timerLabel = new Label(formatTime(remainingSeconds));
        timerLabel.getStyleClass().add("timer-label");

        miniStatusLabel = new Label("Odaklan ve başla");
        miniStatusLabel.getStyleClass().add("mini-status-label");

        pomodoroCountLabel = new Label();
        pomodoroCountLabel.getStyleClass().add("stat-line");

        workedTodayLabel = new Label();
        workedTodayLabel.getStyleClass().add("stat-line");

        completedTasksLabel = new Label();
        completedTasksLabel.getStyleClass().add("stat-line");

        targetLabel = new Label();
        targetLabel.getStyleClass().add("stat-line");

        workSpinner = createSpinner(1, 180, 25);
        shortBreakSpinner = createSpinner(1, 60, 5);
        longBreakSpinner = createSpinner(5, 120, 15);
        targetSpinner = createSpinner(1, 20, 6);

        settingsGrid = new GridPane();
        settingsGrid.setHgap(14);
        settingsGrid.setVgap(14);
        settingsGrid.getStyleClass().add("settings-grid");

        addSettingRow(0, "Çalışma", workSpinner);
        addSettingRow(1, "Kısa mola", shortBreakSpinner);
        addSettingRow(2, "Uzun mola", longBreakSpinner);
        addSettingRow(3, "Günlük hedef", targetSpinner);

        startPauseButton = new Button("Başlat");
        startPauseButton.getStyleClass().addAll("action-button", "primary-button");

        resetButton = new Button("Sıfırla");
        resetButton.getStyleClass().addAll("action-button", "secondary-button");

        skipButton = new Button("Geç");
        skipButton.getStyleClass().addAll("action-button", "secondary-button");

        alwaysOnTopToggle = new ToggleButton("Widget");
        alwaysOnTopToggle.getStyleClass().addAll("action-button", "secondary-button");

        themeToggle = new ToggleButton("Light");
        themeToggle.getStyleClass().addAll("action-button", "secondary-button");

        compactModeToggle = new ToggleButton("Kompakt");
        compactModeToggle.getStyleClass().addAll("action-button", "secondary-button");

        autoStartCheckBox = new CheckBox("Oturumlar arası otomatik başlat");
        autoStartCheckBox.setSelected(true);
        autoStartCheckBox.getStyleClass().add("small-check");

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        VBox headerTexts = new VBox(4, appTitleLabel, subtitleLabel);
        headerBox.getChildren().add(headerTexts);

        VBox timerHeroCard = new VBox(
                14,
                sessionLabel,
                timerLabel,
                miniStatusLabel
        );
        timerHeroCard.setAlignment(Pos.CENTER);
        timerHeroCard.getStyleClass().addAll("glass-card", "hero-card");
        timerHeroCard.setPadding(new Insets(28));

        HBox mainButtonBox = new HBox(12, startPauseButton, resetButton, skipButton);
        mainButtonBox.setAlignment(Pos.CENTER);

        HBox toolButtonBox = new HBox(12, alwaysOnTopToggle, compactModeToggle, themeToggle);
        toolButtonBox.setAlignment(Pos.CENTER);

        VBox controlsCard = new VBox(
                18,
                settingsGrid,
                autoStartCheckBox,
                mainButtonBox,
                toolButtonBox
        );
        controlsCard.getStyleClass().add("glass-card");
        controlsCard.setPadding(new Insets(22));

        targetProgressBar = new ProgressBar(0);
        targetProgressBar.setPrefWidth(340);
        targetProgressBar.getStyleClass().add("target-progress");

        statsCard = new VBox(
                12,
                createSectionTitle("Günlük İstatistik"),
                pomodoroCountLabel,
                workedTodayLabel,
                completedTasksLabel,
                targetLabel,
                targetProgressBar
        );
        statsCard.getStyleClass().add("glass-card");
        statsCard.setPadding(new Insets(22));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Kategoriler");
        yAxis.setLabel("Değer");

        statsChart = new BarChart<>(xAxis, yAxis);
        statsChart.setLegendVisible(false);
        statsChart.setAnimated(false);
        statsChart.setCategoryGap(18);
        statsChart.setBarGap(6);
        statsChart.setPrefHeight(250);

        statsSeries = new XYChart.Series<>();
        statsChart.getData().add(statsSeries);

        chartCard = new VBox(14, createSectionTitle("Bugünün Analizi"), statsChart);
        chartCard.getStyleClass().add("glass-card");
        chartCard.setPadding(new Insets(22));

        taskField = new TextField();
        taskField.setPromptText("Yeni görev ekle...");

        addTaskButton = new Button("Ekle");
        addTaskButton.getStyleClass().addAll("action-button", "secondary-button");

        removeTaskButton = new Button("Sil");
        removeTaskButton.getStyleClass().addAll("action-button", "secondary-button");

        tasks = FXCollections.observableArrayList();
        taskListView = new ListView<>(tasks);
        taskListView.setPrefHeight(340);
        taskListView.getStyleClass().add("task-list");

        HBox taskInputBox = new HBox(10, taskField, addTaskButton);
        taskInputBox.setAlignment(Pos.CENTER_LEFT);

        HBox taskActionBox = new HBox(removeTaskButton);
        taskActionBox.setAlignment(Pos.CENTER_RIGHT);

        taskCard = new VBox(
                14,
                createSectionTitle("Görevler"),
                taskInputBox,
                taskListView,
                taskActionBox
        );
        taskCard.getStyleClass().add("glass-card");
        taskCard.setPadding(new Insets(22));

        leftPanel = new VBox(18, headerBox, timerHeroCard, controlsCard);
        leftPanel.setPrefWidth(430);

        rightPanel = new VBox(18, statsCard, taskCard, chartCard);
        rightPanel.setPrefWidth(580);
        VBox.setVgrow(taskCard, Priority.ALWAYS);

        rootLayout = new HBox(18, leftPanel, rightPanel);
        rootLayout.setPadding(new Insets(22));
        rootLayout.getStyleClass().add("root-pane");

        scene = new Scene(rootLayout, 1180, 820);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
    }

    private void addSettingRow(int row, String text, Spinner<Integer> spinner) {
        Label label = new Label(text);
        label.getStyleClass().add("small-label");
        settingsGrid.add(label, 0, row);
        settingsGrid.add(spinner, 1, row);
    }

    private Spinner<Integer> createSpinner(int min, int max, int initial) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initial);
        spinner.setEditable(true);
        spinner.setPrefWidth(130);
        return spinner;
    }

    private Label createSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                if (currentSessionType == SessionType.WORK) {
                    totalWorkedSeconds++;
                }
                timerLabel.setText(formatTime(remainingSeconds));
                refreshStats();
            } else {
                handleSessionComplete();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void setupActions() {
        startPauseButton.setOnAction(e -> {
            if (!isRunning) {
                applySettingsFromSpinners();
                timeline.play();
                isRunning = true;
                startPauseButton.setText("Duraklat");
                miniStatusLabel.setText("Zaman akıyor...");
                setSettingsDisabled(true);
            } else {
                timeline.pause();
                isRunning = false;
                startPauseButton.setText("Başlat");
                miniStatusLabel.setText("Duraklatıldı");
            }
        });

        resetButton.setOnAction(e -> resetTimer());

        skipButton.setOnAction(e -> {
            remainingSeconds = 0;
            timerLabel.setText(formatTime(remainingSeconds));
            handleSessionComplete();
        });

        addTaskButton.setOnAction(e -> addTask());
        taskField.setOnAction(e -> addTask());

        removeTaskButton.setOnAction(e -> {
            TaskItem selected = taskListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.isCompleted()) {
                    completedTasks = Math.max(0, completedTasks - 1);
                }
                tasks.remove(selected);
                refreshStats();
                updateChart();
            }
        });

        alwaysOnTopToggle.setOnAction(e -> {
            boolean selected = alwaysOnTopToggle.isSelected();
            primaryStage.setAlwaysOnTop(selected);
            alwaysOnTopToggle.setText(selected ? "Widget Açık" : "Widget");
        });

        compactModeToggle.setOnAction(e -> applyCompactMode(compactModeToggle.isSelected()));

        themeToggle.setOnAction(e -> {
            if (themeToggle.isSelected()) {
                applyLightTheme();
                themeToggle.setText("Dark");
            } else {
                applyDarkTheme();
                themeToggle.setText("Light");
            }
        });

        targetSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            dailyTarget = newVal;
            refreshStats();
            updateChart();
        });
    }

    private void setupTaskCellFactory() {
        taskListView.setCellFactory(listView -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final HBox container = new HBox(checkBox);

            {
                container.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(TaskItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                checkBox.setText(item.getText());
                checkBox.setSelected(item.isCompleted());
                updateTaskStyle(item);

                checkBox.setOnAction(e -> {
                    boolean old = item.isCompleted();
                    item.setCompleted(checkBox.isSelected());

                    if (!old && item.isCompleted()) {
                        completedTasks++;
                    } else if (old && !item.isCompleted()) {
                        completedTasks = Math.max(0, completedTasks - 1);
                    }

                    updateTaskStyle(item);
                    refreshStats();
                    updateChart();
                });

                setGraphic(container);
            }

            private void updateTaskStyle(TaskItem item) {
                if (item.isCompleted()) {
                    checkBox.setStyle("-fx-text-fill: #94a3b8; -fx-strikethrough: true;");
                } else {
                    checkBox.setStyle("-fx-text-fill: white; -fx-strikethrough: false;");
                }
            }
        });
    }

    private void applySettingsFromSpinners() {
        workMinutes = workSpinner.getValue();
        shortBreakMinutes = shortBreakSpinner.getValue();
        longBreakMinutes = longBreakSpinner.getValue();
        dailyTarget = targetSpinner.getValue();

        if (!isRunning && currentSessionType == SessionType.WORK) {
            remainingSeconds = workMinutes * 60;
            timerLabel.setText(formatTime(remainingSeconds));
        }
    }

    private void handleSessionComplete() {
        timeline.stop();
        isRunning = false;

        if (currentSessionType == SessionType.WORK) {
            completedWorkSessions++;
            NotificationUtil.playBeep();
            NotificationUtil.showNotification("Focus", "Çalışma süresi bitti.");

            if (completedWorkSessions % 4 == 0) {
                currentSessionType = SessionType.LONG_BREAK;
                remainingSeconds = longBreakSpinner.getValue() * 60;
                sessionLabel.setText("Uzun Mola");
                miniStatusLabel.setText("Harika, şimdi biraz uzun dinlen");
            } else {
                currentSessionType = SessionType.SHORT_BREAK;
                remainingSeconds = shortBreakSpinner.getValue() * 60;
                sessionLabel.setText("Kısa Mola");
                miniStatusLabel.setText("Kısa bir mola zamanı");
            }
        } else {
            completedBreakSessions++;
            NotificationUtil.playBeep();
            NotificationUtil.showNotification("Focus", "Mola bitti. Tekrar çalışma zamanı.");

            currentSessionType = SessionType.WORK;
            remainingSeconds = workSpinner.getValue() * 60;
            sessionLabel.setText("Çalışma Zamanı");
            miniStatusLabel.setText("Yeni odak turu başladı");
        }

        timerLabel.setText(formatTime(remainingSeconds));
        refreshStats();
        updateChart();

        if (autoStartCheckBox.isSelected()) {
            timeline.play();
            isRunning = true;
            startPauseButton.setText("Duraklat");
            setSettingsDisabled(true);
        } else {
            startPauseButton.setText("Başlat");
            setSettingsDisabled(false);
        }
    }

    private void resetTimer() {
        timeline.stop();
        isRunning = false;
        currentSessionType = SessionType.WORK;

        applySettingsFromSpinners();
        remainingSeconds = workSpinner.getValue() * 60;

        sessionLabel.setText("Çalışma Zamanı");
        timerLabel.setText(formatTime(remainingSeconds));
        miniStatusLabel.setText("Sıfırlandı, tekrar başlayabilirsin");
        startPauseButton.setText("Başlat");
        setSettingsDisabled(false);
    }

    private void setSettingsDisabled(boolean disabled) {
        workSpinner.setDisable(disabled);
        shortBreakSpinner.setDisable(disabled);
        longBreakSpinner.setDisable(disabled);
        targetSpinner.setDisable(disabled);
    }

    private void addTask() {
        String text = taskField.getText().trim();
        if (!text.isEmpty()) {
            tasks.add(new TaskItem(text));
            taskField.clear();
        }
    }

    private void refreshStats() {
        pomodoroCountLabel.setText("Tamamlanan Pomodoro: " + completedWorkSessions);
        workedTodayLabel.setText("Bugün Çalışılan Süre: " + formatWorkedTime(totalWorkedSeconds));
        completedTasksLabel.setText("Tamamlanan Görev: " + completedTasks);
        targetLabel.setText("Günlük Hedef: " + completedWorkSessions + " / " + dailyTarget);

        double progress = dailyTarget == 0 ? 0 : Math.min(1.0, (double) completedWorkSessions / dailyTarget);
        targetProgressBar.setProgress(progress);
    }

    private void updateChart() {
        statsSeries.getData().clear();
        statsSeries.getData().add(new XYChart.Data<>("Pomodoro", completedWorkSessions));
        statsSeries.getData().add(new XYChart.Data<>("Görev", completedTasks));
        statsSeries.getData().add(new XYChart.Data<>("Mola", completedBreakSessions));
        statsSeries.getData().add(new XYChart.Data<>("Saat", totalWorkedSeconds / 3600.0));
    }

    private void applyCompactMode(boolean compact) {
        if (compact) {
            primaryStage.setWidth(430);
            primaryStage.setHeight(620);
            rightPanel.setVisible(false);
            rightPanel.setManaged(false);
            subtitleLabel.setText("Kompakt odak görünümü");
            compactModeToggle.setText("Normal");
        } else {
            primaryStage.setWidth(1180);
            primaryStage.setHeight(820);
            rightPanel.setVisible(true);
            rightPanel.setManaged(true);
            subtitleLabel.setText("Minimalist Pomodoro + Görev Takibi");
            compactModeToggle.setText("Kompakt");
        }
    }

    private void applyDarkTheme() {
        rootLayout.getStyleClass().remove("light-root");
        if (!rootLayout.getStyleClass().contains("root-pane")) {
            rootLayout.getStyleClass().add("root-pane");
        }
    }

    private void applyLightTheme() {
        rootLayout.getStyleClass().add("light-root");
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String formatWorkedTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;

        if (hours > 0) {
            return hours + " saat " + minutes + " dk";
        }
        return minutes + " dk";
    }

    public static void main(String[] args) {
        launch(args);
    }
}