package app;

import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TypingTutorUltimateApp extends Application {

    private enum LanguageOption { TURKISH, ENGLISH }
    private enum Difficulty { EASY, MEDIUM, HARD }
    private enum PlayMode { WORDS, SENTENCES }
    private enum Category { GENERAL, PROGRAMMING, SCHOOL, TECHNOLOGY }

    private static class FallingItem {
        Label label;
        TranslateTransition transition;
        String text;
        long spawnMillis;
        boolean countedAsMiss;

        FallingItem(Label label, TranslateTransition transition, String text) {
            this.label = label;
            this.transition = transition;
            this.text = text;
            this.spawnMillis = System.currentTimeMillis();
            this.countedAsMiss = false;
        }
    }

    private final Random random = new Random();

    private final Path appDir = Path.of(System.getProperty("user.home"), ".typing_tutor_ultimate");
    private final Path scoresFile = appDir.resolve("scores.txt");
    private final Path profileFile = appDir.resolve("profile.properties");
    private final Path dailyFile = appDir.resolve("daily.properties");

    private final Map<String, String> tr = new HashMap<>();
    private final Map<String, String> en = new HashMap<>();

    private final List<String> generalWordsEN = Arrays.asList(
            "focus", "screen", "window", "input", "result", "logic", "light", "timer", "panel", "score",
            "future", "target", "design", "practice", "monitor", "keyboard", "control", "motion", "session", "winner"
    );

    private final List<String> programmingWordsEN = Arrays.asList(
            "java", "class", "method", "object", "compile", "debug", "thread", "module", "package", "library",
            "function", "variable", "interface", "inheritance", "framework", "exception", "collection", "stream"
    );

    private final List<String> schoolWordsEN = Arrays.asList(
            "lesson", "student", "teacher", "exam", "project", "reading", "writing", "science", "library", "pencil",
            "notebook", "history", "formula", "subject", "homework", "analysis", "grammar", "education"
    );

    private final List<String> technologyWordsEN = Arrays.asList(
            "network", "system", "device", "digital", "robot", "server", "mobile", "cloud", "storage", "display",
            "security", "database", "wireless", "browser", "computer", "software", "hardware", "internet"
    );

    private final List<String> generalWordsTR = Arrays.asList(
            "odak", "ekran", "pencere", "girdi", "sonuc", "mantik", "isik", "sure", "panel", "puan",
            "hedef", "tasarim", "pratik", "izleme", "klavye", "kontrol", "hareket", "oturum", "oyuncu", "basari"
    );

    private final List<String> programmingWordsTR = Arrays.asList(
            "java", "sinif", "metot", "nesne", "derleme", "hataayikla", "isparcagi", "modul", "paket", "kutuphane",
            "fonksiyon", "degisken", "arayuz", "kalitim", "cerceve", "istisna", "koleksiyon", "akıs"
    );

    private final List<String> schoolWordsTR = Arrays.asList(
            "ders", "ogrenci", "ogretmen", "sinav", "proje", "okuma", "yazma", "bilim", "kutuphane", "kalem",
            "defter", "tarih", "formul", "konu", "odev", "analiz", "dilbilgisi", "egitim"
    );

    private final List<String> technologyWordsTR = Arrays.asList(
            "ag", "sistem", "cihaz", "dijital", "robot", "sunucu", "mobil", "bulut", "depolama", "ekran",
            "guvenlik", "veritabani", "kablosuz", "tarayici", "bilgisayar", "yazilim", "donanim", "internet"
    );

    private final List<String> sentencesEN = Arrays.asList(
            "java makes desktop apps powerful",
            "practice improves typing speed",
            "focus on accuracy before speed",
            "clean code saves debugging time",
            "every key press matters",
            "good habits build fast typing",
            "software design needs patience",
            "small progress becomes big success",
            "typing games can improve reflexes",
            "consistency beats motivation over time"
    );

    private final List<String> sentencesTR = Arrays.asList(
            "java masaustu uygulamalarini guclendirir",
            "pratik yazma hizini gelistirir",
            "once dogruluk sonra hiz gelmelidir",
            "temiz kod hata ayiklamayi kolaylastirir",
            "her tusa basim onemlidir",
            "iyi aliskanliklar hizli yazmayi destekler",
            "yazilim tasarimi sabir ister",
            "kucuk ilerleme buyuk basariya donusur",
            "yazma oyunlari refleksi gelistirebilir",
            "duzenli calisma motivasyondan daha gucludur"
    );

    private final List<FallingItem> activeItems = new ArrayList<>();
    private final List<String> wrongTypedWords = new ArrayList<>();
    private final List<Integer> accuracyHistory = new ArrayList<>();
    private final List<Integer> wpmHistory = new ArrayList<>();
    private final List<Long> reactionTimes = new ArrayList<>();

    private BorderPane root;
    private VBox mainCard;
    private Pane gamePane;
    private StackPane gameStack;
    private VBox resultOverlay;
    private VBox pauseOverlay;
    private Label countdownLabel;
    private Label streakEffectLabel;

    private Label titleLabel;
    private Label subtitleLabel;
    private Label statusLabel;

    private Label languageCaption;
    private Label difficultyCaption;
    private Label categoryCaption;
    private Label modeCaption;
    private Label themeCaption;
    private Label soundCaption;
    private Label musicCaption;
    private Label compactCaption;
    private Label userCaption;

    private Label timeTitle;
    private Label livesTitle;
    private Label correctTitle;
    private Label wrongTitle;
    private Label comboTitle;
    private Label accuracyTitle;
    private Label wpmTitle;
    private Label bestTitle;
    private Label levelTitle;
    private Label gamesTitle;

    private Label timeValue;
    private Label livesValue;
    private Label correctValue;
    private Label wrongValue;
    private Label comboValue;
    private Label accuracyValue;
    private Label wpmValue;
    private Label bestValue;
    private Label levelValue;
    private Label gamesValue;

    private Label resultTitle;
    private Label resultSummary;
    private Label resultWpm;
    private Label resultAccuracy;
    private Label resultCorrect;
    private Label resultWrong;
    private Label resultCombo;
    private Label resultMissed;
    private Label resultLevel;
    private Label resultDifficulty;
    private Label resultLanguage;
    private Label resultMode;
    private Label resultCategory;
    private Label resultReaction;
    private Label resultDaily;
    private Label scoreboardTitle;
    private Label wrongWordsTitle;
    private Label chartsTitle;

    private TextField inputField;
    private TextField nameField;

    private ComboBox<String> languageCombo;
    private ComboBox<String> difficultyCombo;
    private ComboBox<String> categoryCombo;
    private ComboBox<String> modeCombo;
    private ComboBox<String> themeCombo;

    private CheckBox soundCheck;
    private CheckBox musicCheck;
    private CheckBox compactCheck;

    private Button startButton;
    private Button pauseButton;
    private Button resumeButton;
    private Button restartButton;
    private Button playAgainButton;

    private ProgressBar timeProgressBar;
    private TextFlow livePreviewFlow;

    private ListView<String> scoreboardList;
    private ListView<String> wrongWordsList;

    private LineChart<Number, Number> accuracyChart;
    private LineChart<Number, Number> wpmChart;
    private XYChart.Series<Number, Number> accuracySeries;
    private XYChart.Series<Number, Number> wpmSeries;

    private Timeline gameTimer;
    private Timeline spawnTimer;
    private Timeline bossWaveTimer;

    private LanguageOption currentLanguage = LanguageOption.TURKISH;
    private Difficulty currentDifficulty = Difficulty.EASY;
    private PlayMode currentMode = PlayMode.WORDS;
    private Category currentCategory = Category.GENERAL;

    private boolean gameRunning = false;
    private boolean paused = false;
    private boolean soundEnabled = true;
    private boolean musicEnabled = false;
    private boolean compactMode = false;
    private boolean bossWaveActive = false;

    private int totalGameSeconds = 60;
    private int remainingSeconds = totalGameSeconds;
    private int lives = 3;
    private int maxLives = 3;
    private int correctCount = 0;
    private int wrongCount = 0;
    private int missedCount = 0;
    private int combo = 0;
    private int bestCombo = 0;
    private int bestWpm = 0;
    private int gamesPlayed = 0;
    private int sessionTypedCharacters = 0;
    private int level = 1;

    private long sessionStartMillis = 0L;
    private LocalDate today = LocalDate.now();

    private AudioClip correctClip;
    private AudioClip wrongClip;
    private AudioClip gameOverClip;
    private MediaPlayer backgroundPlayer;

    private Stage primaryStageRef;

    @Override
    public void start(Stage stage) {
        this.primaryStageRef = stage;

        createStorage();
        initTexts();
        loadProfile();
        loadHighScore();
        loadDailyStats();
        loadSounds();

        titleLabel = new Label();
        titleLabel.getStyleClass().add("title-label");

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("subtitle-label");

        VBox headerBox = new VBox(4, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        userCaption = createSmallCaption();
        languageCaption = createSmallCaption();
        difficultyCaption = createSmallCaption();
        categoryCaption = createSmallCaption();
        modeCaption = createSmallCaption();
        themeCaption = createSmallCaption();
        soundCaption = createSmallCaption();
        musicCaption = createSmallCaption();
        compactCaption = createSmallCaption();

        nameField = new TextField();
        nameField.getStyleClass().add("mini-field");
        nameField.setText(loadUserName());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> saveUserName(newVal));

        languageCombo = new ComboBox<>(FXCollections.observableArrayList("Türkçe", "English"));
        languageCombo.setValue("Türkçe");
        languageCombo.setOnAction(e -> {
            currentLanguage = "English".equals(languageCombo.getValue()) ? LanguageOption.ENGLISH : LanguageOption.TURKISH;
            updateLanguageTexts();
            updateLiveTypingPreview();
        });

        difficultyCombo = new ComboBox<>(FXCollections.observableArrayList("Easy", "Medium", "Hard"));
        difficultyCombo.setValue("Easy");
        difficultyCombo.setOnAction(e -> currentDifficulty = parseDifficulty(difficultyCombo.getValue()));

        categoryCombo = new ComboBox<>(FXCollections.observableArrayList("General", "Programming", "School", "Technology"));
        categoryCombo.setValue("General");
        categoryCombo.setOnAction(e -> currentCategory = parseCategory(categoryCombo.getValue()));

        modeCombo = new ComboBox<>(FXCollections.observableArrayList("Words", "Sentences"));
        modeCombo.setValue("Words");
        modeCombo.setOnAction(e -> currentMode = "Sentences".equals(modeCombo.getValue()) ? PlayMode.SENTENCES : PlayMode.WORDS);

        themeCombo = new ComboBox<>(FXCollections.observableArrayList("Dark", "Light"));
        themeCombo.setValue("Light");
        themeCombo.setOnAction(e -> applyTheme());

        soundCheck = new CheckBox();
        soundCheck.setSelected(true);
        soundCheck.setOnAction(e -> soundEnabled = soundCheck.isSelected());

        musicCheck = new CheckBox();
        musicCheck.setSelected(false);
        musicCheck.setOnAction(e -> {
            musicEnabled = musicCheck.isSelected();
            updateMusicState();
        });

        compactCheck = new CheckBox();
        compactCheck.setSelected(false);
        compactCheck.setOnAction(e -> {
            compactMode = compactCheck.isSelected();
            applyCompactMode();
        });

        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(14);
        optionsGrid.setVgap(12);
        optionsGrid.setAlignment(Pos.CENTER);

        optionsGrid.add(createOptionBox(userCaption, nameField), 0, 0);
        optionsGrid.add(createOptionBox(languageCaption, languageCombo), 1, 0);
        optionsGrid.add(createOptionBox(difficultyCaption, difficultyCombo), 2, 0);
        optionsGrid.add(createOptionBox(categoryCaption, categoryCombo), 3, 0);
        optionsGrid.add(createOptionBox(modeCaption, modeCombo), 4, 0);
        optionsGrid.add(createOptionBox(themeCaption, themeCombo), 0, 1);
        optionsGrid.add(createOptionBox(soundCaption, soundCheck), 1, 1);
        optionsGrid.add(createOptionBox(musicCaption, musicCheck), 2, 1);
        optionsGrid.add(createOptionBox(compactCaption, compactCheck), 3, 1);

        timeTitle = createStatTitle();
        livesTitle = createStatTitle();
        correctTitle = createStatTitle();
        wrongTitle = createStatTitle();
        comboTitle = createStatTitle();
        accuracyTitle = createStatTitle();
        wpmTitle = createStatTitle();
        bestTitle = createStatTitle();
        levelTitle = createStatTitle();
        gamesTitle = createStatTitle();

        timeValue = createStatValue("60");
        livesValue = createStatValue("❤❤❤");
        correctValue = createStatValue("0");
        wrongValue = createStatValue("0");
        comboValue = createStatValue("0");
        accuracyValue = createStatValue("%0");
        wpmValue = createStatValue("0");
        bestValue = createStatValue("0");
        levelValue = createStatValue("1");
        gamesValue = createStatValue("0");

        FlowPane statsPane = new FlowPane();
        statsPane.setHgap(12);
        statsPane.setVgap(12);
        statsPane.setAlignment(Pos.CENTER);
        statsPane.getChildren().addAll(
                createStatCard(timeTitle, timeValue),
                createStatCard(livesTitle, livesValue),
                createStatCard(correctTitle, correctValue),
                createStatCard(wrongTitle, wrongValue),
                createStatCard(comboTitle, comboValue),
                createStatCard(accuracyTitle, accuracyValue),
                createStatCard(wpmTitle, wpmValue),
                createStatCard(bestTitle, bestValue),
                createStatCard(levelTitle, levelValue),
                createStatCard(gamesTitle, gamesValue)
        );

        timeProgressBar = new ProgressBar(1.0);
        timeProgressBar.getStyleClass().add("time-bar");
        timeProgressBar.setMaxWidth(Double.MAX_VALUE);

        livePreviewFlow = new TextFlow();
        livePreviewFlow.getStyleClass().add("preview-box");
        livePreviewFlow.setMinHeight(46);

        gamePane = new Pane();
        gamePane.setPrefSize(980, 320);
        gamePane.setMinSize(980, 320);
        gamePane.getStyleClass().add("game-pane");

        countdownLabel = new Label("");
        countdownLabel.getStyleClass().add("countdown-label");
        countdownLabel.setVisible(false);

        streakEffectLabel = new Label("");
        streakEffectLabel.getStyleClass().add("streak-label");
        streakEffectLabel.setVisible(false);

        resultTitle = new Label();
        resultTitle.getStyleClass().add("result-title");
        resultSummary = createResultLine();
        resultWpm = createResultLine();
        resultAccuracy = createResultLine();
        resultCorrect = createResultLine();
        resultWrong = createResultLine();
        resultCombo = createResultLine();
        resultMissed = createResultLine();
        resultLevel = createResultLine();
        resultDifficulty = createResultLine();
        resultLanguage = createResultLine();
        resultMode = createResultLine();
        resultCategory = createResultLine();
        resultReaction = createResultLine();
        resultDaily = createResultLine();

        scoreboardTitle = createSectionTitle();
        wrongWordsTitle = createSectionTitle();
        chartsTitle = createSectionTitle();

        scoreboardList = new ListView<>();
        scoreboardList.setPrefHeight(140);

        wrongWordsList = new ListView<>();
        wrongWordsList.setPrefHeight(140);

        accuracyChart = createChart("Accuracy");
        wpmChart = createChart("WPM");
        accuracySeries = new XYChart.Series<>();
        wpmSeries = new XYChart.Series<>();
        accuracyChart.getData().add(accuracySeries);
        wpmChart.getData().add(wpmSeries);

        playAgainButton = new Button();
        playAgainButton.getStyleClass().add("primary-button");
        playAgainButton.setOnAction(e -> {
            hideResultOverlay();
            resetGame();
            startCountdownThenGame();
        });

        VBox resultInfoBox = new VBox(6,
                resultSummary, resultWpm, resultAccuracy, resultCorrect, resultWrong,
                resultCombo, resultMissed, resultLevel, resultDifficulty,
                resultLanguage, resultMode, resultCategory, resultReaction, resultDaily
        );

        HBox bottomLists = new HBox(14,
                createTitledBox(scoreboardTitle, scoreboardList),
                createTitledBox(wrongWordsTitle, wrongWordsList)
        );
        bottomLists.setAlignment(Pos.CENTER);

        VBox chartsBox = new VBox(10, chartsTitle, accuracyChart, wpmChart);
        chartsBox.getStyleClass().add("charts-box");
        chartsBox.setAlignment(Pos.CENTER);

        resultOverlay = new VBox(16, resultTitle, resultInfoBox, bottomLists, chartsBox, playAgainButton);
        resultOverlay.setAlignment(Pos.CENTER);
        resultOverlay.setPadding(new Insets(20));
        resultOverlay.getStyleClass().add("result-overlay");
        resultOverlay.setVisible(false);
        resultOverlay.setManaged(false);

        Label pauseLabel = new Label("PAUSED");
        pauseLabel.getStyleClass().add("pause-title");

        pauseOverlay = new VBox(14, pauseLabel);
        pauseOverlay.setAlignment(Pos.CENTER);
        pauseOverlay.getStyleClass().add("pause-overlay");
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);

        gameStack = new StackPane(gamePane, streakEffectLabel, countdownLabel, pauseOverlay, resultOverlay);
        gameStack.setAlignment(Pos.CENTER);

        inputField = new TextField();
        inputField.getStyleClass().add("input-field");
        inputField.setFont(Font.font(18));
        inputField.textProperty().addListener((obs, oldVal, newVal) -> updateLiveTypingPreview());
        inputField.setOnAction(e -> checkTypedInput());

        startButton = new Button();
        startButton.getStyleClass().add("primary-button");
        startButton.setOnAction(e -> startCountdownThenGame());

        pauseButton = new Button("Pause");
        pauseButton.getStyleClass().add("secondary-button");
        pauseButton.setOnAction(e -> togglePause());

        resumeButton = new Button("Resume");
        resumeButton.getStyleClass().add("secondary-button");
        resumeButton.setOnAction(e -> {
            if (paused) {
                togglePause();
            }
        });

        restartButton = new Button();
        restartButton.getStyleClass().add("danger-button");
        restartButton.setOnAction(e -> resetGame());

        HBox buttonRow = new HBox(12, startButton, pauseButton, resumeButton, restartButton);
        buttonRow.setAlignment(Pos.CENTER);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-neutral");

        mainCard = new VBox(16, optionsGrid, statsPane, timeProgressBar, livePreviewFlow, gameStack, inputField, buttonRow, statusLabel);
        mainCard.setAlignment(Pos.TOP_CENTER);
        mainCard.setPadding(new Insets(22));
        mainCard.getStyleClass().add("main-card");

        root = new BorderPane();
        root.setTop(headerBox);
        ScrollPane scrollPane = new ScrollPane(mainCard);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        root.setCenter(scrollPane);

        BorderPane.setAlignment(headerBox, Pos.CENTER);
        BorderPane.setMargin(headerBox, new Insets(20, 0, 10, 0));
        BorderPane.setMargin(mainCard, new Insets(0, 18, 18, 18));

        Scene scene = new Scene(root, 1180, 820);    
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        this.root.getStyleClass().add("root-pane");

        applyTheme();

        updateLanguageTexts();
        updateScoreboardList();
        resetGame();

        stage.setTitle("Typing Tutor Ultimate");
        stage.setScene(scene);
        stage.show();
    }

    private void createStorage() {
        try {
            Files.createDirectories(appDir);
        } catch (IOException ignored) {
        }
    }

    private void initTexts() {
        tr.put("title", "Typing Tutor Ultimate");
        tr.put("subtitle", "Tum ozelliklerle gelismis yazma hizi oyunu");
        tr.put("user", "Kullanici");
        tr.put("language", "Dil");
        tr.put("difficulty", "Zorluk");
        tr.put("category", "Kategori");
        tr.put("mode", "Mod");
        tr.put("theme", "Tema");
        tr.put("sound", "Ses");
        tr.put("music", "Muzik");
        tr.put("compact", "Kompakt");
        tr.put("time", "Sure");
        tr.put("lives", "Nisa");
        tr.put("correct", "Dogru");
        tr.put("wrong", "Yanlis");
        tr.put("combo", "Combo");
        tr.put("accuracy", "Dogruluk");
        tr.put("wpm", "WPM");
        tr.put("best", "En Iyi");
        tr.put("level", "Seviye");
        tr.put("games", "Oyun");
        tr.put("start", "Baslat");
        tr.put("restart", "Sifirla");
        tr.put("playAgain", "Tekrar Oyna");
        tr.put("ready", "Hazir");
        tr.put("placeholder", "Kelimeyi yaz ve Enter'a bas");
        tr.put("resultTitle", "Oyun Bitti");
        tr.put("scoreboard", "Top 5 Skor");
        tr.put("wrongWords", "Zorlandigin Kelimeler");
        tr.put("charts", "Performans Grafikleri");
        tr.put("gameStarted", "Oyun basladi");
        tr.put("correctStatus", "Dogru");
        tr.put("wrongStatus", "Yanlis");
        tr.put("missedStatus", "Kelime kacti");
        tr.put("finishedStatus", "Sure bitti");
        tr.put("bossWave", "Boss wave basladi");
        tr.put("levelUp", "Seviye atladin");
        tr.put("previewIdle", "Canli kontrol burada gorunecek");
        tr.put("idlePane", "Baslamak icin Baslat'a bas");
        tr.put("daily", "Bugunku toplam calisma");
        tr.put("resume", "Devam");
        tr.put("pause", "Duraklat");
        tr.put("summary", "Ozet");
        tr.put("general", "Genel");
        tr.put("programming", "Programlama");
        tr.put("school", "Okul");
        tr.put("technology", "Teknoloji");
        tr.put("words", "Kelimeler");
        tr.put("sentences", "Cumleler");

        en.put("title", "Typing Tutor Ultimate");
        en.put("subtitle", "Advanced typing game with full feature set");
        en.put("user", "User");
        en.put("language", "Language");
        en.put("difficulty", "Difficulty");
        en.put("category", "Category");
        en.put("mode", "Mode");
        en.put("theme", "Theme");
        en.put("sound", "Sound");
        en.put("music", "Music");
        en.put("compact", "Compact");
        en.put("time", "Time");
        en.put("lives", "Lives");
        en.put("correct", "Correct");
        en.put("wrong", "Wrong");
        en.put("combo", "Combo");
        en.put("accuracy", "Accuracy");
        en.put("wpm", "WPM");
        en.put("best", "Best");
        en.put("level", "Level");
        en.put("games", "Games");
        en.put("start", "Start");
        en.put("restart", "Reset");
        en.put("playAgain", "Play Again");
        en.put("ready", "Ready");
        en.put("placeholder", "Type the target and press Enter");
        en.put("resultTitle", "Game Over");
        en.put("scoreboard", "Top 5 Scores");
        en.put("wrongWords", "Words You Missed");
        en.put("charts", "Performance Charts");
        en.put("gameStarted", "Game started");
        en.put("correctStatus", "Correct");
        en.put("wrongStatus", "Wrong");
        en.put("missedStatus", "A target was missed");
        en.put("finishedStatus", "Time is over");
        en.put("bossWave", "Boss wave started");
        en.put("levelUp", "Level up");
        en.put("previewIdle", "Live validation will appear here");
        en.put("idlePane", "Press Start to begin");
        en.put("daily", "Today's total practice");
        en.put("resume", "Resume");
        en.put("pause", "Pause");
        en.put("summary", "Summary");
        en.put("general", "General");
        en.put("programming", "Programming");
        en.put("school", "School");
        en.put("technology", "Technology");
        en.put("words", "Words");
        en.put("sentences", "Sentences");
    }

    private String t(String key) {
        return currentLanguage == LanguageOption.TURKISH ? tr.get(key) : en.get(key);
    }

    private Label createSmallCaption() {
        Label label = new Label();
        label.getStyleClass().add("small-label");
        return label;
    }

    private Label createStatTitle() {
        Label label = new Label();
        label.getStyleClass().add("stat-title");
        return label;
    }

    private Label createStatValue(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("stat-value");
        return label;
    }

    private Label createResultLine() {
        Label label = new Label();
        label.getStyleClass().add("result-line");
        return label;
    }

    private Label createSectionTitle() {
        Label label = new Label();
        label.getStyleClass().add("section-title");
        return label;
    }

    private VBox createOptionBox(Label caption, Control control) {
        VBox box = new VBox(5, caption, control);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("option-box");
        return box;
    }

    private VBox createStatCard(Label title, Label value) {
        VBox box = new VBox(7, title, value);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(116);
        box.setMinHeight(92);
        box.getStyleClass().add("stat-card");
        return box;
    }

    private VBox createTitledBox(Label title, Control content) {
        VBox box = new VBox(8, title, content);
        box.setAlignment(Pos.TOP_CENTER);
        box.getStyleClass().add("list-box");
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }

    private LineChart<Number, Number> createChart(String name) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("s");
        yAxis.setLabel(name);
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(180);
        chart.getStyleClass().add("small-chart");
        return chart;
    }

    private Difficulty parseDifficulty(String value) {
        if ("Medium".equals(value)) return Difficulty.MEDIUM;
        if ("Hard".equals(value)) return Difficulty.HARD;
        return Difficulty.EASY;
    }

    private Category parseCategory(String value) {
        return switch (value) {
            case "Programming" -> Category.PROGRAMMING;
            case "School" -> Category.SCHOOL;
            case "Technology" -> Category.TECHNOLOGY;
            default -> Category.GENERAL;
        };
    }

    private void updateLanguageTexts() {
        titleLabel.setText(t("title"));
        subtitleLabel.setText(t("subtitle"));
        userCaption.setText(t("user"));
        languageCaption.setText(t("language"));
        difficultyCaption.setText(t("difficulty"));
        categoryCaption.setText(t("category"));
        modeCaption.setText(t("mode"));
        themeCaption.setText(t("theme"));
        soundCaption.setText(t("sound"));
        musicCaption.setText(t("music"));
        compactCaption.setText(t("compact"));

        timeTitle.setText(t("time"));
        livesTitle.setText(t("lives"));
        correctTitle.setText(t("correct"));
        wrongTitle.setText(t("wrong"));
        comboTitle.setText(t("combo"));
        accuracyTitle.setText(t("accuracy"));
        wpmTitle.setText(t("wpm"));
        bestTitle.setText(t("best"));
        levelTitle.setText(t("level"));
        gamesTitle.setText(t("games"));

        startButton.setText(t("start"));
        restartButton.setText(t("restart"));
        playAgainButton.setText(t("playAgain"));
        pauseButton.setText(t("pause"));
        resumeButton.setText(t("resume"));
        inputField.setPromptText(t("placeholder"));

        resultTitle.setText(t("resultTitle"));
        scoreboardTitle.setText(t("scoreboard"));
        wrongWordsTitle.setText(t("wrongWords"));
        chartsTitle.setText(t("charts"));

        if (!gameRunning) {
            setStatus(t("ready"), "status-neutral");
        }
        updateIdlePane();
        updateResultTexts();
    }

    private void applyTheme() {
        Scene scene = primaryStageRef.getScene();
        if (scene == null) return;
        scene.getStylesheets().clear();
        String css = "Dark".equals(themeCombo.getValue()) ? "dark.css" : "light.css";
        var url = getClass().getResource(css);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    private void applyCompactMode() {
        if (primaryStageRef == null) return;
        if (compactMode) {
            primaryStageRef.setWidth(1020);
            primaryStageRef.setHeight(760);
            gamePane.setPrefHeight(240);
            gamePane.setMinHeight(240);
        } else {
            primaryStageRef.setWidth(1320);
            primaryStageRef.setHeight(980);
            gamePane.setPrefHeight(320);
            gamePane.setMinHeight(320);
        }
    }

    private void startCountdownThenGame() {
        if (gameRunning || paused) return;
        hideResultOverlay();
        clearActiveItems();
        clearCharts();
        wrongTypedWords.clear();
        updateWrongWordsList();
        countdownLabel.setVisible(true);

        Timeline countdown = new Timeline(
                new KeyFrame(Duration.seconds(0), e -> countdownLabel.setText("3")),
                new KeyFrame(Duration.seconds(1), e -> countdownLabel.setText("2")),
                new KeyFrame(Duration.seconds(2), e -> countdownLabel.setText("1")),
                new KeyFrame(Duration.seconds(3), e -> countdownLabel.setText("GO!")),
                new KeyFrame(Duration.seconds(4), e -> {
                    countdownLabel.setVisible(false);
                    startGame();
                })
        );
        countdown.play();
    }

    private void startGame() {
        if (gameRunning) return;

        hidePauseOverlay();
        hideResultOverlay();
        clearActiveItems();

        gameRunning = true;
        paused = false;
        remainingSeconds = totalGameSeconds;
        lives = maxLives;
        correctCount = 0;
        wrongCount = 0;
        missedCount = 0;
        combo = 0;
        bestCombo = 0;
        level = 1;
        sessionTypedCharacters = 0;
        accuracyHistory.clear();
        wpmHistory.clear();
        reactionTimes.clear();
        sessionStartMillis = System.currentTimeMillis();
        bossWaveActive = false;

        inputField.setDisable(false);
        inputField.clear();
        inputField.requestFocus();

        startButton.setDisable(true);
        setStatus(t("gameStarted"), "status-info");
        updateStats();
        updateLiveTypingPreview();
        updateMusicState();

        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!gameRunning || paused) return;

            remainingSeconds--;
            updateStats();
            updateCharts();

            int elapsed = totalGameSeconds - remainingSeconds;
            if (elapsed > 0 && elapsed % 20 == 0) {
                triggerBossWave();
            }

            if (remainingSeconds <= 0) {
                finishGame();
            }
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();

        spawnTimer = new Timeline(new KeyFrame(Duration.seconds(getSpawnInterval()), e -> spawnItem()));
        spawnTimer.setCycleCount(Timeline.INDEFINITE);
        spawnTimer.play();

        int initialCount = getInitialCount();
        for (int i = 0; i < initialCount; i++) {
            spawnItem();
        }

        updateIdlePane();
    }

    private void triggerBossWave() {
        bossWaveActive = true;
        setStatus(t("bossWave"), "status-info");

        if (spawnTimer != null) {
            spawnTimer.stop();
        }

        spawnTimer = new Timeline(new KeyFrame(Duration.seconds(Math.max(0.35, getSpawnInterval() * 0.65)), e -> spawnItem()));
        spawnTimer.setCycleCount(Timeline.INDEFINITE);
        spawnTimer.play();

        if (bossWaveTimer != null) {
            bossWaveTimer.stop();
        }

        bossWaveTimer = new Timeline(new KeyFrame(Duration.seconds(6), e -> {
            bossWaveActive = false;
            if (spawnTimer != null) spawnTimer.stop();
            spawnTimer = new Timeline(new KeyFrame(Duration.seconds(getSpawnInterval()), ev -> spawnItem()));
            spawnTimer.setCycleCount(Timeline.INDEFINITE);
            if (gameRunning && !paused) spawnTimer.play();
        }));
        bossWaveTimer.play();
    }

    private void spawnItem() {
        if (!gameRunning || paused) return;

        removeIdleLabel();

        String text = getRandomTarget();
        Label label = new Label(text);
        label.getStyleClass().add("falling-word");

        if (currentMode == PlayMode.SENTENCES) {
            label.getStyleClass().add("sentence-item");
        }

        double maxX = Math.max(60, gamePane.getWidth() - 260);
        double x = 20 + random.nextDouble() * maxX;

        label.setLayoutX(x);
        label.setLayoutY(12);

        TranslateTransition transition = new TranslateTransition(Duration.seconds(getFallDuration()), label);
        transition.setFromY(0);
        transition.setToY(gamePane.getHeight() - 80);

        FallingItem item = new FallingItem(label, transition, text);
        activeItems.add(item);
        gamePane.getChildren().add(label);

        transition.setOnFinished(e -> {
            if (activeItems.contains(item)) {
                activeItems.remove(item);
                gamePane.getChildren().remove(label);
                item.countedAsMiss = true;
                missedCount++;
                wrongCount++;
                lives--;
                combo = 0;
                wrongTypedWords.add(text);
                setStatus(t("missedStatus"), "status-wrong");
                playWrongSound();
                updateStats();
                updateWrongWordsList();
                checkGameOverByLives();
            }
        });

        transition.play();
        updateLiveTypingPreview();
    }

    private void checkTypedInput() {
        if (!gameRunning || paused) return;

        String typed = inputField.getText().trim();
        if (typed.isEmpty()) return;

        sessionTypedCharacters += typed.length();

        FallingItem exactMatch = findExactMatch(typed);
        if (exactMatch != null) {
            handleCorrectMatch(exactMatch);
        } else {
            handleWrongAttempt(typed);
        }

        inputField.clear();
        updateLiveTypingPreview();
        updateStats();
        updateCharts();
        levelUpIfNeeded();
    }

    private FallingItem findExactMatch(String typed) {
        return activeItems.stream()
                .filter(item -> item.text.equalsIgnoreCase(typed))
                .sorted(Comparator.comparingLong(item -> item.spawnMillis))
                .findFirst()
                .orElse(null);
    }

    private FallingItem getFocusTarget() {
        return activeItems.stream()
                .sorted(Comparator.comparingLong(item -> item.spawnMillis))
                .findFirst()
                .orElse(null);
    }

    private void handleCorrectMatch(FallingItem item) {
        item.transition.stop();
        item.label.getStyleClass().add("word-correct");

        long reaction = System.currentTimeMillis() - item.spawnMillis;
        reactionTimes.add(reaction);

        PauseTransition pause = new PauseTransition(Duration.millis(130));
        pause.setOnFinished(e -> gamePane.getChildren().remove(item.label));
        pause.play();

        activeItems.remove(item);
        correctCount++;
        combo++;
        if (combo > bestCombo) bestCombo = combo;

        setStatus(t("correctStatus"), "status-correct");
        playCorrectSound();
        maybeShowStreakEffect();
    }

    private void handleWrongAttempt(String typed) {
        wrongCount++;
        lives--;
        combo = 0;
        wrongTypedWords.add(typed);
        setStatus(t("wrongStatus"), "status-wrong");
        playWrongSound();
        updateWrongWordsList();
        checkGameOverByLives();
    }

    private void maybeShowStreakEffect() {
        String text = null;
        if (combo == 5) text = "Nice!";
        else if (combo == 10) text = "Great!";
        else if (combo == 15) text = "Amazing!";
        else if (combo == 20) text = "Unstoppable!";

        if (text != null) {
            streakEffectLabel.setText(text);
            streakEffectLabel.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(150), streakEffectLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), streakEffectLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            SequentialTransition seq = new SequentialTransition(fadeIn, fadeOut);
            seq.setOnFinished(e -> streakEffectLabel.setVisible(false));
            seq.play();
        }
    }

    private void levelUpIfNeeded() {
        int newLevel = 1 + (correctCount / 8);
        if (newLevel > level) {
            level = newLevel;
            setStatus(t("levelUp") + " -> " + level, "status-info");
            if (spawnTimer != null) {
                spawnTimer.stop();
                spawnTimer = new Timeline(new KeyFrame(Duration.seconds(getSpawnInterval()), e -> spawnItem()));
                spawnTimer.setCycleCount(Timeline.INDEFINITE);
                if (gameRunning && !paused) spawnTimer.play();
            }
        }
    }

    private void togglePause() {
        if (!gameRunning) return;

        paused = !paused;

        if (paused) {
            if (gameTimer != null) gameTimer.pause();
            if (spawnTimer != null) spawnTimer.pause();
            if (bossWaveTimer != null) bossWaveTimer.pause();

            for (FallingItem item : activeItems) {
                item.transition.pause();
            }

            inputField.setDisable(true);
            showPauseOverlay();
            pauseBackgroundMusic();
            setStatus(t("pause"), "status-info");
        } else {
            if (gameTimer != null) gameTimer.play();
            if (spawnTimer != null) spawnTimer.play();
            if (bossWaveTimer != null) bossWaveTimer.play();

            for (FallingItem item : activeItems) {
                item.transition.play();
            }

            inputField.setDisable(false);
            inputField.requestFocus();
            hidePauseOverlay();
            updateMusicState();
            setStatus(t("resume"), "status-info");
        }
    }

    private void finishGame() {
        gameRunning = false;
        paused = false;

        if (gameTimer != null) gameTimer.stop();
        if (spawnTimer != null) spawnTimer.stop();
        if (bossWaveTimer != null) bossWaveTimer.stop();

        for (FallingItem item : new ArrayList<>(activeItems)) {
            item.transition.stop();
        }

        inputField.setDisable(true);
        startButton.setDisable(false);

        int currentWpm = calculateWpm();
        if (currentWpm > bestWpm) {
            bestWpm = currentWpm;
            saveHighScore();
        }

        gamesPlayed++;
        saveGamesPlayed();
        saveScoreEntry(loadUserName(), currentWpm);
        saveDailyPractice(totalGameSeconds - remainingSeconds);
        stopBackgroundMusic();

        updateStats();
        updateResultTexts();
        updateScoreboardList();
        updateWrongWordsList();
        showResultOverlay();
        playGameOverSound();
    }

    private void checkGameOverByLives() {
        if (lives <= 0) {
            finishGame();
        }
    }

    private void resetGame() {
        if (gameTimer != null) gameTimer.stop();
        if (spawnTimer != null) spawnTimer.stop();
        if (bossWaveTimer != null) bossWaveTimer.stop();

        gameRunning = false;
        paused = false;
        bossWaveActive = false;
        remainingSeconds = totalGameSeconds;
        lives = maxLives;
        correctCount = 0;
        wrongCount = 0;
        missedCount = 0;
        combo = 0;
        bestCombo = 0;
        level = 1;
        sessionTypedCharacters = 0;
        inputField.clear();
        inputField.setDisable(true);
        startButton.setDisable(false);
        clearActiveItems();
        clearCharts();
        wrongTypedWords.clear();

        updateWrongWordsList();
        updateScoreboardList();
        updateIdlePane();
        updateStats();
        updateLiveTypingPreview();
        setStatus(t("ready"), "status-neutral");
        hidePauseOverlay();
        hideResultOverlay();
        stopBackgroundMusic();
    }

    private void updateIdlePane() {
        removeIdleLabel();
        if (!gameRunning && gamePane != null) {
            Label idle = new Label(t("idlePane"));
            idle.getStyleClass().add("idle-label");
            idle.setLayoutX(Math.max(80, gamePane.getPrefWidth() / 2 - 180));
            idle.setLayoutY(Math.max(80, gamePane.getPrefHeight() / 2 - 20));
            gamePane.getChildren().add(idle);
        }
    }

    private void removeIdleLabel() {
        gamePane.getChildren().removeIf(node ->
                node instanceof Label label && label.getStyleClass().contains("idle-label"));
    }

    private void updateStats() {
        int totalAttempts = correctCount + wrongCount;

        timeValue.setText(String.valueOf(remainingSeconds));
        livesValue.setText("❤".repeat(Math.max(0, lives)));
        correctValue.setText(String.valueOf(correctCount));
        wrongValue.setText(String.valueOf(wrongCount));
        comboValue.setText(String.valueOf(combo));
        accuracyValue.setText("%" + Math.round(calculateAccuracy(totalAttempts)));
        wpmValue.setText(String.valueOf(calculateWpm()));
        bestValue.setText(String.valueOf(bestWpm));
        levelValue.setText(String.valueOf(level));
        gamesValue.setText(String.valueOf(gamesPlayed));

        timeProgressBar.setProgress(Math.max(0, remainingSeconds / (double) totalGameSeconds));
    }

    private void updateLiveTypingPreview() {
        livePreviewFlow.getChildren().clear();

        FallingItem target = getFocusTarget();
        String typed = inputField.getText();

        if (target == null) {
            Text idleText = new Text(t("previewIdle"));
            idleText.setFill(Color.GRAY);
            idleText.setStyle("-fx-font-size: 15px;");
            livePreviewFlow.getChildren().add(idleText);
            return;
        }

        String targetText = target.text;
        int correctPrefix = 0;
        while (correctPrefix < typed.length()
                && correctPrefix < targetText.length()
                && Character.toLowerCase(typed.charAt(correctPrefix)) == Character.toLowerCase(targetText.charAt(correctPrefix))) {
            correctPrefix++;
        }

        String ok = targetText.substring(0, correctPrefix);
        String wrongTyped = typed.length() > correctPrefix
                ? typed.substring(correctPrefix, Math.min(typed.length(), targetText.length()))
                : "";
        String remaining = targetText.substring(Math.min(typed.length(), targetText.length()));
        String overflow = typed.length() > targetText.length() ? typed.substring(targetText.length()) : "";

        Text okText = new Text(ok);
        okText.setFill(Color.web("#16a34a"));
        okText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Text wrongText = new Text(wrongTyped);
        wrongText.setFill(Color.web("#dc2626"));
        wrongText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Text remainingText = new Text(remaining);
        remainingText.setFill(Color.web("#64748b"));
        remainingText.setStyle("-fx-font-size: 18px;");

        Text overflowText = new Text(overflow);
        overflowText.setFill(Color.web("#b91c1c"));
        overflowText.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        livePreviewFlow.getChildren().addAll(okText, wrongText, remainingText, overflowText);
    }

    private void updateCharts() {
        int elapsed = totalGameSeconds - remainingSeconds;
        if (elapsed < 0) return;

        int totalAttempts = correctCount + wrongCount;
        accuracyHistory.add((int) Math.round(calculateAccuracy(totalAttempts)));
        wpmHistory.add(calculateWpm());

        accuracySeries.getData().add(new XYChart.Data<>(elapsed, accuracyHistory.get(accuracyHistory.size() - 1)));
        wpmSeries.getData().add(new XYChart.Data<>(elapsed, wpmHistory.get(wpmHistory.size() - 1)));
    }

    private void clearCharts() {
        accuracySeries.getData().clear();
        wpmSeries.getData().clear();
        accuracyHistory.clear();
        wpmHistory.clear();
    }

    private void updateResultTexts() {
        int totalAttempts = correctCount + wrongCount;
        long avgReaction = reactionTimes.isEmpty()
                ? 0
                : Math.round(reactionTimes.stream().mapToLong(Long::longValue).average().orElse(0));

        resultTitle.setText(t("resultTitle"));
        resultSummary.setText(t("summary") + ": " + loadUserName());
        resultWpm.setText("WPM: " + calculateWpm());
        resultAccuracy.setText(t("accuracy") + ": %" + Math.round(calculateAccuracy(totalAttempts)));
        resultCorrect.setText(t("correct") + ": " + correctCount);
        resultWrong.setText(t("wrong") + ": " + wrongCount);
        resultCombo.setText(t("combo") + ": " + bestCombo);
        resultMissed.setText("Missed: " + missedCount);
        resultLevel.setText(t("level") + ": " + level);
        resultDifficulty.setText(t("difficulty") + ": " + difficultyCombo.getValue());
        resultLanguage.setText(t("language") + ": " + languageCombo.getValue());
        resultMode.setText(t("mode") + ": " + modeCombo.getValue());
        resultCategory.setText(t("category") + ": " + categoryCombo.getValue());
        resultReaction.setText("Avg Reaction: " + avgReaction + " ms");
        resultDaily.setText(t("daily") + ": " + loadTodayPracticeText());
    }

    private void showResultOverlay() {
        updateResultTexts();
        resultOverlay.setVisible(true);
        resultOverlay.setManaged(true);
    }

    private void hideResultOverlay() {
        resultOverlay.setVisible(false);
        resultOverlay.setManaged(false);
    }

    private void showPauseOverlay() {
        pauseOverlay.setVisible(true);
        pauseOverlay.setManaged(true);
    }

    private void hidePauseOverlay() {
        pauseOverlay.setVisible(false);
        pauseOverlay.setManaged(false);
    }

    private void clearActiveItems() {
        for (FallingItem item : activeItems) {
            if (item.transition != null) item.transition.stop();
        }
        activeItems.clear();
        gamePane.getChildren().clear();
    }

    private double calculateAccuracy(int totalAttempts) {
        if (totalAttempts == 0) return 0;
        return (correctCount * 100.0) / totalAttempts;
    }

    private int calculateWpm() {
        int elapsed = totalGameSeconds - remainingSeconds;
        if (elapsed <= 0) return 0;

        if (currentMode == PlayMode.WORDS) {
            return (int) Math.round(correctCount / (elapsed / 60.0));
        } else {
            int charEquivalent = sessionTypedCharacters == 0 ? correctCount * 5 : sessionTypedCharacters;
            return (int) Math.round((charEquivalent / 5.0) / (elapsed / 60.0));
        }
    }

    private String getRandomTarget() {
        if (currentMode == PlayMode.SENTENCES) {
            List<String> pool = currentLanguage == LanguageOption.ENGLISH ? sentencesEN : sentencesTR;
            return pool.get(random.nextInt(pool.size()));
        }

        List<String> pool;
        if (currentLanguage == LanguageOption.ENGLISH) {
            pool = switch (currentCategory) {
                case PROGRAMMING -> programmingWordsEN;
                case SCHOOL -> schoolWordsEN;
                case TECHNOLOGY -> technologyWordsEN;
                default -> generalWordsEN;
            };
        } else {
            pool = switch (currentCategory) {
                case PROGRAMMING -> programmingWordsTR;
                case SCHOOL -> schoolWordsTR;
                case TECHNOLOGY -> technologyWordsTR;
                default -> generalWordsTR;
            };
        }

        List<String> adjusted = new ArrayList<>(pool);

        if (currentDifficulty == Difficulty.EASY) {
            adjusted = adjusted.stream().filter(w -> w.length() <= 8).collect(Collectors.toList());
        } else if (currentDifficulty == Difficulty.MEDIUM) {
            adjusted = adjusted.stream().filter(w -> w.length() >= 5).collect(Collectors.toList());
        } else {
            adjusted = adjusted.stream().sorted(Comparator.comparingInt(String::length).reversed()).collect(Collectors.toList());
        }

        if (bossWaveActive) {
            adjusted = adjusted.stream().sorted(Comparator.comparingInt(String::length).reversed()).collect(Collectors.toList());
        }

        if (adjusted.isEmpty()) adjusted = pool;
        return adjusted.get(random.nextInt(adjusted.size()));
    }

    private double getSpawnInterval() {
        double base = switch (currentDifficulty) {
            case EASY -> currentMode == PlayMode.SENTENCES ? 4.0 : 2.8;
            case MEDIUM -> currentMode == PlayMode.SENTENCES ? 3.5 : 2.25;
            case HARD -> currentMode == PlayMode.SENTENCES ? 2.5 : 1.5;
        };

        base -= (level - 1) * 0.06;
        if (bossWaveActive) base *= 0.75;
        return Math.max(0.45, base);
    }

    private double getFallDuration() {
        double base = switch (currentDifficulty) {
            case EASY -> currentMode == PlayMode.SENTENCES ? 10.0 : 8.2;
            case MEDIUM -> currentMode == PlayMode.SENTENCES ? 7.3 : 5.6;
            case HARD -> currentMode == PlayMode.SENTENCES ? 6.0 : 4.3;
        };

        base *= 1.2; // %20 

        base -= (level - 1) * 0.12;

        if (bossWaveActive) base *= 0.82;

        return Math.max(1.4, base);
    }

    private int getInitialCount() {
        int count = switch (currentDifficulty) {
            case EASY -> 2;
            case MEDIUM -> 3;
            case HARD -> 4;
        };

        if (currentMode == PlayMode.SENTENCES) {
            count = Math.max(1, count - 1);
        }

        return count;
    }

    private void setStatus(String text, String styleClass) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-neutral", "status-info", "status-correct", "status-wrong");
        if (!statusLabel.getStyleClass().contains(styleClass)) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    private void loadSounds() {
        correctClip = loadClip("correct.wav");
        wrongClip = loadClip("wrong.wav");
        gameOverClip = loadClip("gameover.wav");

        try {
            var musicUrl = getClass().getResource("bgmusic.mp3");
            if (musicUrl != null) {
                backgroundPlayer = new MediaPlayer(new Media(musicUrl.toExternalForm()));
                backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                backgroundPlayer.setVolume(0.35);
            }
        } catch (Exception ignored) {
            backgroundPlayer = null;
        }
    }

    private AudioClip loadClip(String name) {
        try {
            var url = getClass().getResource(name);
            if (url != null) {
                return new AudioClip(url.toExternalForm());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void playCorrectSound() {
        if (!soundEnabled) return;
        if (correctClip != null) correctClip.play();
        else Toolkit.getDefaultToolkit().beep();
    }

    private void playWrongSound() {
        if (!soundEnabled) return;
        if (wrongClip != null) wrongClip.play();
        else Toolkit.getDefaultToolkit().beep();
    }

    private void playGameOverSound() {
        if (!soundEnabled) return;
        if (gameOverClip != null) gameOverClip.play();
        else Toolkit.getDefaultToolkit().beep();
    }

    private void updateMusicState() {
        if (musicEnabled && gameRunning && !paused && backgroundPlayer != null) {
            backgroundPlayer.play();
        } else {
            pauseBackgroundMusic();
        }
    }

    private void pauseBackgroundMusic() {
        if (backgroundPlayer != null) {
            backgroundPlayer.pause();
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
        }
    }

    private void saveScoreEntry(String name, int wpm) {
        try {
            List<String> scores = new ArrayList<>();
            if (Files.exists(scoresFile)) {
                scores.addAll(Files.readAllLines(scoresFile));
            }

            String safeName = (name == null || name.isBlank()) ? "Player" : name.trim();
            scores.add(safeName + "|" + wpm);

            scores = scores.stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank() && s.contains("|"))
                    .sorted((a, b) -> {
                        int wa = Integer.parseInt(a.split("\\|")[1]);
                        int wb = Integer.parseInt(b.split("\\|")[1]);
                        return Integer.compare(wb, wa);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            Files.write(scoresFile, scores, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    private void updateScoreboardList() {
        List<String> lines = new ArrayList<>();
        try {
            if (Files.exists(scoresFile)) {
                lines = Files.readAllLines(scoresFile).stream()
                        .filter(s -> s.contains("|"))
                        .map(s -> {
                            String[] p = s.split("\\|");
                            return p[0] + " - " + p[1] + " WPM";
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }

        scoreboardList.setItems(FXCollections.observableArrayList(lines));
    }

    private void updateWrongWordsList() {
        Map<String, Long> grouped = wrongTypedWords.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.groupingBy(s -> s, LinkedHashMap::new, Collectors.counting()));

        List<String> items = grouped.entrySet().stream()
                .map(e -> e.getKey() + "  x" + e.getValue())
                .limit(20)
                .collect(Collectors.toList());

        wrongWordsList.setItems(FXCollections.observableArrayList(items));
    }

    private void loadProfile() {
        createStorage();
        Properties p = new Properties();
        if (Files.exists(profileFile)) {
            try (InputStream in = Files.newInputStream(profileFile)) {
                p.load(in);
                String bp = p.getProperty("bestWpm");
                String gp = p.getProperty("gamesPlayed");
                if (bp != null) bestWpm = Integer.parseInt(bp);
                if (gp != null) gamesPlayed = Integer.parseInt(gp);
            } catch (Exception ignored) {
            }
        }
    }

    private void saveProfileValue(String key, String value) {
        try {
            Properties p = new Properties();
            if (Files.exists(profileFile)) {
                try (InputStream in = Files.newInputStream(profileFile)) {
                    p.load(in);
                }
            }
            p.setProperty(key, value);
            p.store(Files.newOutputStream(profileFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), "TypingTutorProfile");
        } catch (Exception ignored) {
        }
    }

    private void saveUserName(String name) {
        saveProfileValue("userName", name == null ? "" : name.trim());
    }

    private String loadUserName() {
        try {
            Properties p = new Properties();
            if (Files.exists(profileFile)) {
                try (InputStream in = Files.newInputStream(profileFile)) {
                    p.load(in);
                    return p.getProperty("userName", "Player");
                }
            }
        } catch (Exception ignored) {
        }
        return "Player";
    }

    private void saveHighScore() {
        saveProfileValue("bestWpm", String.valueOf(bestWpm));
    }

    private void loadHighScore() {
        try {
            Properties p = new Properties();
            if (Files.exists(profileFile)) {
                try (InputStream in = Files.newInputStream(profileFile)) {
                    p.load(in);
                    bestWpm = Integer.parseInt(p.getProperty("bestWpm", "0"));
                }
            }
        } catch (Exception ignored) {
            bestWpm = 0;
        }
    }

    private void saveGamesPlayed() {
        saveProfileValue("gamesPlayed", String.valueOf(gamesPlayed));
    }

    private void loadDailyStats() {
        if (!Files.exists(dailyFile)) return;
        try {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(dailyFile)) {
                p.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    private void saveDailyPractice(int seconds) {
        try {
            Properties p = new Properties();
            if (Files.exists(dailyFile)) {
                try (InputStream in = Files.newInputStream(dailyFile)) {
                    p.load(in);
                }
            }

            String key = today.toString();
            int oldSeconds = Integer.parseInt(p.getProperty(key + ".seconds", "0"));
            int oldGames = Integer.parseInt(p.getProperty(key + ".games", "0"));

            p.setProperty(key + ".seconds", String.valueOf(oldSeconds + Math.max(0, seconds)));
            p.setProperty(key + ".games", String.valueOf(oldGames + 1));

            p.store(Files.newOutputStream(dailyFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), "TypingTutorDaily");
        } catch (Exception ignored) {
        }
    }

    private String loadTodayPracticeText() {
        try {
            Properties p = new Properties();
            if (Files.exists(dailyFile)) {
                try (InputStream in = Files.newInputStream(dailyFile)) {
                    p.load(in);
                }
            }
            String key = today.toString();
            int sec = Integer.parseInt(p.getProperty(key + ".seconds", "0"));
            int games = Integer.parseInt(p.getProperty(key + ".games", "0"));
            return sec + " sec / " + games + " games";
        } catch (Exception ignored) {
            return "0 sec / 0 games";
        }
    }

    @Override
    public void stop() {
        stopBackgroundMusic();
    }

    public static void main(String[] args) {
        launch(args);
    }
}