package app.systeminfo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

public class ModernSystemInfoDashboard extends Application {

    private final long appStartTime = System.currentTimeMillis();
    private final DecimalFormat df = new DecimalFormat("0.0");

    private boolean darkMode = true;
    private VBox root;

    private Label osLabel;
    private Label javaLabel;
    private Label userLabel;
    private Label nowLabel;
    private Label uptimeLabel;

    private Arc cpuArc;
    private Label cpuPercentLabel;
    private Label cpuDetailLabel;
    private Label cpuStateLabel;

    private Arc ramArc;
    private Label ramPercentLabel;
    private Label ramDetailLabel;
    private Label ramStateLabel;

    private Arc diskArc;
    private Label diskPercentLabel;
    private Label diskDetailLabel;
    private Label diskStateLabel;

    private VBox systemCard;
    private StackPane cpuCard;
    private StackPane ramCard;
    private StackPane diskCard;
    private VBox footerLeft;
    private VBox footerRight;

    private VBox chartCard;
    private VBox disksCard;
    private VBox diskListBox;

    private LineChart<String, Number> usageChart;
    private XYChart.Series<String, Number> cpuSeries;
    private XYChart.Series<String, Number> ramSeries;
    private XYChart.Series<String, Number> diskSeries;

    private final Deque<Double> cpuHistory = new ArrayDeque<>();
    private final Deque<Double> ramHistory = new ArrayDeque<>();
    private final Deque<Double> diskHistory = new ArrayDeque<>();
    private final int maxHistory = 10;

    @Override
    public void start(Stage stage) {
        root = new VBox(22);
        root.setPadding(new Insets(28));

        VBox header = buildHeader();
        HBox topRow = buildTopRow();
        HBox middleRow = buildMiddleRow();
        HBox bottomRow = buildBottomRow();

        root.getChildren().addAll(header, topRow, middleRow, bottomRow);

        applyTheme();

        Scene scene = new Scene(root, 1520, 920);
        stage.setTitle("Modern System Info Dashboard");
        stage.setScene(scene);
        stage.show();

        updateAll();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateAll())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private VBox buildHeader() {
        Label badge = new Label("UTILITY DASHBOARD");
        badge.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        badge.setStyle(
                "-fx-background-radius: 999;" +
                "-fx-padding: 7 14 7 14;"
        );

        Label title = new Label("Yerel Sistem Bilgi Paneli");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 31));

        Label subtitle = new Label("CPU, RAM, disk kullanımı, işletim sistemi bilgileri, çoklu disk özeti ve canlı geçmiş grafiği.");
        subtitle.setFont(Font.font("Arial", 15));

        Label iconLine = new Label("◉  Anlık İzleme   •   ◈  Kritik Durum Uyarısı   •   ◎  Canlı Grafik");
        iconLine.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 13));

        Button refreshBtn = new Button("Yenile");
        stylePrimaryButton(refreshBtn);
        refreshBtn.setOnAction(e -> updateAll());

        Button themeBtn = new Button("Light / Dark");
        styleSecondaryButton(themeBtn);
        themeBtn.setOnAction(e -> {
            darkMode = !darkMode;
            applyTheme();
        });

        HBox actions = new HBox(10, refreshBtn, themeBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox left = new VBox(10, badge, title, subtitle, iconLine);
        HBox headerTop = new HBox(left, actions);
        HBox.setHgrow(left, Priority.ALWAYS);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(headerTop);
        header.setUserData(new Label[]{badge, title, subtitle, iconLine});
        return header;
    }

    private HBox buildTopRow() {
        systemCard = createInfoCard();
        cpuCard = createGaugeCard("CPU Kullanımı");
        ramCard = createGaugeCard("RAM Kullanımı");
        diskCard = createGaugeCard("Ana Disk");

        HBox row = new HBox(22, systemCard, cpuCard, ramCard, diskCard);
        row.setAlignment(Pos.CENTER);
        HBox.setHgrow(systemCard, Priority.ALWAYS);
        return row;
    }

    private HBox buildMiddleRow() {
        chartCard = createChartCard();
        disksCard = createDisksCard();

        HBox row = new HBox(22, chartCard, disksCard);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        HBox.setHgrow(disksCard, Priority.ALWAYS);
        return row;
    }

    private HBox buildBottomRow() {
        footerLeft = createMiniCard(
                "Durum",
                "Bu panel her 1 saniyede bir kendini yeniler.\nKritik seviyelerde renk otomatik değişir.\nYenile butonu ile manuel güncelleme yapabilirsin."
        );

        footerRight = createMiniCard(
                "Teknolojiler",
                "JavaFX • Timeline • Runtime • File • OperatingSystemMXBean • Theme Toggle • LineChart"
        );

        HBox row = new HBox(22, footerLeft, footerRight);
        HBox.setHgrow(footerLeft, Priority.ALWAYS);
        HBox.setHgrow(footerRight, Priority.ALWAYS);
        return row;
    }

    private VBox createInfoCard() {
        Label title = new Label("Sistem Özeti");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        osLabel = createInfoLabel();
        javaLabel = createInfoLabel();
        userLabel = createInfoLabel();
        nowLabel = createInfoLabel();
        uptimeLabel = createInfoLabel();

        VBox box = new VBox(18, title, osLabel, javaLabel, userLabel, nowLabel, uptimeLabel);
        box.setPrefWidth(360);
        box.setMinHeight(360);
        box.setUserData(title);
        return box;
    }

    private StackPane createGaugeCard(String titleText) {
        Label title = new Label(titleText);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        Circle ringBg = new Circle(98);
        ringBg.setFill(Color.TRANSPARENT);
        ringBg.setStrokeWidth(18);

        Arc progressArc = new Arc(0, 0, 98, 98, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(null);
        progressArc.setStrokeWidth(18);
        progressArc.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        Label percentLabel = new Label("0%");
        percentLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        percentLabel.setAlignment(Pos.CENTER);

        Label detailLabel = new Label("-");
        detailLabel.setFont(Font.font("Consolas", 11.5));
        detailLabel.setWrapText(true);
        detailLabel.setAlignment(Pos.CENTER);
        detailLabel.setMaxWidth(150);

        Label stateLabel = new Label("Normal");
        stateLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        stateLabel.setAlignment(Pos.CENTER);
        stateLabel.setMinWidth(104);
        stateLabel.setMaxWidth(104);

        VBox centerContent = new VBox(10, percentLabel, detailLabel, stateLabel);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setMaxWidth(170);

        StackPane gauge = new StackPane(ringBg, progressArc, centerContent);
        gauge.setAlignment(Pos.CENTER);
        gauge.setPrefSize(280, 280);

        VBox wrapper = new VBox(18, title, gauge);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPrefWidth(270);
        wrapper.setMinHeight(360);
        wrapper.setUserData(new Object[]{title, ringBg, percentLabel, detailLabel, stateLabel});

        if (titleText.contains("CPU")) {
            cpuArc = progressArc;
            cpuPercentLabel = percentLabel;
            cpuDetailLabel = detailLabel;
            cpuStateLabel = stateLabel;
        } else if (titleText.contains("RAM")) {
            ramArc = progressArc;
            ramPercentLabel = percentLabel;
            ramDetailLabel = detailLabel;
            ramStateLabel = stateLabel;
        } else {
            diskArc = progressArc;
            diskPercentLabel = percentLabel;
            diskDetailLabel = detailLabel;
            diskStateLabel = stateLabel;
        }

        return new StackPane(wrapper);
    }

    private VBox createChartCard() {
        Label title = new Label("Canlı Kullanım Grafiği");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 20);
        yAxis.setAutoRanging(false);

        usageChart = new LineChart<>(xAxis, yAxis);
        usageChart.setLegendVisible(true);
        usageChart.setAnimated(false);
        usageChart.setCreateSymbols(false);
        usageChart.setMinHeight(260);
        usageChart.setTitle(null);

        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU");

        ramSeries = new XYChart.Series<>();
        ramSeries.setName("RAM");

        diskSeries = new XYChart.Series<>();
        diskSeries.setName("Disk");

        usageChart.getData().addAll(cpuSeries, ramSeries, diskSeries);

        VBox box = new VBox(16, title, usageChart);
        box.setMinHeight(340);
        box.setUserData(title);
        VBox.setVgrow(usageChart, Priority.ALWAYS);
        return box;
    }

    private VBox createDisksCard() {
        Label title = new Label("Disk Sürücüleri");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        diskListBox = new VBox(12);
        VBox.setVgrow(diskListBox, Priority.ALWAYS);

        VBox box = new VBox(16, title, diskListBox);
        box.setMinHeight(340);
        box.setUserData(title);
        return box;
    }

    private VBox createMiniCard(String titleText, String contentText) {
        Label title = new Label(titleText);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label content = new Label(contentText);
        content.setFont(Font.font("Arial", 14));
        content.setWrapText(true);

        VBox box = new VBox(14, title, content);
        box.setMinHeight(150);
        box.setUserData(new Label[]{title, content});
        return box;
    }

    private void updateAll() {
        osLabel.setText("İşletim Sistemi: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        javaLabel.setText("Java Sürümü: " + System.getProperty("java.version"));
        userLabel.setText("Kullanıcı: " + System.getProperty("user.name"));
        nowLabel.setText("Tarih / Saat: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        uptimeLabel.setText("Çalışma Süresi: " + formatUptime(System.currentTimeMillis() - appStartTime));

        double cpu = updateCpu();
        double ram = updateRam();
        double disk = updateMainDisk();

        updateHistory(cpuHistory, cpu);
        updateHistory(ramHistory, ram);
        updateHistory(diskHistory, disk);
        refreshChart();
        refreshDiskList();
    }

    private double updateCpu() {
        double percent = getSystemCpuLoadPercent();

        cpuPercentLabel.setText(df.format(percent) + "%");
        cpuDetailLabel.setText(
                "İşlemci çekirdekleri: " + Runtime.getRuntime().availableProcessors() +
                "\nAnlık yük: " + df.format(percent) + "%"
        );
        cpuArc.setLength(-percent * 3.6);
        applyGaugeStyle(percent, cpuArc, cpuStateLabel);
        return percent;
    }

    private double updateRam() {
        Runtime runtime = Runtime.getRuntime();

        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;

        double percent = used * 100.0 / total;

        ramPercentLabel.setText(df.format(percent) + "%");
        ramDetailLabel.setText("Kullanılan: " + formatBytes(used) + "\nToplam: " + formatBytes(total));
        ramArc.setLength(-percent * 3.6);
        applyGaugeStyle(percent, ramArc, ramStateLabel);
        return percent;
    }

    private double updateMainDisk() {
        File rootDisk = File.listRoots()[0];

        long total = rootDisk.getTotalSpace();
        long free = rootDisk.getFreeSpace();
        long used = total - free;

        double percent = total > 0 ? used * 100.0 / total : 0;

        diskPercentLabel.setText(df.format(percent) + "%");
        diskDetailLabel.setText("Kullanılan: " + formatBytes(used) + "\nToplam: " + formatBytes(total));
        diskArc.setLength(-percent * 3.6);
        applyGaugeStyle(percent, diskArc, diskStateLabel);
        return percent;
    }

    private void refreshDiskList() {
        diskListBox.getChildren().clear();

        File[] roots = File.listRoots();
        for (File rootDisk : roots) {
            long total = rootDisk.getTotalSpace();
            long free = rootDisk.getFreeSpace();
            long used = total - free;
            double percent = total > 0 ? used * 100.0 / total : 0;

            Label name = new Label("Sürücü: " + rootDisk.getAbsolutePath());
            name.setFont(Font.font("Arial", FontWeight.BOLD, 14));

            Label info = new Label(
                    "Kullanılan: " + formatBytes(used) +
                    "    |    Boş: " + formatBytes(free) +
                    "    |    Toplam: " + formatBytes(total) +
                    "    |    Doluluk: " + df.format(percent) + "%"
            );
            info.setFont(Font.font("Arial", 13));
            info.setWrapText(true);

            Region fill = new Region();
            fill.setPrefHeight(12);
            fill.setMinHeight(12);
            fill.setMaxHeight(12);
            fill.setPrefWidth(Math.max(0, percent * 3.6));

            StackPane bar = new StackPane();
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setPrefHeight(12);
            bar.setMinHeight(12);
            bar.setStyle(
                    "-fx-background-color: rgba(148,163,184,0.20);" +
                    "-fx-background-radius: 999;"
            );

            fill.setStyle(
                    "-fx-background-color: " + getUsageHex(percent) + ";" +
                    "-fx-background-radius: 999;"
            );

            StackPane fillWrap = new StackPane(fill);
            fillWrap.setAlignment(Pos.CENTER_LEFT);

            bar.getChildren().add(fillWrap);

            VBox item = new VBox(8, name, info, bar);
            item.setPadding(new Insets(12));
            item.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.05);" +
                    "-fx-background-radius: 18;" +
                    "-fx-border-color: rgba(255,255,255,0.07);" +
                    "-fx-border-radius: 18;"
            );

            if (!darkMode) {
                item.setStyle(
                        "-fx-background-color: rgba(255,255,255,0.78);" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: rgba(148,163,184,0.18);" +
                        "-fx-border-radius: 18;"
                );
                name.setTextFill(Color.web("#0f172a"));
                info.setTextFill(Color.web("#334155"));
            } else {
                name.setTextFill(Color.WHITE);
                info.setTextFill(Color.web("#d6deeb"));
            }

            diskListBox.getChildren().add(item);
        }
    }

    private void updateHistory(Deque<Double> history, double value) {
        if (history.size() >= maxHistory) {
            history.removeFirst();
        }
        history.addLast(value);
    }

    private void refreshChart() {
        cpuSeries.getData().clear();
        ramSeries.getData().clear();
        diskSeries.getData().clear();

        int i = 1;
        for (Double value : cpuHistory) {
            cpuSeries.getData().add(new XYChart.Data<>("T" + i, value));
            i++;
        }

        i = 1;
        for (Double value : ramHistory) {
            ramSeries.getData().add(new XYChart.Data<>("T" + i, value));
            i++;
        }

        i = 1;
        for (Double value : diskHistory) {
            diskSeries.getData().add(new XYChart.Data<>("T" + i, value));
            i++;
        }
    }

    private double getSystemCpuLoadPercent() {
        try {
            com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

            double load = osBean.getCpuLoad();

            if (load < 0) {
                return 0;
            }
            return load * 100.0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void applyGaugeStyle(double percent, Arc arc, Label stateLabel) {
        if (percent < 60) {
            arc.setStroke(Color.web("#22c55e"));
            stateLabel.setText("Normal");
            stateLabel.setStyle(
                    "-fx-background-color: rgba(34,197,94,0.18);" +
                    "-fx-text-fill: #86efac;" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 8 14 8 14;" +
                    "-fx-alignment: center;"
            );
        } else if (percent < 80) {
            arc.setStroke(Color.web("#f59e0b"));
            stateLabel.setText("Dikkat");
            stateLabel.setStyle(
                    "-fx-background-color: rgba(245,158,11,0.18);" +
                    "-fx-text-fill: #fde68a;" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 8 14 8 14;" +
                    "-fx-alignment: center;"
            );
        } else {
            arc.setStroke(Color.web("#ef4444"));
            stateLabel.setText("Kritik");
            stateLabel.setStyle(
                    "-fx-background-color: rgba(239,68,68,0.18);" +
                    "-fx-text-fill: #fca5a5;" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 8 14 8 14;" +
                    "-fx-alignment: center;"
            );
        }
    }

    private Label createInfoLabel() {
        return new Label("-");
    }

    private void applyTheme() {
        if (darkMode) {
            root.setBackground(new Background(new BackgroundFill(
                    new LinearGradient(
                            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#0b1220")),
                            new Stop(1, Color.web("#17263f"))
                    ),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));
        } else {
            root.setBackground(new Background(new BackgroundFill(
                    new LinearGradient(
                            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#eef4ff")),
                            new Stop(1, Color.web("#dbeafe"))
                    ),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));
        }

        styleMainCard(systemCard);
        styleGaugeCard(cpuCard);
        styleGaugeCard(ramCard);
        styleGaugeCard(diskCard);
        styleSimpleCard(chartCard);
        styleSimpleCard(disksCard);
        styleMiniCard(footerLeft);
        styleMiniCard(footerRight);
        styleChart();

        VBox header = (VBox) root.getChildren().get(0);
        Label[] headerLabels = (Label[]) header.getUserData();

        Label badge = headerLabels[0];
        Label title = headerLabels[1];
        Label subtitle = headerLabels[2];
        Label iconLine = headerLabels[3];

        if (darkMode) {
            badge.setTextFill(Color.web("#93c5fd"));
            badge.setStyle(
                    "-fx-background-color: rgba(59,130,246,0.15);" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 7 14 7 14;"
            );
            title.setTextFill(Color.WHITE);
            subtitle.setTextFill(Color.web("#d6deeb"));
            iconLine.setTextFill(Color.web("#a5b4fc"));
        } else {
            badge.setTextFill(Color.web("#1d4ed8"));
            badge.setStyle(
                    "-fx-background-color: rgba(37,99,235,0.12);" +
                    "-fx-background-radius: 999;" +
                    "-fx-padding: 7 14 7 14;"
            );
            title.setTextFill(Color.web("#0f172a"));
            subtitle.setTextFill(Color.web("#334155"));
            iconLine.setTextFill(Color.web("#4338ca"));
        }
    }

    private void styleMainCard(VBox box) {
        Label title = (Label) box.getUserData();
        styleCard(box);

        if (darkMode) {
            title.setTextFill(Color.WHITE);
            setInfoLabelColors(Color.web("#eef2ff"));
        } else {
            title.setTextFill(Color.web("#0f172a"));
            setInfoLabelColors(Color.web("#1e293b"));
        }
    }

    private void setInfoLabelColors(Color color) {
        osLabel.setTextFill(color);
        javaLabel.setTextFill(color);
        userLabel.setTextFill(color);
        nowLabel.setTextFill(color);
        uptimeLabel.setTextFill(color);

        osLabel.setFont(Font.font("Arial", 15));
        javaLabel.setFont(Font.font("Arial", 15));
        userLabel.setFont(Font.font("Arial", 15));
        nowLabel.setFont(Font.font("Arial", 15));
        uptimeLabel.setFont(Font.font("Arial", 15));
    }

    private void styleGaugeCard(StackPane stackPane) {
        VBox wrapper = (VBox) stackPane.getChildren().get(0);
        styleCard(wrapper);

        Object[] data = (Object[]) wrapper.getUserData();
        Label title = (Label) data[0];
        Circle ringBg = (Circle) data[1];
        Label percent = (Label) data[2];
        Label detail = (Label) data[3];

        if (darkMode) {
            title.setTextFill(Color.WHITE);
            ringBg.setStroke(Color.web("#334155"));
            percent.setTextFill(Color.WHITE);
            detail.setTextFill(Color.web("#d6deeb"));
        } else {
            title.setTextFill(Color.web("#0f172a"));
            ringBg.setStroke(Color.web("#bfdbfe"));
            percent.setTextFill(Color.web("#0f172a"));
            detail.setTextFill(Color.web("#334155"));
        }
    }

    private void styleSimpleCard(VBox box) {
        Label title = (Label) box.getUserData();
        styleCard(box);

        if (darkMode) {
            title.setTextFill(Color.WHITE);
        } else {
            title.setTextFill(Color.web("#0f172a"));
        }
    }

    private void styleMiniCard(VBox box) {
        styleCard(box);
        Label[] labels = (Label[]) box.getUserData();

        if (darkMode) {
            labels[0].setTextFill(Color.WHITE);
            labels[1].setTextFill(Color.web("#d6deeb"));
        } else {
            labels[0].setTextFill(Color.web("#0f172a"));
            labels[1].setTextFill(Color.web("#334155"));
        }
    }

    private void styleCard(Region region) {
        if (darkMode) {
            region.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.07);" +
                    "-fx-background-radius: 26;" +
                    "-fx-border-color: rgba(255,255,255,0.09);" +
                    "-fx-border-radius: 26;" +
                    "-fx-padding: 24;"
            );
        } else {
            region.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.72);" +
                    "-fx-background-radius: 26;" +
                    "-fx-border-color: rgba(148,163,184,0.22);" +
                    "-fx-border-radius: 26;" +
                    "-fx-padding: 24;"
            );
        }
    }

    private void styleChart() {
        if (darkMode) {
            usageChart.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: white;"
            );
        } else {
            usageChart.setStyle(
                    "-fx-background-color: transparent;" +
                    "-fx-text-fill: #0f172a;"
            );
        }
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: linear-gradient(to right, #2563eb, #4f46e5);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 14;" +
                "-fx-padding: 10 18 10 18;"
        );
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: #334155;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 14;" +
                "-fx-padding: 10 18 10 18;"
        );
    }

    private String formatUptime(long millis) {
        long totalSeconds = millis / 1000;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private String formatBytes(long bytes) {
        double gb = bytes / (1024.0 * 1024 * 1024);
        return df.format(gb) + " GB";
    }

    private String getUsageHex(double percent) {
        if (percent < 60) return "#22c55e";
        if (percent < 80) return "#f59e0b";
        return "#ef4444";
    }

    public static void main(String[] args) {
        launch(args);
    }
}