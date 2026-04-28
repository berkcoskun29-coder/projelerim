package todoappfx_modern;

import javafx.application.Application;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private final String APP_FILE = "todo_pro.txt";
    private final FileStorageTxt storage = new FileStorageTxt(APP_FILE);

    private final ObservableList<String> categories = FXCollections.observableArrayList("Work", "School", "Personal");
    private final Map<String, ObservableList<Task>> master = new LinkedHashMap<>(); // category -> tasks ordered
    private int nextId = 1;

    // UI state
    private String theme = "dark";
    private String accent = "blue";
    private enum StatusFilter { ALL, ACTIVE, DONE }
    private StatusFilter statusFilter = StatusFilter.ALL;
    private String searchQuery = "";

    private BorderPane root;
    private HBox board;
    private final Map<String, ListView<Task>> listViews = new LinkedHashMap<>();
    private final Map<String, ObservableList<Task>> viewLists = new LinkedHashMap<>(); // filtered view snapshots

    @Override
    public void start(Stage stage) {

        // ===== Load =====
        var loaded = storage.load();
        theme = loaded.theme == null ? "dark" : loaded.theme;
        accent = loaded.accent == null ? "blue" : loaded.accent;

        categories.setAll(loaded.categories);
        ensureMasterLists();

        for (Task t : loaded.tasks) {
            nextId = Math.max(nextId, t.getId() + 1);
            addToMaster(t.getCategory(), t);
        }

        // ===== Topbar: title + search + chips + stats + theme + import/export/about =====
        Label appTitle = new Label("To-Do Kanban Pro");
        appTitle.getStyleClass().add("app-title");

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Ara: başlık / not / #tag");
        txtSearch.getStyleClass().add("search");

        ToggleButton chipAll = chip("Tümü");
        ToggleButton chipActive = chip("Aktif");
        ToggleButton chipDone = chip("Tamamlanan");
        ToggleGroup chips = new ToggleGroup();
        chipAll.setToggleGroup(chips);
        chipActive.setToggleGroup(chips);
        chipDone.setToggleGroup(chips);
        chipAll.setSelected(true);

        ComboBox<String> accentBox = new ComboBox<>();
        accentBox.getItems().addAll("blue", "purple");
        accentBox.setValue(accent);

        ToggleButton themeToggle = new ToggleButton();
        themeToggle.getStyleClass().add("ghost-btn");
        themeToggle.setSelected("dark".equalsIgnoreCase(theme));
        themeToggle.setText(themeToggle.isSelected() ? "🌙 Dark" : "☀️ Light");

        Button btnExport = new Button("Export");
        btnExport.getStyleClass().add("ghost-btn");
        Button btnImport = new Button("Import");
        btnImport.getStyleClass().add("ghost-btn");
        Button btnAbout = new Button("About");
        btnAbout.getStyleClass().add("ghost-btn");

        HBox chipRow = new HBox(8, chipAll, chipActive, chipDone);
        chipRow.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox top = new HBox(12, appTitle, sp, txtSearch, chipRow, new Label("Accent:"), accentBox, themeToggle, btnImport, btnExport, btnAbout);
        top.setPadding(new Insets(16));
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");

        // ===== Dashboard =====
        Label vTotal = new Label();
        Label vActive = new Label();
        Label vDone = new Label();
        Label vOverdue = new Label();
        Label vToday = new Label();

        HBox stats = new HBox(12,
                statCard("Total", vTotal),
                statCard("Active", vActive),
                statCard("Done", vDone),
                statCard("Overdue", vOverdue),
                statCard("Today", vToday)
        );
        stats.setPadding(new Insets(0, 16, 12, 16));

        // ===== Left panel: Add + Category manage =====
        TextField fTitle = new TextField();
        fTitle.setPromptText("Görev başlığı");

        TextArea fNote = new TextArea();
        fNote.setPromptText("Not (opsiyonel)");
        fNote.setPrefRowCount(3);

        ComboBox<Task.Priority> fPr = new ComboBox<>();
        fPr.getItems().addAll(Task.Priority.values());
        fPr.setValue(Task.Priority.ORTA);

        ComboBox<String> fCat = new ComboBox<>(categories);
        fCat.setValue(categories.get(0));

        DatePicker fDate = new DatePicker();
        fDate.setPromptText("Bitiş tarihi");

        TextField fTags = new TextField();
        fTags.setPromptText("#spor,#ödev");

        Button btnAdd = new Button("Ekle");
        btnAdd.getStyleClass().add("primary-btn");

        Button btnClear = new Button("Temizle");
        btnClear.getStyleClass().add("ghost-btn");

        // Category manager
        TextField newCat = new TextField();
        newCat.setPromptText("Yeni kategori adı");

        Button btnAddCat = new Button("Kategori Ekle");
        btnAddCat.getStyleClass().add("ghost-btn");

        Button btnDelCat = new Button("Kategori Sil");
        btnDelCat.getStyleClass().add("danger-btn");

        VBox left = new VBox(10,
                label("Başlık"), fTitle,
                label("Not"), fNote,
                label("Öncelik"), fPr,
                label("Kategori"), fCat,
                label("Tarih"), fDate,
                label("Etiketler"), fTags,
                new HBox(10, btnAdd, btnClear),
                new Separator(),
                label("Kategori Yönetimi"),
                newCat,
                new HBox(10, btnAddCat, btnDelCat)
        );
        left.setPadding(new Insets(16));
        left.getStyleClass().add("panel");
        left.setPrefWidth(340);

        // ===== Board (dynamic columns) =====
        board = new HBox(14);
        board.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(board);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox center = new VBox(12, stats, scroll);
        HBox.setHgrow(center, Priority.ALWAYS);

        root = new BorderPane();
        root.setTop(top);
        root.setCenter(new HBox(14, left, center));
        root.getStyleClass().add("root");
        applyTheme();

        Scene scene = new Scene(root, 1400, 780);
        scene.getStylesheets().add(getClass().getResource("/todoappfx_modern/app.css").toExternalForm());

        stage.setTitle("To-Do Kanban Pro (JavaFX + TXT)");
        stage.setScene(scene);
        stage.show();

        // ===== Build columns UI initially =====
        rebuildBoardUI();

        // ===== Refresh stats + filter =====
        Runnable refresh = () -> {
            applyGlobalFilter(txtSearch.getText(), chips);
            updateViewLists();  // create view snapshots
            refreshListViews();
            updateStats(vTotal, vActive, vDone, vOverdue, vToday);
        };

        refresh.run();

        // ===== Events =====
        txtSearch.textProperty().addListener((o,a,b) -> refresh.run());

        chips.selectedToggleProperty().addListener((o,a,b) -> {
            if (b == chipAll) statusFilter = StatusFilter.ALL;
            else if (b == chipActive) statusFilter = StatusFilter.ACTIVE;
            else statusFilter = StatusFilter.DONE;
            refresh.run();
        });

        themeToggle.setOnAction(e -> {
            theme = themeToggle.isSelected() ? "dark" : "light";
            themeToggle.setText(themeToggle.isSelected() ? "🌙 Dark" : "☀️ Light");
            applyTheme();
            autoSave();
        });

        accentBox.setOnAction(e -> {
            accent = accentBox.getValue();
            applyTheme();
            autoSave();
        });

        btnAdd.setOnAction(e -> {
            String titleText = fTitle.getText().trim();
            if (titleText.isEmpty()) { info("Başlık boş olamaz!"); return; }

            Task t = new Task(nextId++, fCat.getValue(), titleText, fNote.getText(), false, fPr.getValue(), fDate.getValue(), fTags.getText());
            addToMaster(t.getCategory(), t);

            fTitle.clear();
            fNote.clear();
            fPr.setValue(Task.Priority.ORTA);
            fDate.setValue(null);
            fTags.clear();

            autoSave();
            refresh.run();
        });

        btnClear.setOnAction(e -> {
            fTitle.clear();
            fNote.clear();
            fPr.setValue(Task.Priority.ORTA);
            fCat.setValue(categories.get(0));
            fDate.setValue(null);
            fTags.clear();
        });

        btnAddCat.setOnAction(e -> {
            String c = newCat.getText().trim();
            if (c.isBlank()) return;
            if (categories.contains(c)) { info("Bu kategori zaten var."); return; }

            categories.add(c);
            master.put(c, FXCollections.observableArrayList());
            newCat.clear();

            fCat.setItems(categories);
            rebuildBoardUI();
            autoSave();
            refresh.run();
        });

        btnDelCat.setOnAction(e -> {
            String selected = fCat.getValue();
            if (selected == null) return;
            if (categories.size() <= 1) { info("En az 1 kategori kalmalı."); return; }

            // move tasks to first category
            String target = categories.stream().filter(x -> !x.equals(selected)).findFirst().orElse(categories.get(0));
            var moving = new ArrayList<>(master.getOrDefault(selected, FXCollections.observableArrayList()));
            for (Task t : moving) {
                t.setCategory(target);
                addToMaster(target, t);
            }

            master.remove(selected);
            categories.remove(selected);

            fCat.setItems(categories);
            fCat.setValue(target);

            rebuildBoardUI();
            autoSave();
            refresh.run();
        });

        btnAbout.setOnAction(e -> info(
                "To-Do Kanban Pro\n" +
                "- Drag&Drop: reorder + move\n" +
                "- Search + filter + tags\n" +
                "- Theme + Accent\n" +
                "- TXT save + backup + import/export\n\n" +
                "Kısayollar:\nCtrl+F Search | Ctrl+N New | Ctrl+S Save | Delete Sil | Space Done | Esc Temizle"
        ));

        btnExport.setOnAction(e -> exportTo(stage));
        btnImport.setOnAction(e -> {
            if (importFrom(stage)) {
                rebuildBoardUI();
                refresh.run();
                info("Import tamam ✅");
            }
        });

        // ===== Shortcuts =====
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.isControlDown() && ev.getCode() == KeyCode.F) { txtSearch.requestFocus(); ev.consume(); }
            if (ev.isControlDown() && ev.getCode() == KeyCode.N) { fTitle.requestFocus(); ev.consume(); }
            if (ev.isControlDown() && ev.getCode() == KeyCode.S) { autoSave(); ev.consume(); }

            if (ev.getCode() == KeyCode.ESCAPE) { btnClear.fire(); ev.consume(); }

            if (ev.getCode() == KeyCode.DELETE) {
                Task sel = getAnySelectedTask();
                if (sel != null && confirm("Seçili görevi silmek istiyor musun?")) {
                    deleteTask(sel);
                    autoSave();
                    refresh.run();
                }
                ev.consume();
            }

            if (ev.getCode() == KeyCode.SPACE) {
                Task sel = getAnySelectedTask();
                if (sel != null) {
                    sel.setDone(!sel.isDone());
                    autoSave();
                    refresh.run();
                }
                ev.consume();
            }
        });

        stage.setOnCloseRequest(e -> autoSave());

        // ===== Startup notification =====
        showStartupReminder();
    }

    // =========================
    // Board build & list views
    // =========================

    private void rebuildBoardUI() {
        board.getChildren().clear();
        listViews.clear();
        viewLists.clear();

        for (String cat : categories) {
            ListView<Task> lv = new ListView<>();
            lv.setPrefWidth(360);

            BooleanSupplier canDragDrop = () -> statusFilter == StatusFilter.ALL && (searchQuery == null || searchQuery.isBlank());

            TaskCell.Callbacks callbacks = new TaskCell.Callbacks() {
                @Override public void onDelete(Task t) {
                    if (!confirm("Silinsin mi?")) return;
                    deleteTask(t);
                    autoSave();
                    updateViewLists();
                    refreshListViews();
                }
                @Override public void onEdit(Task t) {
                    boolean changed = EditTaskDialog.show(t, categories);
                    if (changed) {
                        // category changed? move
                        if (!master.containsKey(t.getCategory())) {
                            categories.add(t.getCategory());
                            master.put(t.getCategory(), FXCollections.observableArrayList());
                            rebuildBoardUI();
                        }
                        moveToCategoryIfNeeded(t);
                        autoSave();
                        updateViewLists();
                        refreshListViews();
                    }
                }
                @Override public void onToggleDone(Task t, boolean done) {
                    t.setDone(done);
                    autoSave();
                    updateViewLists();
                    refreshListViews();
                }
                @Override public void onReorder(Task dragged, Task target, boolean before) {
                    if (!canDragDrop.getAsBoolean()) return;
                    String c = dragged.getCategory();
                    ObservableList<Task> list = master.get(c);
                    if (list == null) return;
                    int oldIndex = list.indexOf(dragged);
                    int newIndex = list.indexOf(target);
                    if (oldIndex < 0 || newIndex < 0 || oldIndex == newIndex) return;
                    list.remove(dragged);
                    list.add(newIndex, dragged);
                    autoSave();
                }
            };

            lv.setCellFactory(v -> new TaskCell(callbacks, canDragDrop));

            // drop to empty area => move between columns
            lv.setOnDragOver(ev -> {
                if (!canDragDrop.getAsBoolean()) return;
                if (ev.getDragboard().hasString()) ev.acceptTransferModes(TransferMode.MOVE);
                ev.consume();
            });

            lv.setOnDragDropped(ev -> {
                if (!canDragDrop.getAsBoolean()) return;
                Dragboard db = ev.getDragboard();
                if (!db.hasString()) return;
                int id = Integer.parseInt(db.getString());
                Task dragged = findById(id);
                if (dragged == null) return;

                // move to this column
                String targetCategory = cat;
                moveTaskToCategory(dragged, targetCategory);

                autoSave();
                updateViewLists();
                refreshListViews();

                ev.setDropCompleted(true);
                ev.consume();
            });

            listViews.put(cat, lv);

            Label t = new Label(cat);
            t.getStyleClass().add("column-title");
            Label s = new Label("Sürükle-bırak ile taşı / sırala");
            s.getStyleClass().add("column-sub");

            VBox col = new VBox(10, t, s, lv);
            col.setPadding(new Insets(16));
            col.getStyleClass().add("panel");
            VBox.setVgrow(lv, Priority.ALWAYS);

            board.getChildren().add(col);
            HBox.setHgrow(col, Priority.ALWAYS);
        }

        updateViewLists();
        refreshListViews();
    }

    private void updateViewLists() {
        // if filtering/searching: create snapshots, else show master lists directly
        boolean filtered = !(statusFilter == StatusFilter.ALL && (searchQuery == null || searchQuery.isBlank()));

        viewLists.clear();
        for (String cat : categories) {
            ObservableList<Task> src = master.getOrDefault(cat, FXCollections.observableArrayList());

            if (!filtered) {
                viewLists.put(cat, src);
            } else {
                List<Task> filteredList = src.stream()
                        .filter(this::matchesFilter)
                        .collect(Collectors.toList());
                viewLists.put(cat, FXCollections.observableArrayList(filteredList));
            }
        }
    }

    private void refreshListViews() {
        for (String cat : categories) {
            ListView<Task> lv = listViews.get(cat);
            if (lv != null) lv.setItems(viewLists.getOrDefault(cat, FXCollections.observableArrayList()));
        }
    }

    // =========================
    // Filtering
    // =========================

    private void applyGlobalFilter(String q, ToggleGroup chips) {
        searchQuery = (q == null) ? "" : q.trim().toLowerCase();
        // statusFilter is managed by chip events
    }

    private boolean matchesFilter(Task t) {
        boolean statusOk =
                statusFilter == StatusFilter.ALL ||
                (statusFilter == StatusFilter.ACTIVE && !t.isDone()) ||
                (statusFilter == StatusFilter.DONE && t.isDone());

        if (!statusOk) return false;

        if (searchQuery == null || searchQuery.isBlank()) return true;

        // tag search: if query contains '#', match tags also
        String in = searchQuery;

        boolean textMatch =
                safeLower(t.getTitle()).contains(in) ||
                safeLower(t.getNote()).contains(in) ||
                safeLower(t.getTagsCsv()).contains(in);

        return textMatch;
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    // =========================
    // Stats
    // =========================

    private void updateStats(Label total, Label active, Label done, Label overdue, Label today) {
        List<Task> all = allTasksInOrder();

        long cTotal = all.size();
        long cDone = all.stream().filter(Task::isDone).count();
        long cActive = cTotal - cDone;

        LocalDate now = LocalDate.now();
        long cOverdue = all.stream().filter(t -> !t.isDone() && t.getDueDate() != null && t.getDueDate().isBefore(now)).count();
        long cToday = all.stream().filter(t -> !t.isDone() && t.getDueDate() != null && t.getDueDate().isEqual(now)).count();

        total.setText(String.valueOf(cTotal));
        active.setText(String.valueOf(cActive));
        done.setText(String.valueOf(cDone));
        overdue.setText(String.valueOf(cOverdue));
        today.setText(String.valueOf(cToday));
    }

    private VBox statCard(String label, Label value) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        value.getStyleClass().add("stat-value");
        VBox box = new VBox(4, l, value);
        box.getStyleClass().add("stat-card");
        box.setPadding(new Insets(12));
        return box;
    }

    // =========================
    // Theme
    // =========================

    private void applyTheme() {
        root.getStyleClass().removeAll("theme-dark", "theme-light", "accent-blue", "accent-purple");
        root.getStyleClass().add("dark".equalsIgnoreCase(theme) ? "theme-dark" : "theme-light");
        root.getStyleClass().add("purple".equalsIgnoreCase(accent) ? "accent-purple" : "accent-blue");
    }

    // =========================
    // Persistence
    // =========================

    private void autoSave() {
        storage.saveWithBackup(theme, accent, new ArrayList<>(categories), allTasksInOrder());
    }

    // =========================
    // Task operations
    // =========================

    private void ensureMasterLists() {
        master.clear();
        for (String c : categories) master.put(c, FXCollections.observableArrayList());
    }

    private void addToMaster(String category, Task t) {
        if (!master.containsKey(category)) {
            categories.add(category);
            master.put(category, FXCollections.observableArrayList());
        }
        master.get(category).add(t);
    }

    private void deleteTask(Task t) {
        ObservableList<Task> list = master.get(t.getCategory());
        if (list != null) list.removeIf(x -> x.getId() == t.getId());
    }

    private void moveToCategoryIfNeeded(Task t) {
        // ensure task lives in the right list
        for (String c : master.keySet()) {
            ObservableList<Task> list = master.get(c);
            if (list == null) continue;

            boolean inThis = list.stream().anyMatch(x -> x.getId() == t.getId());
            if (inThis && !c.equals(t.getCategory())) {
                list.removeIf(x -> x.getId() == t.getId());
                addToMaster(t.getCategory(), t);
                return;
            }
        }
        // not found in any list -> add
        if (!master.getOrDefault(t.getCategory(), FXCollections.observableArrayList()).contains(t)) {
            addToMaster(t.getCategory(), t);
        }
    }

    private void moveTaskToCategory(Task t, String targetCategory) {
        if (t.getCategory().equals(targetCategory)) return;
        deleteTask(t);
        t.setCategory(targetCategory);
        addToMaster(targetCategory, t);
    }

    private Task findById(int id) {
        for (Task t : allTasksInOrder()) if (t.getId() == id) return t;
        return null;
    }

    private List<Task> allTasksInOrder() {
        List<Task> all = new ArrayList<>();
        for (String c : categories) {
            all.addAll(master.getOrDefault(c, FXCollections.observableArrayList()));
        }
        return all;
    }

    private Task getAnySelectedTask() {
        for (ListView<Task> lv : listViews.values()) {
            Task sel = lv.getSelectionModel().getSelectedItem();
            if (sel != null) return sel;
        }
        return null;
    }

    // =========================
    // Import / Export
    // =========================

    private void exportTo(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export TXT");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT", "*.txt"));
        fc.setInitialFileName("todo_export.txt");
        File out = fc.showSaveDialog(stage);
        if (out == null) return;

        new FileStorageTxt(out.getAbsolutePath()).saveWithBackup(theme, accent, new ArrayList<>(categories), allTasksInOrder());
        info("Export tamam ✅");
    }

    private boolean importFrom(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import TXT");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("TXT", "*.txt"));
        File in = fc.showOpenDialog(stage);
        if (in == null) return false;

        var data = new FileStorageTxt(in.getAbsolutePath()).load();

        theme = data.theme == null ? "dark" : data.theme;
        accent = data.accent == null ? "blue" : data.accent;

        categories.setAll(data.categories);
        ensureMasterLists();

        nextId = data.tasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
        for (Task t : data.tasks) addToMaster(t.getCategory(), t);

        applyTheme();
        autoSave();
        return true;
    }

    // =========================
    // Startup reminder
    // =========================

    private void showStartupReminder() {
        List<Task> all = allTasksInOrder();
        LocalDate now = LocalDate.now();
        long overdue = all.stream().filter(t -> !t.isDone() && t.getDueDate() != null && t.getDueDate().isBefore(now)).count();
        long today = all.stream().filter(t -> !t.isDone() && t.getDueDate() != null && t.getDueDate().isEqual(now)).count();

        if (overdue > 0 || today > 0) {
            info("Hatırlatma:\n" +
                    (overdue > 0 ? "• " + overdue + " gecikmiş görev var\n" : "") +
                    (today > 0 ? "• Bugün bitmesi gereken " + today + " görev var\n" : "") +
                    "\nNot: Filtre/arama açıkken sürükle-bırak kapalıdır.");
        }
    }

    // =========================
    // UI helpers
    // =========================

    private ToggleButton chip(String text) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("chip");
        return b;
    }

    private Label label(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("field-label");
        return l;
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
