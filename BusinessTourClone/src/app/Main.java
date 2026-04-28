package app;

import engine.GameEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Board;
import ui.GameView;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Board board = Board.createDefaultBoard();
        GameEngine engine = new GameEngine(board);

        GameView view = new GameView(engine);

        Scene scene = new Scene(view.getRoot(), 1100, 720);
        stage.setTitle("Business Tour - JavaFX (Turn Based)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
