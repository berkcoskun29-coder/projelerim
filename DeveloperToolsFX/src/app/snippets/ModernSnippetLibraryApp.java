package app.snippets;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ModernSnippetLibraryApp extends Application {

    private final ObservableList<Snippet> javaSnippets = FXCollections.observableArrayList();
    private final ObservableList<Snippet> sqlSnippets = FXCollections.observableArrayList();
    private final ObservableList<Snippet> htmlSnippets = FXCollections.observableArrayList();

    private TextArea codeArea;
    private Label selectedTitle;
    private Label selectedMeta;

    @Override
    public void start(Stage stage) {
        loadData();

        VBox root = new VBox(18);
        root.setPadding(new Insets(22));
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(
                        0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#111827")),
                        new Stop(1, Color.web("#1e293b"))
                ),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        VBox header = buildHeader();
        HBox content = buildContent();

        root.getChildren().addAll(header, content);

        Scene scene = new Scene(root, 1150, 700);
        stage.setTitle("Modern Snippet Library");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildHeader() {
        Label badge = new Label("DEVELOPER TOOL");
        badge.setTextFill(Color.web("#c4b5fd"));
        badge.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        badge.setStyle(
                "-fx-background-color: rgba(139,92,246,0.16);" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 6 12 6 12;"
        );

        Label title = new Label("Snippet Kütüphanesi");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 30));

        Label subtitle = new Label("Java, SQL ve HTML kod parçalarını kategorize et, görüntüle ve tek tıkla panoya kopyala.");
        subtitle.setTextFill(Color.web("#cbd5e1"));
        subtitle.setFont(Font.font("Arial", 15));

        return new VBox(10, badge, title, subtitle);
    }

    private HBox buildContent() {
        VBox sidebar = buildSidebar();
        VBox preview = buildPreview();

        HBox box = new HBox(20, sidebar, preview);
        HBox.setHgrow(preview, Priority.ALWAYS);
        return box;
    }

    private VBox buildSidebar() {
        Label title = new Label("Kategoriler");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("Arial", FontWeight.BOLD, 19));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().add(new Tab("Java", createSnippetList(javaSnippets, "Java")));
        tabPane.getTabs().add(new Tab("SQL", createSnippetList(sqlSnippets, "SQL")));
        tabPane.getTabs().add(new Tab("HTML", createSnippetList(htmlSnippets, "HTML")));

        VBox box = new VBox(14, title, tabPane);
        box.setPrefWidth(320);
        styleCard(box);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        return box;
    }

    private VBox buildPreview() {
        selectedTitle = new Label("Snippet seçiniz");
        selectedTitle.setTextFill(Color.WHITE);
        selectedTitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));

        selectedMeta = new Label("Seçili dil: -");
        selectedMeta.setTextFill(Color.web("#a5b4fc"));
        selectedMeta.setFont(Font.font("Arial", 13));

        codeArea = new TextArea();
        codeArea.setEditable(false);
        codeArea.setWrapText(false);
        codeArea.setFont(Font.font("Consolas", 15));
        codeArea.setStyle(
                "-fx-control-inner-background: #0f172a;" +
                "-fx-text-fill: #e2e8f0;" +
                "-fx-highlight-fill: #312e81;" +
                "-fx-highlight-text-fill: white;" +
                "-fx-background-radius: 18;"
        );

        Button copyBtn = new Button("Copy");
        stylePrimaryButton(copyBtn);
        copyBtn.setOnAction(e -> copySnippet());

        Button clearBtn = new Button("Clear");
        styleSecondaryButton(clearBtn);
        clearBtn.setOnAction(e -> {
            selectedTitle.setText("Snippet seçiniz");
            selectedMeta.setText("Seçili dil: -");
            codeArea.clear();
        });

        HBox actions = new HBox(10, copyBtn, clearBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(12, selectedTitle, selectedMeta, codeArea, actions);
        styleCard(box);
        VBox.setVgrow(codeArea, Priority.ALWAYS);
        return box;
    }

    private ListView<Snippet> createSnippetList(ObservableList<Snippet> data, String language) {
        ListView<Snippet> listView = new ListView<>(data);

        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Snippet item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.getTitle());
                    setStyle(
                            "-fx-background-color: #1e293b;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-background-radius: 12;" +
                            "-fx-border-radius: 12;" +
                            "-fx-padding: 10;"
                    );
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectedTitle.setText(newValue.getTitle());
                selectedMeta.setText("Seçili dil: " + language);
                codeArea.setText(newValue.getCode());
            }
        });

        return listView;
    }

    private void copySnippet() {
        String text = codeArea.getText();
        if (text == null || text.isEmpty()) {
            showInfo("Uyarı", "Önce bir snippet seçmelisin.");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);

        showInfo("Başarılı", "Snippet panoya kopyalandı.");
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void styleCard(Region region) {
        region.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-background-radius: 24;" +
                "-fx-border-color: rgba(255,255,255,0.08);" +
                "-fx-border-radius: 24;" +
                "-fx-padding: 20;"
        );
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed);" +
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

    private void loadData() {
        javaSnippets.add(new Snippet(
                "For Loop",
                "for (int i = 0; i < 10; i++) {\n    System.out.println(i);\n}"
        ));

        javaSnippets.add(new Snippet(
                "ArrayList",
                "ArrayList<String> list = new ArrayList<>();\nlist.add(\"Hello\");"
        ));

        javaSnippets.add(new Snippet(
                "Scanner Input",
                "Scanner scanner = new Scanner(System.in);\nString name = scanner.nextLine();"
        ));

        sqlSnippets.add(new Snippet(
                "Select All",
                "SELECT * FROM users;"
        ));

        sqlSnippets.add(new Snippet(
                "Where Condition",
                "SELECT * FROM users WHERE age > 18;"
        ));

        sqlSnippets.add(new Snippet(
                "Create Table",
                "CREATE TABLE students (\n" +
                "    id INT PRIMARY KEY,\n" +
                "    name VARCHAR(100),\n" +
                "    surname VARCHAR(100)\n" +
                ");"
        ));

        htmlSnippets.add(new Snippet(
                "HTML Boilerplate",
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Hello World</h1>\n" +
                "</body>\n" +
                "</html>"
        ));

        htmlSnippets.add(new Snippet(
                "Button",
                "<button>Click Me</button>"
        ));

        htmlSnippets.add(new Snippet(
                "Anchor Link",
                "<a href=\"https://example.com\">Visit</a>"
        ));
    }

    public static void main(String[] args) {
        launch(args);
    }
}