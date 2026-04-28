package asciiart;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AsciiArtApp extends Application {

    private final ImageView originalImageView = new ImageView();
    private final TextArea asciiArea = new TextArea();

    private final Slider widthSlider = new Slider(40, 250, 120);
    private final Label widthValueLabel = new Label("120");

    private final Slider fontSlider = new Slider(5, 18, 7);
    private final Label fontValueLabel = new Label("7");

    private final TextField charField = new TextField("@%#*+=-:. ");
    private final ComboBox<String> presetBox = new ComboBox<>();

    private final CheckBox invertCheckBox = new CheckBox("Invert");
    private final CheckBox grayscaleCheckBox = new CheckBox("Black & White");
    private final CheckBox colorPreviewCheckBox = new CheckBox("Colored ASCII Preview");

    private final Label statusLabel = new Label("Ready");
    private final Label imageInfoLabel = new Label("No image loaded");

    private Image loadedImage;
    private String currentAscii = "";
    private boolean darkMode = true;

    @Override
    public void start(Stage stage) {
        stage.setTitle("ASCII Art Converter");

        BorderPane root = new BorderPane();
        VBox topPanel = buildTopPanel(stage);
        SplitPane centerPane = buildCenterPane();

        root.setTop(topPanel);
        root.setCenter(centerPane);
        root.setBottom(buildBottomBar());

        BorderPane.setMargin(centerPane, new Insets(10));
        BorderPane.setMargin(topPanel, new Insets(10, 10, 0, 10));
        BorderPane.setMargin(root.getBottom(), new Insets(0, 10, 10, 10));

        setupDragAndDrop(root);
        applyTheme(root);

        Scene scene = new Scene(root, 1320, 760);
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildTopPanel(Stage stage) {
        Label title = new Label("ASCII Art Converter");
        title.setStyle("""
                -fx-font-size: 24px;
                -fx-font-weight: bold;
                """);

        Label subtitle = new Label("Convert images into ASCII art with JavaFX");
        subtitle.setStyle("-fx-font-size: 13px;");

        Button loadButton = createStyledButton("Load Image");
        Button convertButton = createStyledButton("Generate ASCII");
        Button copyButton = createStyledButton("Copy");
        Button saveTxtButton = createStyledButton("Save TXT");
        Button exportPngButton = createStyledButton("Export PNG");
        Button exportImageButton = createStyledButton("Export JPG/PNG");
        Button clearButton = createStyledButton("Clear");
        Button themeButton = createStyledButton("Dark / Light");

        loadButton.setOnAction(e -> loadImage(stage));
        convertButton.setOnAction(e -> generateAscii());
        copyButton.setOnAction(e -> copyAsciiToClipboard());
        saveTxtButton.setOnAction(e -> saveAsciiToFile(stage));
        exportPngButton.setOnAction(e -> exportAsciiAsPng(stage));
        exportImageButton.setOnAction(e -> exportAsciiAsImage(stage));
        clearButton.setOnAction(e -> clearAll());
        themeButton.setOnAction(e -> {
            darkMode = !darkMode;
            BorderPane root = (BorderPane) asciiArea.getScene().getRoot();
            applyTheme(root);
        });

        widthSlider.setBlockIncrement(1);
        widthSlider.setShowTickLabels(true);
        widthSlider.setShowTickMarks(true);
        widthSlider.valueProperty().addListener((obs, oldV, newV) -> widthValueLabel.setText(String.valueOf(newV.intValue())));

        fontSlider.setBlockIncrement(1);
        fontSlider.setShowTickLabels(true);
        fontSlider.setShowTickMarks(true);
        fontSlider.valueProperty().addListener((obs, oldV, newV) -> {
            int value = newV.intValue();
            fontValueLabel.setText(String.valueOf(value));
            asciiArea.setFont(Font.font("Consolas", value));
        });

        presetBox.getItems().addAll(
                "Simple: @%#*+=-:. ",
                "Classic: @#S%?*+;:,. ",
                "Detailed: $@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^'. ",
                "Soft: #&@%*o!;:. ",
                "Minimal: @#*. "
        );
        presetBox.setValue("Simple: @%#*+=-:. ");
        presetBox.setOnAction(e -> applyPreset());

        styleTextField(charField, "Character Set");
        charField.setPrefWidth(300);

        setWhiteText(widthValueLabel, fontValueLabel, imageInfoLabel, statusLabel);
        setWhiteText(invertCheckBox, grayscaleCheckBox, colorPreviewCheckBox);

        Label widthLabel = new Label("Width");
        Label fontLabel = new Label("Font");
        Label presetLabel = new Label("Preset");
        Label charsLabel = new Label("Chars");

        setWhiteText(widthLabel, fontLabel, presetLabel, charsLabel);
        widthLabel.setStyle(widthLabel.getStyle() + "-fx-font-weight: bold;");
        fontLabel.setStyle(fontLabel.getStyle() + "-fx-font-weight: bold;");
        presetLabel.setStyle(presetLabel.getStyle() + "-fx-font-weight: bold;");
        charsLabel.setStyle(charsLabel.getStyle() + "-fx-font-weight: bold;");

        HBox row1 = new HBox(10, loadButton, convertButton, copyButton, saveTxtButton, exportPngButton, exportImageButton, clearButton, themeButton);
        row1.setAlignment(Pos.CENTER_LEFT);

        HBox row2 = new HBox(
                12,
                widthLabel, widthSlider, widthValueLabel,
                new Separator(),
                fontLabel, fontSlider, fontValueLabel,
                new Separator(),
                presetLabel, presetBox,
                new Separator(),
                charsLabel, charField,
                new Separator(),
                invertCheckBox,
                grayscaleCheckBox,
                colorPreviewCheckBox
        );
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox controlCard = new VBox(12, row1, row2, imageInfoLabel);
        controlCard.setPadding(new Insets(14));
        controlCard.setStyle(cardStyle());

        VBox topBox = new VBox(8, title, subtitle, controlCard);
        topBox.setPadding(new Insets(14));
        return topBox;
    }

    private SplitPane buildCenterPane() {
        VBox leftCard = new VBox(10);
        leftCard.setPadding(new Insets(15));
        leftCard.setStyle(cardStyle());

        Label leftTitle = new Label("Original Image");
        leftTitle.setStyle(sectionTitleStyle());

        originalImageView.setPreserveRatio(true);
        originalImageView.setFitWidth(520);
        originalImageView.setFitHeight(540);
        originalImageView.setSmooth(true);

        Label dragInfo = new Label("You can also drag and drop an image here");
        dragInfo.setStyle("-fx-font-size: 12px;");

        StackPane imageHolder = new StackPane(originalImageView);
        imageHolder.setAlignment(Pos.CENTER);
        imageHolder.setPrefHeight(570);
        imageHolder.setStyle(innerPanelStyle());

        leftCard.getChildren().addAll(leftTitle, dragInfo, imageHolder);

        VBox rightCard = new VBox(10);
        rightCard.setPadding(new Insets(15));
        rightCard.setStyle(cardStyle());

        Label rightTitle = new Label("ASCII Output");
        rightTitle.setStyle(sectionTitleStyle());

        asciiArea.setWrapText(false);
        asciiArea.setEditable(false);
        asciiArea.setFont(Font.font("Consolas", 7));

        rightCard.getChildren().addAll(rightTitle, asciiArea);
        VBox.setVgrow(asciiArea, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(leftCard, rightCard);
        splitPane.setDividerPositions(0.42);
        return splitPane;
    }

    private HBox buildBottomBar() {
        HBox bottom = new HBox(statusLabel);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 16, 10, 16));
        bottom.setStyle(cardStyle());
        return bottom;
    }

    private void loadImage(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );

        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadImageFromFile(file);
        }
    }

    private void loadImageFromFile(File file) {
        try {
            loadedImage = new Image(file.toURI().toString());
            originalImageView.setImage(loadedImage);

            imageInfoLabel.setText("Loaded: " + file.getName() + " | " + (int) loadedImage.getWidth() + " x " + (int) loadedImage.getHeight());
            statusLabel.setText("Image loaded successfully");
            generateAscii();
        } catch (Exception ex) {
            showError("Image could not be loaded.");
        }
    }

    private void setupDragAndDrop(BorderPane root) {
        root.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        root.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                String name = file.getName().toLowerCase();

                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                        || name.endsWith(".bmp") || name.endsWith(".gif")) {
                    loadImageFromFile(file);
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void applyPreset() {
        String selected = presetBox.getValue();
        if (selected == null) return;

        if (selected.startsWith("Simple")) {
            charField.setText("@%#*+=-:. ");
        } else if (selected.startsWith("Classic")) {
            charField.setText("@#S%?*+;:,. ");
        } else if (selected.startsWith("Detailed")) {
            charField.setText("$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^'. ");
        } else if (selected.startsWith("Soft")) {
            charField.setText("#&@%*o!;:. ");
        } else if (selected.startsWith("Minimal")) {
            charField.setText("@#*. ");
        }

        if (loadedImage != null) {
            generateAscii();
        }
    }

    private void generateAscii() {
        if (loadedImage == null) {
            showError("Please load an image first.");
            return;
        }

        String chars = charField.getText();
        if (chars == null || chars.isBlank()) {
            showError("Character set cannot be empty.");
            return;
        }

        int targetWidth = (int) widthSlider.getValue();
        currentAscii = convertImageToAscii(loadedImage, targetWidth, chars, invertCheckBox.isSelected(), grayscaleCheckBox.isSelected());

        asciiArea.setText(currentAscii);
        asciiArea.setFont(Font.font("Consolas", (int) fontSlider.getValue()));

        if (colorPreviewCheckBox.isSelected()) {
            if (darkMode) {
                asciiArea.setStyle("""
                        -fx-control-inner-background: #020617;
                        -fx-text-fill: #60a5fa;
                        -fx-highlight-fill: #334155;
                        -fx-highlight-text-fill: white;
                        -fx-border-color: rgba(255,255,255,0.10);
                        -fx-border-radius: 14;
                        """);
            } else {
                asciiArea.setStyle("""
                        -fx-control-inner-background: white;
                        -fx-text-fill: #2563eb;
                        -fx-highlight-fill: #bfdbfe;
                        -fx-highlight-text-fill: black;
                        -fx-border-color: #d1d5db;
                        -fx-border-radius: 14;
                        """);
            }
        } else {
            if (darkMode) {
                asciiArea.setStyle("""
                        -fx-control-inner-background: #020617;
                        -fx-text-fill: #e2e8f0;
                        -fx-highlight-fill: #334155;
                        -fx-highlight-text-fill: white;
                        -fx-border-color: rgba(255,255,255,0.10);
                        -fx-border-radius: 14;
                        """);
            } else {
                asciiArea.setStyle("""
                        -fx-control-inner-background: white;
                        -fx-text-fill: #111827;
                        -fx-highlight-fill: #e5e7eb;
                        -fx-highlight-text-fill: black;
                        -fx-border-color: #d1d5db;
                        -fx-border-radius: 14;
                        """);
            }
        }

        statusLabel.setText("ASCII generated");
    }

    private String convertImageToAscii(Image image, int targetWidth, String charset, boolean invert, boolean grayscale) {
        PixelReader reader = image.getPixelReader();
        if (reader == null) return "";

        int originalWidth = (int) image.getWidth();
        int originalHeight = (int) image.getHeight();

        double aspectRatio = (double) originalHeight / originalWidth;
        int targetHeight = Math.max(1, (int) (targetWidth * aspectRatio * 0.5));

        WritableImage resized = resizeImage(image, targetWidth, targetHeight);
        PixelReader resizedReader = resized.getPixelReader();

        StringBuilder sb = new StringBuilder();
        String chars = invert ? new StringBuilder(charset).reverse().toString() : charset;
        int maxIndex = chars.length() - 1;

        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                Color color = resizedReader.getColor(x, y);

                if (grayscale) {
                    double gray = (color.getRed() + color.getGreen() + color.getBlue()) / 3.0;
                    color = new Color(gray, gray, gray, color.getOpacity());
                }

                double luminance = 0.2126 * color.getRed()
                        + 0.7152 * color.getGreen()
                        + 0.0722 * color.getBlue();

                int charIndex = (int) Math.round(luminance * maxIndex);
                charIndex = Math.max(0, Math.min(maxIndex, charIndex));

                sb.append(chars.charAt(charIndex));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private WritableImage resizeImage(Image image, int width, int height) {
        ImageView tempView = new ImageView(image);
        tempView.setFitWidth(width);
        tempView.setFitHeight(height);
        tempView.setPreserveRatio(false);
        tempView.setSmooth(true);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        return tempView.snapshot(params, new WritableImage(width, height));
    }

    private void copyAsciiToClipboard() {
        if (currentAscii == null || currentAscii.isBlank()) {
            showError("There is no ASCII text to copy.");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(currentAscii);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("ASCII copied to clipboard");
    }

    private void saveAsciiToFile(Stage stage) {
        if (currentAscii == null || currentAscii.isBlank()) {
            showError("There is no ASCII text to save.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save ASCII");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        chooser.setInitialFileName("ascii-art.txt");

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(currentAscii);
                statusLabel.setText("TXT file saved");
            } catch (IOException e) {
                showError("Text file could not be saved.");
            }
        }
    }

    private void exportAsciiAsPng(Stage stage) {
        exportAsciiAsImageInternal(stage, "png");
    }

    private void exportAsciiAsImage(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export ASCII Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("JPG Image", "*.jpg")
        );
        chooser.setInitialFileName("ascii-art.png");

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            String lower = file.getName().toLowerCase();
            String format = lower.endsWith(".jpg") || lower.endsWith(".jpeg") ? "jpg" : "png";
            exportAsciiNodeToImage(file, format);
        }
    }

    private void exportAsciiAsImageInternal(Stage stage, String format) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export PNG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        chooser.setInitialFileName("ascii-art.png");

        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            exportAsciiNodeToImage(file, format);
        }
    }

    private void exportAsciiNodeToImage(File file, String format) {
        if (currentAscii == null || currentAscii.isBlank()) {
            showError("There is no ASCII output to export.");
            return;
        }

        Label exportLabel = new Label(currentAscii);
        exportLabel.setFont(Font.font("Consolas", Math.max(8, (int) fontSlider.getValue() + 2)));
        exportLabel.setWrapText(false);
        exportLabel.setPadding(new Insets(20));

        if (darkMode) {
            exportLabel.setTextFill(Color.web(colorPreviewCheckBox.isSelected() ? "#60a5fa" : "#e2e8f0"));
            exportLabel.setStyle("-fx-background-color: #020617;");
        } else {
            exportLabel.setTextFill(Color.web(colorPreviewCheckBox.isSelected() ? "#2563eb" : "#111827"));
            exportLabel.setStyle("-fx-background-color: white;");
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(darkMode ? Color.web("#020617") : Color.WHITE);

        WritableImage image = exportLabel.snapshot(params, null);

        try {
            javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(image, null), format, file);
            statusLabel.setText("Image exported as " + format.toUpperCase());
        } catch (IOException e) {
            showError("Image export failed.");
        }
    }

    private void clearAll() {
        loadedImage = null;
        currentAscii = "";
        originalImageView.setImage(null);
        asciiArea.clear();
        imageInfoLabel.setText("No image loaded");
        statusLabel.setText("Cleared");
    }

    private void applyTheme(BorderPane root) {
        if (darkMode) {
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #111827, #1f2937);");

            updateLabelsForTheme(root, true);
            asciiArea.setStyle("""
                    -fx-control-inner-background: #020617;
                    -fx-text-fill: #e2e8f0;
                    -fx-highlight-fill: #334155;
                    -fx-highlight-text-fill: white;
                    -fx-border-color: rgba(255,255,255,0.10);
                    -fx-border-radius: 14;
                    """);
        } else {
            root.setStyle("-fx-background-color: linear-gradient(to bottom right, #fff7ed, #ffedd5);");

            updateLabelsForTheme(root, false);
            asciiArea.setStyle("""
                    -fx-control-inner-background: white;
                    -fx-text-fill: #111827;
                    -fx-highlight-fill: #e5e7eb;
                    -fx-highlight-text-fill: black;
                    -fx-border-color: #d1d5db;
                    -fx-border-radius: 14;
                    """);
        }

        statusLabel.setStyle(darkMode ? "-fx-text-fill: white;" : "-fx-text-fill: #111827;");
        imageInfoLabel.setStyle(darkMode ? "-fx-text-fill: #cbd5e1;" : "-fx-text-fill: #374151;");
    }

    private void updateLabelsForTheme(Pane root, boolean dark) {
        applyTextColorRecursively(root, dark ? "#ffffff" : "#111827");
    }

    private void applyTextColorRecursively(javafx.scene.Node node, String color) {
        if (node instanceof Labeled labeled) {
            String existing = labeled.getStyle() == null ? "" : labeled.getStyle();
            if (!existing.contains("-fx-text-fill")) {
                labeled.setStyle(existing + "-fx-text-fill: " + color + ";");
            } else {
                labeled.setStyle(existing.replaceAll("-fx-text-fill:\\s*[^;]+;", "-fx-text-fill: " + color + ";"));
            }
        }

        if (node instanceof TextInputControl input) {
            if (darkMode) {
                input.setStyle("""
                        -fx-background-color: rgba(255,255,255,0.12);
                        -fx-text-fill: white;
                        -fx-prompt-text-fill: #94a3b8;
                        -fx-background-radius: 12;
                        -fx-border-radius: 12;
                        -fx-border-color: rgba(255,255,255,0.12);
                        -fx-padding: 8 10 8 10;
                        """);
            } else {
                input.setStyle("""
                        -fx-background-color: white;
                        -fx-text-fill: #111827;
                        -fx-prompt-text-fill: #6b7280;
                        -fx-background-radius: 12;
                        -fx-border-radius: 12;
                        -fx-border-color: #d1d5db;
                        -fx-padding: 8 10 8 10;
                        """);
            }
        }

        if (node instanceof Pane pane) {
            for (javafx.scene.Node child : pane.getChildren()) {
                applyTextColorRecursively(child, color);
            }
        }
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle("""
                -fx-background-color: linear-gradient(to right, #2563eb, #0891b2);
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 14;
                -fx-padding: 10 16 10 16;
                -fx-cursor: hand;
                """);

        button.setOnMouseEntered(e -> button.setStyle("""
                -fx-background-color: linear-gradient(to right, #1d4ed8, #0e7490);
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 14;
                -fx-padding: 10 16 10 16;
                -fx-cursor: hand;
                """));

        button.setOnMouseExited(e -> button.setStyle("""
                -fx-background-color: linear-gradient(to right, #2563eb, #0891b2);
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 14;
                -fx-padding: 10 16 10 16;
                -fx-cursor: hand;
                """));

        return button;
    }

    private void styleTextField(TextField field, String prompt) {
        field.setPromptText(prompt);
    }

    private void setWhiteText(Labeled... labels) {
        for (Labeled label : labels) {
            label.setStyle("-fx-text-fill: white;");
        }
    }

    private String cardStyle() {
        return """
                -fx-background-color: rgba(255,255,255,0.08);
                -fx-background-radius: 20;
                -fx-border-color: rgba(255,255,255,0.10);
                -fx-border-radius: 20;
                """;
    }

    private String sectionTitleStyle() {
        return """
                -fx-font-size: 18px;
                -fx-font-weight: bold;
                """;
    }

    private String innerPanelStyle() {
        return """
                -fx-background-color: rgba(2,6,23,0.75);
                -fx-background-radius: 16;
                -fx-border-color: rgba(255,255,255,0.10);
                -fx-border-radius: 16;
                """;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        statusLabel.setText("Error: " + message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}