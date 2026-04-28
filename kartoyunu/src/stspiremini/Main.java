package stspiremini;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override public void start(Stage stage) {
        GameUI root = new GameUI();

        Scene scene = new Scene(root, 1100, 650);
        stage.setTitle("Slay the Spire Mini (JavaFX) - Drag & Drop Cards");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
