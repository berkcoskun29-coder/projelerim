package stspiremini;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;

public class GameUI extends StackPane {

    // Basit state
    private int playerHP = 60;
    private int playerBlock = 0;
    private int energy = 3;

    private int enemyHP = 55;

    // UI
    private final Pane world = new Pane();
    private final HBox handBar = new HBox(16);
    private final Label hud = new Label();

    private final StackPane dropZone = new StackPane();
    private final StackPane enemyNode = new StackPane();

    // Drag kartı çözmek için
    private final Map<Integer, CardView> idToCardView = new HashMap<>();

    public GameUI() {
        setStyle("-fx-background-color: radial-gradient(radius 120%, #1b1f2a, #0b0d12);");

        // world: oyun alanı
        world.setPrefSize(1100, 650);

        // Üst HUD
        hud.setStyle("""
            -fx-font-size: 16px;
            -fx-font-weight: 800;
            -fx-text-fill: white;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 12, 0.25, 0, 2);
        """);
        hud.setLayoutX(20);
        hud.setLayoutY(16);

        // Düşman (basit bir daire + panel)
        enemyNode.setPrefSize(220, 220);
        enemyNode.setLayoutX(760);
        enemyNode.setLayoutY(160);
        enemyNode.setAlignment(Pos.CENTER);
        enemyNode.setEffect(new DropShadow(30, Color.rgb(0,0,0,0.6)));

        Circle body = new Circle(85);
        body.setFill(Color.rgb(255, 80, 80, 0.9));
        body.setStroke(Color.rgb(255,255,255,0.35));
        body.setStrokeWidth(2);

        Label enemyLabel = new Label();
        enemyLabel.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: 900;
            -fx-text-fill: white;
        """);

        enemyNode.getChildren().addAll(body, enemyLabel);

        // Drop zone (kart bırakma alanı)
        dropZone.setPrefSize(420, 170);
        dropZone.setLayoutX(320);
        dropZone.setLayoutY(250);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setStyle("""
            -fx-background-radius: 22px;
            -fx-border-radius: 22px;
            -fx-border-color: rgba(255,255,255,0.25);
            -fx-border-width: 2;
            -fx-background-color: rgba(255,255,255,0.06);
        """);

        Label dzText = new Label("PLAY AREA\n(Drag a card here)");
        dzText.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: 900;
            -fx-text-fill: rgba(255,255,255,0.8);
            -fx-alignment: center;
        """);
        dzText.setAlignment(Pos.CENTER);
        dropZone.getChildren().add(dzText);

        // Hand bar
        handBar.setPadding(new Insets(0, 0, 22, 0));
        handBar.setAlignment(Pos.BOTTOM_CENTER);
        handBar.setPrefHeight(240);

        VBox bottom = new VBox(10, handBar);
        bottom.setAlignment(Pos.BOTTOM_CENTER);
        bottom.setPadding(new Insets(0, 0, 0, 0));
        bottom.setPickOnBounds(false);

        StackPane.setAlignment(bottom, Pos.BOTTOM_CENTER);

        world.getChildren().addAll(hud, enemyNode, dropZone);
        getChildren().addAll(world, bottom);

        // Drag-over / Drop handling
        dropZone.setOnDragOver(e -> {
            if (e.getDragboard().hasString() && e.getDragboard().getString().startsWith("CARD:")) {
                e.acceptTransferModes(TransferMode.MOVE);
                dropZone.setStyle("""
                    -fx-background-radius: 22px;
                    -fx-border-radius: 22px;
                    -fx-border-color: rgba(255,255,255,0.6);
                    -fx-border-width: 2.5;
                    -fx-background-color: rgba(255,255,255,0.10);
                """);
            }
            e.consume();
        });

        dropZone.setOnDragExited(e -> {
            resetDropZoneStyle();
            e.consume();
        });

        dropZone.setOnDragDropped(e -> {
            boolean ok = false;
            String s = e.getDragboard().getString();
            if (s != null && s.startsWith("CARD:")) {
                int id = Integer.parseInt(s.substring("CARD:".length()));
                CardView cv = idToCardView.get(id);
                if (cv != null) {
                    ok = tryPlayCard(cv);
                }
            }
            e.setDropCompleted(ok);
            resetDropZoneStyle();
            e.consume();
        });

        // başlangıç: deck/hand
        drawStartingHand();
        updateHUD(enemyLabel);
    }

    private void resetDropZoneStyle() {
        dropZone.setStyle("""
            -fx-background-radius: 22px;
            -fx-border-radius: 22px;
            -fx-border-color: rgba(255,255,255,0.25);
            -fx-border-width: 2;
            -fx-background-color: rgba(255,255,255,0.06);
        """);
    }

    private void drawStartingHand() {
        handBar.getChildren().clear();
        idToCardView.clear();

        // basit demo eli
        List<Card> cards = List.of(
                Card.strike(), Card.strike(),
                Card.defend(),
                Card.bash(),
                new Card("Zap", CardType.SKILL, 0, 3, 0),
                new Card("Guard", CardType.SKILL, 1, 0, 8)
        );

        for (Card c : cards) {
            CardView cv = new CardView(c);
            int id = System.identityHashCode(cv);
            idToCardView.put(id, cv);
            handBar.getChildren().add(cv);
        }
    }

    private boolean tryPlayCard(CardView cv) {
        Card c = cv.card;

        if (energy < c.cost) {
            // enerji yetmiyorsa küçük uyarı efekti
            FX.punch(dropZone);
            return false;
        }

        // enerji düş
        energy -= c.cost;

        // kart etkileri
        if (c.damage > 0) {
            enemyHP = Math.max(0, enemyHP - c.damage);
            FX.popDamage(world, enemyNode.getLayoutX() + 95, enemyNode.getLayoutY() + 60, c.damage);
            FX.punch(enemyNode);
            FX.screenShake(world);
        }
        if (c.block > 0) {
            playerBlock += c.block;
            FX.punch(hud);
        }

        // oynanan kartı elden kaldır
        handBar.getChildren().remove(cv);
        idToCardView.values().remove(cv);

        // düşman öldüyse mini durum
        if (enemyHP <= 0) {
            Label win = new Label("ENEMY DEFEATED!");
            win.setStyle("""
                -fx-font-size: 40px;
                -fx-font-weight: 1000;
                -fx-text-fill: white;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 20, 0.25, 0, 3);
            """);
            win.setLayoutX(340);
            win.setLayoutY(80);
            world.getChildren().add(win);
        }

        updateHUD((Label) enemyNode.getChildren().get(1));
        return true;
    }

    private void updateHUD(Label enemyLabel) {
        enemyLabel.setText("Enemy\nHP: " + enemyHP);

        hud.setText("Player HP: " + playerHP
                + "   Block: " + playerBlock
                + "   Energy: " + energy
                + "   |   Cards in hand: " + handBar.getChildren().size());
    }
}
