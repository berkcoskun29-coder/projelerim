package ui;

import engine.GameEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.Player;
import model.Tile;

import java.util.ArrayList;
import java.util.List;

public final class GameView {
    private final GameEngine engine;

    private final BorderPane root = new BorderPane();

    private final GridPane boardGrid = new GridPane();
    private final TextArea log = new TextArea();

    private final Label currentPlayerLabel = new Label();
    private final Label currentMoneyLabel = new Label();
    private final Label currentPosLabel = new Label();

    private final Button rollBtn = new Button("Zar At");
    private final Button endTurnBtn = new Button("Turu Bitir");
    private final Button buyBtn = new Button("Satın Al");

    // tile ui referansları (40 tile)
    private final List<StackPane> tilePanes = new ArrayList<>();

    // oyuncu piyonları (Circle)
    private final List<Circle> tokens = new ArrayList<>();

    public GameView(GameEngine engine) {
        this.engine = engine;
        buildLayout();
        refreshAll();
        
    }

    public Parent getRoot() {
        return root;
    }

    private void buildLayout() {
        root.setPadding(new Insets(10));

        // Board (sol)
        boardGrid.setHgap(2);
        boardGrid.setVgap(2);
        boardGrid.setPadding(new Insets(5));
        root.setCenter(boardGrid);

        // Sağ panel
        VBox right = new VBox(10);
        right.setPadding(new Insets(10));
        right.setPrefWidth(320);

        Label title = new Label("Oyuncu Paneli");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Oyuncu sayısı seçimi
        HBox playerCountBox = new HBox(8);
        playerCountBox.setAlignment(Pos.CENTER_LEFT);
        Label pcLbl = new Label("Oyuncu sayısı:");
        ComboBox<Integer> playerCount = new ComboBox<>();
        playerCount.getItems().addAll(2, 3, 4);
        playerCount.setValue(engine.players().size());
        playerCount.setOnAction(e -> {
            engine.setPlayerCount(playerCount.getValue());
            buildTokens();
            refreshAll();
            appendLog("Oyuncu sayısı: " + playerCount.getValue());
        });
        playerCountBox.getChildren().addAll(pcLbl, playerCount);

        VBox infoBox = new VBox(6);
        infoBox.getChildren().addAll(currentPlayerLabel, currentMoneyLabel, currentPosLabel);

        right.getChildren().addAll(title, playerCountBox, infoBox);
        root.setRight(right);

        // Alt bar (butonlar)
        HBox bottom = new HBox(10);
        bottom.setPadding(new Insets(10));
        bottom.setAlignment(Pos.CENTER_LEFT);

        rollBtn.setOnAction(e -> {
            String msg = engine.rollAndMove();
            appendLog(msg);

            Player p = engine.currentPlayer();
            appendLog(engine.describeTileEffect(p));

            refreshAll();
        });

        endTurnBtn.setOnAction(e -> {
            String msg = engine.endTurn();
            appendLog(msg);
            refreshAll();
        });

        buyBtn.setOnAction(e -> {
            String msg = engine.buyCurrentTile();
            appendLog(msg);
            refreshAll();
        });

        bottom.getChildren().addAll(rollBtn, buyBtn, endTurnBtn);

        root.setBottom(bottom);

        // Log (üst)
        log.setEditable(false);
        log.setPrefRowCount(6);
        root.setTop(log);

        // Board'u çiz
        buildBoardGrid();
        buildTokens();
    }

    // 11x11 çerçeveye 40 tile yerleştiriyoruz (Monopoly mantığı)
    private void buildBoardGrid() {
        boardGrid.getChildren().clear();
        tilePanes.clear();

        // 11x11; tile yerleşimi haritası
        // Kenar boyunca 40 tile:
        // alt sıra: 0..10, sol sütun: 11..19, üst sıra: 20..30, sağ sütun: 31..39
        // (Bu dizilim senin board index'inle birebir eşleşsin diye biz de böyle mapliyoruz.)

        // alt sıra (row=10, col=10..0) -> 0..10
        int idx = 0;
        for (int col = 10; col >= 0; col--) {
            StackPane tile = createTilePane(idx);
            boardGrid.add(tile, col, 10);
            idx++;
        }

        // sol sütun (col=0, row=9..1) -> 11..19
        for (int row = 9; row >= 1; row--) {
            StackPane tile = createTilePane(idx);
            boardGrid.add(tile, 0, row);
            idx++;
        }

        // üst sıra (row=0, col=0..10) -> 20..30
        for (int col = 0; col <= 10; col++) {
            StackPane tile = createTilePane(idx);
            boardGrid.add(tile, col, 0);
            idx++;
        }

        // sağ sütun (col=10, row=1..9) -> 31..39
        for (int row = 1; row <= 9; row++) {
            StackPane tile = createTilePane(idx);
            boardGrid.add(tile, 10, row);
            idx++;
        }

        // ortayı boş bırakıyoruz: istersen buraya görsel/afiş/market koyarsın
        // Grid hücre boyutları
        for (StackPane pane : tilePanes) {
            pane.setPrefSize(85, 55);
        }
    }

    private StackPane createTilePane(int tileIndex) {
        Tile tile = engine.board().get(tileIndex);

        Label name = new Label(tile.name());
        name.setWrapText(true);
        name.setMaxWidth(80);
        name.setStyle("-fx-font-size: 10px; -fx-text-alignment: center;");

        StackPane pane = new StackPane(name);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(4));
        pane.setStyle(tileStyle(tileIndex));

        tilePanes.add(pane);
        return pane;
    }

    private String tileStyle(int tileIndex) {
        Tile tile = engine.board().get(tileIndex);
        String base = "-fx-border-color: #444; -fx-border-width: 1; -fx-background-color: ";
        return switch (tile.type()) {
            case START -> base + "#d9fdd3;";
            case TAX -> base + "#ffd6d6;";
            case CHANCE -> base + "#d6e8ff;";
            case AIRPORT -> base + "#fff1cc;";
            case RESORT -> base + "#e7d6ff;";
            case ISLAND -> base + "#e0e0e0;";
            default -> base + "#ffffff;";
        };
    }

    private void buildTokens() {
        tokens.clear();
        // her oyuncu için bir token
        for (int i = 0; i < engine.players().size(); i++) {
            Circle c = new Circle(6);
            tokens.add(c);
        }
        placeTokens();
    }

    private void placeTokens() {
        // önce bütün tile'lardan tokenları kaldır
        for (StackPane p : tilePanes) {
            // label dışındakileri temizlemek için:
            // StackPane child[0] label, sonrakiler token
            if (p.getChildren().size() > 1) {
                p.getChildren().remove(1, p.getChildren().size());
            }
        }

        // tokenları ilgili tile'a koy
        for (int i = 0; i < engine.players().size(); i++) {
            Player pl = engine.players().get(i);
            int pos = pl.position();
            StackPane tilePane = tilePanes.get(pos);

            // aynı tile'da 2-4 token üst üste binmesin diye küçük offset:
            Circle token = tokens.get(i);
            token.setTranslateX(-20 + (i * 12));
            token.setTranslateY(14);

            tilePane.getChildren().add(token);
        }
    }

    private void refreshAll() {
        Player p = engine.currentPlayer();
        currentPlayerLabel.setText("Sıra: " + p.name());
        currentMoneyLabel.setText("Para: " + formatMoney(p.money()));
        currentPosLabel.setText("Konum: " + p.position() + " → " + engine.board().get(p.position()).name());

        // buton durumu
        rollBtn.setDisable(engine.phase() != GameEngine.Phase.WAIT_ROLL);
        endTurnBtn.setDisable(engine.phase() != GameEngine.Phase.WAIT_END_TURN);
     // Satın Al sadece: WAIT_END_TURN aşamasında ve tile sahipsiz property ise aktif
        var tile = engine.board().get(p.position());
        boolean canBuy = (engine.phase() == GameEngine.Phase.WAIT_END_TURN)
                && (tile instanceof model.PropertyTile prop)
                && (!prop.isOwned())
                && (p.money() >= prop.price());

        buyBtn.setDisable(!canBuy);


        placeTokens();
    }

    private void appendLog(String msg) {
        log.appendText(msg + "\n");
    }

    private String formatMoney(int v) {
        return String.format("%,d", v).replace(',', '.');
    }
}
