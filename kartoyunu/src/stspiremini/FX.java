package stspiremini;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class FX {

    public static void popDamage(Pane world, double x, double y, int amount) {
        Label dmg = new Label("-" + amount);
        dmg.setStyle("""
            -fx-font-size: 28px;
            -fx-font-weight: 900;
            -fx-text-fill: white;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.65), 12, 0.25, 0, 3);
        """);
        dmg.setTextFill(Color.WHITE);
        dmg.setLayoutX(x);
        dmg.setLayoutY(y);

        world.getChildren().add(dmg);

        TranslateTransition tt = new TranslateTransition(Duration.millis(650), dmg);
        tt.setFromY(0);
        tt.setToY(-65);

        FadeTransition ft = new FadeTransition(Duration.millis(650), dmg);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(200), dmg);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.15);  st.setToY(1.15);
        st.setAutoReverse(true);
        st.setCycleCount(2);

        ParallelTransition pt = new ParallelTransition(tt, ft, st);
        pt.setOnFinished(e -> world.getChildren().remove(dmg));
        pt.play();
    }

    public static void punch(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(90), node);
        st.setFromX(1.0); st.setFromY(1.0);
        st.setToX(1.06);  st.setToY(1.06);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    public static void screenShake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(70), node);
        tt.setFromX(0);
        tt.setToX(8);
        tt.setAutoReverse(true);
        tt.setCycleCount(6);
        tt.play();
    }
}
