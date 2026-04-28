package pentago;

import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PentagoApp extends Application {

    // 0 boş, 1 Beyaz, 2 Siyah
    private final int[][] board = new int[6][6];

    private boolean vsComputer = false;
    private int currentPlayer = 1;
    private boolean gameOver = false;
    private boolean placedThisTurn = false;

    // UI refs
    private Stage primaryStage;
    private BorderPane root;

    private final Circle[][] discs = new Circle[6][6];
    private final StackPane[][] cellPanes = new StackPane[6][6];
    private final Rectangle[][] cellGlow = new Rectangle[6][6];
    private final Rectangle[][] cellBg = new Rectangle[6][6];

    private final GridPane[] quadrants = new GridPane[4];

    private Label status;
    private VBox rightPanel;

    private Label modeLabel;

    // Score
    private Label scoreLabelWhite;
    private Label scoreLabelBlack;
    private int scoreWhite = 0;
    private int scoreBlack = 0;

    // Stats
    private int gamesPlayed = 0;
    private int draws = 0;
    private int totalMovesAllGames = 0;
    private int movesThisGame = 0;

    private Label statsGames;
    private Label statsWhiteWins;
    private Label statsBlackWins;
    private Label statsDraws;
    private Label statsMovesThis;
    private Label statsAvgMoves;

    // Hint + difficulty + theme
    private Label hintLabel;
    private ToggleButton autoHintToggle;
    private ComboBox<PentagoAI.Difficulty> difficultyBox;

    private enum Theme { NEON("Neon"), CLASSIC("Klasik"), WOOD("Wood");
        final String label; Theme(String label){this.label=label;}
        @Override public String toString(){return label;}
    }
    private ComboBox<Theme> themeBox;

    private final PentagoAI ai = new PentagoAI();

    // Hint visuals
    private Rectangle hintCellOutline = null;
    private FadeTransition hintPulse = null;

    // Win animation
    private final List<FadeTransition> winPulses = new ArrayList<>();

    // Theme palette (set by applyTheme)
    private String BG_APP, PANEL_BG, PANEL_BG_HOVER, BORDER, BORDER_HOVER, TEXT_MAIN, TEXT_DIM;
    private String ACCENT, ACCENT2, DISC_BLACK_STROKE, CELL_BG, CELL_BG_STROKE;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        root = new BorderPane();
        root.setPadding(new Insets(14));

        // TOP
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(0, 0, 10, 0));

        modeLabel = new Label("Mod: -");
        modeLabel.setFont(Font.font(15));

        Button pvpBtn = mkButton("2 Kişilik");
        Button pvcBtn = mkButton("Bilgisayara Karşı");
        Button resetBtn = mkButton("Reset (Tahta)");
        Button resetAllBtn = mkButton("Reset (Skor+İstatistik)");

        pvpBtn.setOnAction(e -> startNewGame(false, true));
        pvcBtn.setOnAction(e -> startNewGame(true, true));
        resetBtn.setOnAction(e -> resetBoardKeepScore());
        resetAllBtn.setOnAction(e -> resetAll());

        top.getChildren().addAll(modeLabel, pvpBtn, pvcBtn, resetBtn, resetAllBtn);

        // CENTER
        VBox center = new VBox(12);
        center.setAlignment(Pos.CENTER);
        GridPane boardUI = buildBoardUI();
        center.getChildren().add(boardUI);

        // RIGHT
        rightPanel = new VBox(12);
        VBox right = rightPanel;

        right.setPadding(new Insets(6, 0, 0, 14));
        right.setPrefWidth(320);

        Label title = new Label("PENTAGO");
        title.setFont(Font.font(20));
        title.setStyle("-fx-font-weight: bold;");

        // SCORE
        Label scoreTitle = new Label("SKOR");
        scoreTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        scoreLabelWhite = new Label("Beyaz: 0");
        scoreLabelBlack = new Label("Siyah: 0");

        // STATS
        Separator sep1 = new Separator(); sep1.setOpacity(0.35);
        Label statsTitle = new Label("İSTATİSTİK");
        statsTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        statsGames = new Label("Oyun: 0");
        statsWhiteWins = new Label("Beyaz galibiyet: 0");
        statsBlackWins = new Label("Siyah galibiyet: 0");
        statsDraws = new Label("Beraberlik: 0");
        statsMovesThis = new Label("Bu oyun hamle: 0");
        statsAvgMoves = new Label("Ortalama hamle: 0.0");

        // AI DIFF
        Separator sep2 = new Separator(); sep2.setOpacity(0.35);
        Label aiTitle = new Label("AI ZORLUK");
        aiTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        difficultyBox = new ComboBox<>();
        difficultyBox.getItems().addAll(PentagoAI.Difficulty.EASY, PentagoAI.Difficulty.NORMAL, PentagoAI.Difficulty.HARD);
        difficultyBox.setValue(PentagoAI.Difficulty.NORMAL);
        difficultyBox.setMaxWidth(Double.MAX_VALUE);

        Label aiInfo = new Label("Not: Zorluk sadece\n“Bilgisayara Karşı” modunda etkili.");
        aiInfo.setWrapText(true);

        // THEME
        Separator sep3 = new Separator(); sep3.setOpacity(0.35);
        Label themeTitle = new Label("TEMA");
        themeTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        themeBox = new ComboBox<>();
        themeBox.getItems().addAll(Theme.NEON, Theme.CLASSIC, Theme.WOOD);
        themeBox.setValue(Theme.NEON);
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setOnAction(e -> applyTheme(themeBox.getValue()));

     // HELP
        Separator sep4 = new Separator(); 
        sep4.setOpacity(0.35);

        Label helpTitle = new Label("YARDIM (A/B/C + Neden?)");
        helpTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Button hintBtn = mkButton("Öneriyi Göster");
        hintBtn.setOnAction(e -> showHintNow());

        autoHintToggle = new ToggleButton("Otomatik Öneri: Kapalı");
        autoHintToggle.setOnAction(e -> {
            autoHintToggle.setText(autoHintToggle.isSelected() ? "Otomatik Öneri: Açık" : "Otomatik Öneri: Kapalı");
            if (autoHintToggle.isSelected()) showHintNow();
            else clearHintVisual();
        });

        // ✅ hintLabel
        hintLabel = new Label("İpucu: Taş koy → Quadrant’a tıkla\nSOL tık=⟲  SAĞ tık=⟳");
        hintLabel.setMaxWidth(280);
        hintLabel.setWrapText(true);

        // ✅ hintScroll (hintLabel bunun içinde)
        ScrollPane hintScroll = new ScrollPane(hintLabel);
        hintScroll.setFitToWidth(true);
        hintScroll.setPrefViewportHeight(140);
        hintScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        hintScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        hintScroll.setStyle("""
            -fx-background: transparent;
            -fx-background-color: transparent;
            -fx-border-color: transparent;
        """);

        // Rules
        Label rules = new Label("""
        Kontroller:
        • Hücre SOL tık: Taş koy
        • Quadrant SOL tık: ⟲ döndür
        • Quadrant SAĞ tık: ⟳ döndür

        Amaç: 5 taşı yan yana diz.
        """);
        rules.setWrapText(true);

        // ✅ right panel’e EKLEME (hintLabel değil, hintScroll eklenecek)
        right.getChildren().addAll(
                title,
                scoreTitle, scoreLabelWhite, scoreLabelBlack,
                sep1, statsTitle, statsGames, statsWhiteWins, statsBlackWins, statsDraws, statsMovesThis, statsAvgMoves,
                sep2, aiTitle, difficultyBox, aiInfo,
                sep3, themeTitle, themeBox,
                sep4, helpTitle, hintBtn, autoHintToggle, hintScroll,
                rules
        );

                
        

        // BOTTOM
        status = new Label("Mod seç: 2 Kişilik veya Bilgisayara Karşı");
        status.setFont(Font.font(14));
        status.setPadding(new Insets(10, 0, 0, 0));

        root.setTop(top);
        root.setCenter(center);
        root.setRight(right);
        root.setBottom(status);

        Scene scene = new Scene(root, 1120, 780);
        stage.setTitle("PentagoFX PRO++ (Theme + Stats + Win FX + Hint A/B/C)");
        stage.setScene(scene);
        stage.show();

        // Apply initial theme
        applyTheme(Theme.NEON);
        updateModeLabel();
        renderAll();
        updateDifficultyEnabled();
        updateStatsLabels();
    }

    // ---------------- Theme + Styling ----------------

    private void applyTheme(Theme t) {
        // palette
        if (t == Theme.NEON) {
            BG_APP = "#0b0f1a";
            PANEL_BG = "#0f1730";
            PANEL_BG_HOVER = "#101c38";
            BORDER = "#2a3b66";
            BORDER_HOVER = "#7cf7ff";
            TEXT_MAIN = "#cfe8ff";
            TEXT_DIM = "#9fb6d9";
            ACCENT = "#7cf7ff";
            ACCENT2 = "#ff4dff";
            DISC_BLACK_STROKE = "#7cf7ff";
            CELL_BG = "#0b1226";
            CELL_BG_STROKE = "#2a3b66";
        } else if (t == Theme.CLASSIC) {
            BG_APP = "#141414";
            PANEL_BG = "#202020";
            PANEL_BG_HOVER = "#262626";
            BORDER = "#444444";
            BORDER_HOVER = "#bbbbbb";
            TEXT_MAIN = "#f2f2f2";
            TEXT_DIM = "#b8b8b8";
            ACCENT = "#dddddd";
            ACCENT2 = "#ffd86a";
            DISC_BLACK_STROKE = "#d0d0d0";
            CELL_BG = "#1b1b1b";
            CELL_BG_STROKE = "#4a4a4a";
        } else { // WOOD
            BG_APP = "#1b130c";
            PANEL_BG = "#2a1d12";
            PANEL_BG_HOVER = "#332315";
            BORDER = "#6b4a2c";
            BORDER_HOVER = "#d7b27a";
            TEXT_MAIN = "#f6ead8";
            TEXT_DIM = "#cbb79a";
            ACCENT = "#d7b27a";
            ACCENT2 = "#76f7c5";
            DISC_BLACK_STROKE = "#e6d4b8";
            CELL_BG = "#20150d";
            CELL_BG_STROKE = "#6b4a2c";
         // ✅ Sağ paneldeki tüm yazıları tema rengine çek
            if (rightPanel != null) {
                rightPanel.lookupAll(".label").forEach(n -> {
                    if (n instanceof Label lb) {
                        String txt = lb.getText() == null ? "" : lb.getText();
                        boolean isTitle = txt.equals(txt.toUpperCase()) && txt.length() <= 18;
                        lb.setStyle("-fx-text-fill: " + (isTitle ? TEXT_MAIN : TEXT_DIM) + ";");
                    }
                });
            }

        }

        root.setStyle("-fx-background-color: " + BG_APP + ";");

        modeLabel.setStyle("-fx-text-fill: " + TEXT_MAIN + ";");
        status.setStyle("-fx-text-fill: " + TEXT_MAIN + ";");

        // right panel labels
        // (tek tek uğraşmamak için right panel içindeki tüm label’lara stil vermiyoruz;
        //  ama görünüm zaten palete uyuyor. Aşağıdakiler kritik.)
        scoreLabelWhite.setStyle("-fx-text-fill: " + TEXT_MAIN + "; -fx-font-size: 14;");
        scoreLabelBlack.setStyle("-fx-text-fill: " + TEXT_MAIN + "; -fx-font-size: 14;");

        statsGames.setStyle("-fx-text-fill: " + TEXT_DIM + ";");
        statsWhiteWins.setStyle("-fx-text-fill: " + TEXT_DIM + ";");
        statsBlackWins.setStyle("-fx-text-fill: " + TEXT_DIM + ";");
        statsDraws.setStyle("-fx-text-fill: " + TEXT_DIM + ";");
        statsMovesThis.setStyle("-fx-text-fill: " + TEXT_DIM + ";");
        statsAvgMoves.setStyle("-fx-text-fill: " + TEXT_DIM + ";");

        hintLabel.setStyle("-fx-text-fill: " + TEXT_DIM + "; -fx-font-size: 12;");
        autoHintToggle.setStyle("""
            -fx-background-color: %s;
            -fx-text-fill: %s;
            -fx-border-color: %s;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            -fx-padding: 10 12;
        """.formatted(PANEL_BG, TEXT_MAIN, BORDER));

        difficultyBox.setStyle("""
            -fx-background-color: %s;
            -fx-border-color: %s;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            -fx-padding: 6 8;
            -fx-text-fill: %s;
        """.formatted(PANEL_BG, BORDER, TEXT_MAIN));

        themeBox.setStyle("""
            -fx-background-color: %s;
            -fx-border-color: %s;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            -fx-padding: 6 8;
            -fx-text-fill: %s;
        """.formatted(PANEL_BG, BORDER, TEXT_MAIN));

        // quadrants + cells
        for (int q = 0; q < 4; q++) {
            setQuadrantStyle(q, false);
        }
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                cellBg[r][c].setFill(Color.web(CELL_BG));
                cellBg[r][c].setStroke(Color.web(CELL_BG_STROKE));
            }
        }
        renderAll();
    }

    private Button mkButton(String text) {
        Button b = new Button(text);
        b.setStyle(buttonStyle(false));
        b.setOnMouseEntered(e -> b.setStyle(buttonStyle(true)));
        b.setOnMouseExited(e -> b.setStyle(buttonStyle(false)));
        return b;
    }

    private String buttonStyle(boolean hover) {
        if (!hover) {
            return """
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
                -fx-border-width: 1.5;
                -fx-border-radius: 12;
                -fx-background-radius: 12;
                -fx-padding: 10 14;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0.2, 0, 0);
            """.formatted(PANEL_BG, TEXT_MAIN, BORDER);
        } else {
            return """
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
                -fx-border-width: 1.8;
                -fx-border-radius: 12;
                -fx-background-radius: 12;
                -fx-padding: 10 14;
                -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.18), 18, 0.25, 0, 0);
            """.formatted(PANEL_BG_HOVER, TEXT_MAIN, BORDER_HOVER);
        }
    }


    private void setQuadrantStyle(int q, boolean hover) {
        if (!hover) {
            quadrants[q].setStyle("""
                -fx-background-color: %s;
                -fx-background-radius: 18;
                -fx-border-color: %s;
                -fx-border-width: 2;
                -fx-border-radius: 18;
            """.formatted(PANEL_BG, BORDER));
        } else {
            quadrants[q].setStyle("""
                -fx-background-color: %s;
                -fx-background-radius: 18;
                -fx-border-color: %s;
                -fx-border-width: 2;
                -fx-border-radius: 18;
                -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.18), 22, 0.22, 0, 0);
            """.formatted(PANEL_BG_HOVER, BORDER_HOVER));
        }
    }

    // ---------------- Board UI ----------------

    private GridPane buildBoardUI() {
        GridPane wrapper = new GridPane();
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setHgap(18);
        wrapper.setVgap(18);

        for (int q = 0; q < 4; q++) {
            GridPane gp = new GridPane();
            gp.setHgap(7);
            gp.setVgap(7);
            gp.setPadding(new Insets(12));

            final int quadrantIndex = q;

            gp.setOnMouseClicked(e -> {
                if (gameOver) return;
                if (!placedThisTurn) {
                    status.setText("Önce taş koymalısın. (Hücreye sol tık)");
                    return;
                }
                boolean cw;
                if (e.getButton() == MouseButton.PRIMARY) cw = false;         // SOL = CCW
                else if (e.getButton() == MouseButton.SECONDARY) cw = true;  // SAĞ = CW
                else return;

                rotateQuadrant(quadrantIndex, cw);
            });

            gp.setOnMouseEntered(e -> setQuadrantStyle(quadrantIndex, true));
            gp.setOnMouseExited(e -> setQuadrantStyle(quadrantIndex, false));

            quadrants[q] = gp;

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int globalR = (q < 2 ? 0 : 3) + r;
                    int globalC = (q % 2 == 0 ? 0 : 3) + c;

                    StackPane cell = makeCell(globalR, globalC);
                    gp.add(cell, c, r);
                    cellPanes[globalR][globalC] = cell;
                }
            }
        }

        wrapper.add(quadrants[0], 0, 0);
        wrapper.add(quadrants[1], 1, 0);
        wrapper.add(quadrants[2], 0, 1);
        wrapper.add(quadrants[3], 1, 1);

        return wrapper;
    }

    private StackPane makeCell(int r, int c) {
        StackPane sp = new StackPane();
        sp.setPrefSize(78, 78);

        Rectangle bg = new Rectangle(78, 78);
        bg.setArcHeight(18);
        bg.setArcWidth(18);
        bg.setFill(Color.web("#0b1226"));
        bg.setStroke(Color.web("#2a3b66"));
        bg.setStrokeWidth(1.6);
        cellBg[r][c] = bg;

        Rectangle glow = new Rectangle(78, 78);
        glow.setArcHeight(18);
        glow.setArcWidth(18);
        glow.setFill(Color.TRANSPARENT);
        glow.setStroke(Color.TRANSPARENT);
        glow.setStrokeWidth(2.8);
        cellGlow[r][c] = glow;

        Circle disc = new Circle(23);
        disc.setFill(Color.TRANSPARENT);
        disc.setStroke(Color.web("#2a3b66"));
        disc.setStrokeWidth(1.2);
        discs[r][c] = disc;

        sp.getChildren().addAll(bg, glow, disc);

        sp.setOnMouseEntered(e -> {
            if (!gameOver && !placedThisTurn && board[r][c] == 0 && canCurrentPlayerClickPlace()) {
                glow.setStroke(Color.web(ACCENT));
                glow.setOpacity(0.9);
            }
        });
        sp.setOnMouseExited(e -> {
            if (hintCellOutline != glow && !winPulsesContains(glow)) {
                glow.setStroke(Color.TRANSPARENT);
                glow.setOpacity(1.0);
                glow.setStyle("");
            }
        });

        sp.setOnMouseClicked(e -> {
            if (gameOver) return;
            if (e.getButton() != MouseButton.PRIMARY) return;

            // ✅ hücre tıklaması quadrant'a gitmesin
            e.consume();

            if (!canCurrentPlayerClickPlace()) return;

            handlePlace(r, c);
        });

        return sp;
    }

    private boolean winPulsesContains(Rectangle glowRect) {
        // Eğer kazanma animasyonu aktifse glow’u silmeyelim (basit kontrol)
        for (FadeTransition ft : winPulses) {
            if (ft.getNode() == glowRect) return true;
        }
        return false;
    }

    private boolean canCurrentPlayerClickPlace() {
        // PvE'de insan sadece beyaz
        if (vsComputer && currentPlayer != 1) return false;
        return true;
    }

    // ---------------- Game flow ----------------

    private void startNewGame(boolean vsComputerMode, boolean resetScore) {
        vsComputer = vsComputerMode;
        if (resetScore) {
            scoreWhite = 0;
            scoreBlack = 0;
            updateScoreLabels();
        }
        resetBoardKeepScore();
        updateModeLabel();
        updateDifficultyEnabled();
    }

    private void resetAll() {
        scoreWhite = 0;
        scoreBlack = 0;
        gamesPlayed = 0;
        draws = 0;
        totalMovesAllGames = 0;
        movesThisGame = 0;
        updateScoreLabels();
        updateStatsLabels();
        resetBoardKeepScore();
    }

    private void resetBoardKeepScore() {
        currentPlayer = 1;
        gameOver = false;
        placedThisTurn = false;

        clearHintVisual();
        stopWinAnimation();

        for (int r = 0; r < 6; r++) for (int c = 0; c < 6; c++) board[r][c] = 0;

        movesThisGame = 0;
        renderAll();
        status.setText("Sıra: " + playerName(currentPlayer) + " (Taş koy)");
        updateStatsLabels();
        maybeAutoHint();
    }

    private void updateModeLabel() {
        modeLabel.setText("Mod: " + (vsComputer ? "Bilgisayara Karşı (Sen=BEYAZ)" : "2 Kişilik"));
    }

    private void updateDifficultyEnabled() {
        difficultyBox.setDisable(!vsComputer);
    }

    private void handlePlace(int r, int c) {
        if (board[r][c] != 0) {
            status.setText("Orası dolu. Boş bir yere koy.");
            return;
        }
        if (placedThisTurn) {
            status.setText("Bu tur taş koydun. Şimdi quadrant’a tıkla (Sol=⟲, Sağ=⟳).");
            return;
        }

        board[r][c] = currentPlayer;
        placedThisTurn = true;

        movesThisGame++;
        updateStatsLabels();

        renderAll();

        clearHintVisual();
        status.setText(playerName(currentPlayer) + " taş koydu. Şimdi quadrant’a tıkla (Sol=⟲, Sağ=⟳).");
        hintLabel.setText("Döndürme: Quadrant’a SOL tık=⟲, SAĞ tık=⟳");
    }

    private void rotateQuadrant(int q, boolean clockwise) {
        RotateTransition rt = new RotateTransition(Duration.millis(220), quadrants[q]);
        rt.setByAngle(clockwise ? 90 : -90);

        rt.setOnFinished(ev -> {
            quadrants[q].setRotate(0);

            rotateModelBoard(q, clockwise);
            renderAll();

            int winner = checkWinner(); // -1 draw-both, 0 none, 1 white, 2 black
            if (winner != 0) {
                gameOver = true;

                if (winner == 1) scoreWhite++;
                else if (winner == 2) scoreBlack++;
                else draws++;

                gamesPlayed++;
                totalMovesAllGames += movesThisGame;

                updateScoreLabels();
                updateStatsLabels();

                String msg;
                if (winner == -1) msg = "Beraberlik! İki taraf da 5 yaptı.";
                else msg = "Kazanan: " + playerName(winner) + " 🎉";

                status.setText(msg);

                // ✅ Win animation (if single winner)
                if (winner == 1 || winner == 2) {
                    playWinAnimation(winner);
                }

                showEndPopup(msg);
                return;
            }

            if (isBoardFull()) {
                gameOver = true;

                gamesPlayed++;
                draws++;
                totalMovesAllGames += movesThisGame;
                updateStatsLabels();

                String msg = "Beraberlik! Tahta doldu.";
                status.setText(msg);
                showEndPopup(msg);
                return;
            }

            placedThisTurn = false;
            currentPlayer = 3 - currentPlayer;

            clearHintVisual();

            if (vsComputer && currentPlayer == 2 && !gameOver) {
                status.setText("Bilgisayar düşünüyor... (" + difficultyBox.getValue().label + ")");
                computerTurn();
            } else {
                status.setText("Sıra: " + playerName(currentPlayer) + " (Taş koy)");
                maybeAutoHint();
            }
        });

        rt.play();
    }

    private void showEndPopup(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Oyun Bitti");
            alert.setHeaderText(message);
            alert.setContentText("Yeni oyun başlatılsın mı? (Skor korunur)");

            if (primaryStage != null) {
                alert.initOwner(primaryStage);
                alert.initModality(Modality.WINDOW_MODAL);
            }

            ButtonType yes = new ButtonType("Evet", ButtonBar.ButtonData.OK_DONE);
            ButtonType no  = new ButtonType("Hayır", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(yes, no);

            alert.setOnShown(ev -> {
                if (alert.getDialogPane().getScene() != null &&
                        alert.getDialogPane().getScene().getWindow() != null) {
                	var w = alert.getDialogPane().getScene().getWindow();
                    w.requestFocus();

                    if (w instanceof Stage s) {
                        s.toFront(); 
                }
            }}
                );

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == yes) {
                resetBoardKeepScore();
            } else {
                status.setText(message + " | Reset ile yeni tahta açabilirsin.");
            }
        });
    }

    private void computerTurn() {
        PentagoAI.Difficulty diff = difficultyBox.getValue();
        PentagoAI.Move mv = ai.chooseMove(board, 2, diff);

        if (mv == null) {
            gameOver = true;
            String msg = "Beraberlik! Hamle kalmadı.";
            status.setText(msg);
            showEndPopup(msg);
            return;
        }

        board[mv.r][mv.c] = 2;
        placedThisTurn = true;

        movesThisGame++;
        updateStatsLabels();

        renderAll();

        rotateQuadrant(mv.quadrant, mv.clockwise);
    }

    private void updateScoreLabels() {
        scoreLabelWhite.setText("Beyaz: " + scoreWhite);
        scoreLabelBlack.setText("Siyah: " + scoreBlack);
    }

    private void updateStatsLabels() {
        statsGames.setText("Oyun: " + gamesPlayed);
        statsWhiteWins.setText("Beyaz galibiyet: " + scoreWhite);
        statsBlackWins.setText("Siyah galibiyet: " + scoreBlack);
        statsDraws.setText("Beraberlik: " + draws);
        statsMovesThis.setText("Bu oyun hamle: " + movesThisGame);

        double avg = (gamesPlayed == 0) ? 0.0 : (double) totalMovesAllGames / gamesPlayed;
        statsAvgMoves.setText("Ortalama hamle: " + String.format("%.1f", avg));
    }

    // ---------------- Hint system (A/B/C + reason) ----------------

    private void maybeAutoHint() {
        if (!autoHintToggle.isSelected()) return;
        if (vsComputer && currentPlayer != 1) return;
        showHintNow();
    }

    private void showHintNow() {
        if (gameOver) return;
        if (placedThisTurn) {
            hintLabel.setText("Bu tur taş koydun. Şimdi quadrant’a tıkla (Sol=⟲, Sağ=⟳).");
            return;
        }
        if (vsComputer && currentPlayer != 1) return;

        PentagoAI.Difficulty diff = vsComputer ? difficultyBox.getValue() : PentagoAI.Difficulty.NORMAL;

        List<PentagoAI.Advice> adv = ai.getTopAdvices(board, currentPlayer, diff, 3);
        if (adv.isEmpty()) {
            hintLabel.setText("İpucu yok (hamle kalmadı).");
            return;
        }

        // highlight best (A)
        clearHintVisual();
        PentagoAI.Advice best = adv.get(0);

        Rectangle glow = cellGlow[best.move.r][best.move.c];
        hintCellOutline = glow;

        glow.setStroke(Color.web(ACCENT2));
        glow.setOpacity(1.0);
        glow.setStrokeWidth(3.0);
        glow.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.22), 18, 0.3, 0, 0);");

        hintPulse = new FadeTransition(Duration.millis(650), glow);
        hintPulse.setFromValue(0.25);
        hintPulse.setToValue(1.0);
        hintPulse.setAutoReverse(true);
        hintPulse.setCycleCount(8);
        hintPulse.play();

        // build text A/B/C
        StringBuilder sb = new StringBuilder();
        sb.append("A) ").append(formatMove(best)).append("\n   Neden: ").append(best.reason).append("\n");
        if (adv.size() > 1) {
            sb.append("B) ").append(formatMove(adv.get(1))).append("\n");
        }
        if (adv.size() > 2) {
            sb.append("C) ").append(formatMove(adv.get(2))).append("\n");
        }
        hintLabel.setText(sb.toString().trim());
        status.setText("💡 Öneri A: hücre işaretlendi (mor).");
    }

    private String formatMove(PentagoAI.Advice a) {
        String dir = a.move.clockwise ? "⟳" : "⟲";
        return "(" + (a.move.r + 1) + "," + (a.move.c + 1) + ") sonra " + quadrantName(a.move.quadrant) + " " + dir;
    }

    private void clearHintVisual() {
        if (hintPulse != null) {
            hintPulse.stop();
            hintPulse = null;
        }
        if (hintCellOutline != null) {
            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 6; c++) {
                    cellGlow[r][c].setStroke(Color.TRANSPARENT);
                    cellGlow[r][c].setStyle("");
                }
            }
            hintCellOutline = null;
        }
    }

    private String quadrantName(int q) {
        return switch (q) {
            case 0 -> "Sol-Üst";
            case 1 -> "Sağ-Üst";
            case 2 -> "Sol-Alt";
            case 3 -> "Sağ-Alt";
            default -> "Q?";
        };
    }

    // ---------------- Win animation ----------------

    private void stopWinAnimation() {
        for (FadeTransition ft : winPulses) ft.stop();
        winPulses.clear();

        // reset glows (win effect might have set them)
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                if (hintCellOutline != cellGlow[r][c]) {
                    cellGlow[r][c].setStroke(Color.TRANSPARENT);
                    cellGlow[r][c].setStyle("");
                    cellGlow[r][c].setOpacity(1.0);
                }
            }
        }
    }

    private void playWinAnimation(int winner) {
        stopWinAnimation();

        List<int[]> line = findWinningLine(winner);
        if (line.isEmpty()) return;

        // winner highlight color
        String winColor = (winner == 1) ? ACCENT : BORDER_HOVER;

        for (int[] rc : line) {
            int r = rc[0], c = rc[1];
            Rectangle glow = cellGlow[r][c];
            glow.setStroke(Color.web(winColor));
            glow.setStrokeWidth(3.5);
            glow.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,255,255,0.25), 22, 0.25, 0, 0);");

            FadeTransition ft = new FadeTransition(Duration.millis(520), glow);
            ft.setFromValue(0.25);
            ft.setToValue(1.0);
            ft.setAutoReverse(true);
            ft.setCycleCount(10);
            ft.play();
            winPulses.add(ft);
        }
    }

    private List<int[]> findWinningLine(int p) {
        List<int[]> out = new ArrayList<>();

        // horizontal windows 5
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c <= 1; c++) {
                if (board[r][c] == p && board[r][c+1] == p && board[r][c+2] == p && board[r][c+3] == p && board[r][c+4] == p) {
                    for (int k = 0; k < 5; k++) out.add(new int[]{r, c+k});
                    return out;
                }
            }
        }
        // vertical
        for (int c = 0; c < 6; c++) {
            for (int r = 0; r <= 1; r++) {
                if (board[r][c] == p && board[r+1][c] == p && board[r+2][c] == p && board[r+3][c] == p && board[r+4][c] == p) {
                    for (int k = 0; k < 5; k++) out.add(new int[]{r+k, c});
                    return out;
                }
            }
        }
        // diag TL->BR
        for (int r = 0; r <= 1; r++) {
            for (int c = 0; c <= 1; c++) {
                if (board[r][c] == p && board[r+1][c+1] == p && board[r+2][c+2] == p && board[r+3][c+3] == p && board[r+4][c+4] == p) {
                    for (int k = 0; k < 5; k++) out.add(new int[]{r+k, c+k});
                    return out;
                }
            }
        }
        // diag TR->BL
        for (int r = 0; r <= 1; r++) {
            for (int c = 4; c < 6; c++) {
                if (board[r][c] == p && board[r+1][c-1] == p && board[r+2][c-2] == p && board[r+3][c-3] == p && board[r+4][c-4] == p) {
                    for (int k = 0; k < 5; k++) out.add(new int[]{r+k, c-k});
                    return out;
                }
            }
        }
        return out;
    }

    // ---------------- Model ops ----------------

    private void rotateModelBoard(int q, boolean clockwise) {
        int r0 = (q < 2 ? 0 : 3);
        int c0 = (q % 2 == 0 ? 0 : 3);

        int[][] sub = new int[3][3];
        for (int r = 0; r < 3; r++) System.arraycopy(board[r0 + r], c0, sub[r], 0, 3);

        int[][] rotated = new int[3][3];
        if (clockwise) {
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) rotated[c][2 - r] = sub[r][c];
        } else {
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) rotated[2 - c][r] = sub[r][c];
        }

        for (int r = 0; r < 3; r++) System.arraycopy(rotated[r], 0, board[r0 + r], c0, 3);
    }

    private void renderAll() {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                int v = board[r][c];
                Circle disc = discs[r][c];
                if (v == 0) {
                    disc.setFill(Color.TRANSPARENT);
                    disc.setStroke(Color.web(CELL_BG_STROKE));
                } else if (v == 1) {
                    disc.setFill(Color.web("#ffffff"));
                    disc.setStroke(Color.web(ACCENT));
                } else {
                    disc.setFill(Color.web("#0a0a12"));
                    disc.setStroke(Color.web(DISC_BLACK_STROKE));
                }
            }
        }
    }

    private String playerName(int p) {
        return p == 1 ? "BEYAZ" : "SİYAH";
    }

    private boolean isBoardFull() {
        for (int r = 0; r < 6; r++) for (int c = 0; c < 6; c++) if (board[r][c] == 0) return false;
        return true;
    }

    /**
     * return:
     *  1 white wins
     *  2 black wins
     *  0 none
     * -1 both have five (draw)
     */
    private int checkWinner() {
        boolean w = hasFive(1);
        boolean b = hasFive(2);
        if (w && b) return -1;
        if (w) return 1;
        if (b) return 2;
        return 0;
    }

    private boolean hasFive(int p) {
        for (int r = 0; r < 6; r++) for (int c = 0; c <= 1; c++)
            if (board[r][c] == p && board[r][c+1] == p && board[r][c+2] == p && board[r][c+3] == p && board[r][c+4] == p) return true;

        for (int c = 0; c < 6; c++) for (int r = 0; r <= 1; r++)
            if (board[r][c] == p && board[r+1][c] == p && board[r+2][c] == p && board[r+3][c] == p && board[r+4][c] == p) return true;

        for (int r = 0; r <= 1; r++) for (int c = 0; c <= 1; c++)
            if (board[r][c] == p && board[r+1][c+1] == p && board[r+2][c+2] == p && board[r+3][c+3] == p && board[r+4][c+4] == p) return true;

        for (int r = 0; r <= 1; r++) for (int c = 4; c < 6; c++)
            if (board[r][c] == p && board[r+1][c-1] == p && board[r+2][c-2] == p && board[r+3][c-3] == p && board[r+4][c-4] == p) return true;

        return false;
    }
}
