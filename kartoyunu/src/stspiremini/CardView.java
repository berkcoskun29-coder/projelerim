package stspiremini;

import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class CardView extends StackPane {
    public final Card card;

    private final ScaleTransition hoverIn;
    private final ScaleTransition hoverOut;

    public CardView(Card card) {
        this.card = card;

        setPrefSize(140, 200);
        setMaxSize(140, 200);
        setMinSize(140, 200);
        setCursor(Cursor.HAND);

        // Kart tasarım
        String bg = switch (card.type) {
            case ATTACK -> "linear-gradient(to bottom right, #ff5f6d, #ffc371)";
            case SKILL  -> "linear-gradient(to bottom right, #36d1dc, #5b86e5)";
            case POWER  -> "linear-gradient(to bottom right, #b24592, #f15f79)";
        };

        setStyle("""
            -fx-background-radius: 18px;
            -fx-border-radius: 18px;
            -fx-border-color: rgba(255,255,255,0.35);
            -fx-border-width: 1.2;
            -fx-background-color: %s;
        """.formatted(bg));

        setEffect(new DropShadow(20, Color.rgb(0,0,0,0.45)));

        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        content.setAlignment(Pos.TOP_LEFT);

        Label cost = new Label(String.valueOf(card.cost));
        cost.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: 900;
            -fx-text-fill: white;
            -fx-background-color: rgba(0,0,0,0.35);
            -fx-padding: 4 10 4 10;
            -fx-background-radius: 999px;
        """);

        Label name = new Label(card.name);
        name.setStyle("""
            -fx-font-size: 16px;
            -fx-font-weight: 900;
            -fx-text-fill: rgba(0,0,0,0.85);
        """);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label desc = new Label(makeDesc(card));
        desc.setWrapText(true);
        desc.setStyle("""
            -fx-font-size: 13px;
            -fx-font-weight: 700;
            -fx-text-fill: rgba(0,0,0,0.75);
            -fx-background-color: rgba(255,255,255,0.35);
            -fx-padding: 8;
            -fx-background-radius: 12px;
        """);

        content.getChildren().addAll(cost, name, spacer, desc);
        getChildren().add(content);

        // Hover animasyonları
        hoverIn = new ScaleTransition(Duration.millis(120), this);
        hoverIn.setToX(1.08);
        hoverIn.setToY(1.08);

        hoverOut = new ScaleTransition(Duration.millis(120), this);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);

        setOnMouseEntered(e -> { hoverOut.stop(); hoverIn.playFromStart(); toFront(); });
        setOnMouseExited(e -> { hoverIn.stop(); hoverOut.playFromStart(); });

        // Drag & Drop
        setOnDragDetected(e -> {
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();

            // Kartı kimlikle taşıyalım (GameUI içinde map’leyeceğiz)
            cc.putString("CARD:" + System.identityHashCode(this));
            db.setContent(cc);

            setOpacity(0.55);
            e.consume();
        });

        setOnDragDone(e -> {
            setOpacity(1.0);
            e.consume();
        });
    }

    private String makeDesc(Card c) {
        StringBuilder sb = new StringBuilder();
        if (c.damage > 0) sb.append("Deal ").append(c.damage).append(" damage.\n");
        if (c.block > 0) sb.append("Gain ").append(c.block).append(" block.\n");
        sb.append("(").append(c.type).append(")");
        return sb.toString();
    }
}
