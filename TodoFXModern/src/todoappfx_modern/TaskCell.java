package todoappfx_modern;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.BooleanSupplier;

public class TaskCell extends ListCell<Task> {

    public interface Callbacks {
        void onDelete(Task t);
        void onEdit(Task t);
        void onToggleDone(Task t, boolean done);
        void onReorder(Task dragged, Task target, boolean before);
    }

    private final Callbacks cb;
    private final BooleanSupplier canDragDrop;

    public TaskCell(Callbacks cb, BooleanSupplier canDragDrop) {
        this.cb = cb;
        this.canDragDrop = canDragDrop;
    }

    @Override
    protected void updateItem(Task t, boolean empty) {
        super.updateItem(t, empty);

        if (empty || t == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        LocalDate today = LocalDate.now();
        boolean overdue = !t.isDone() && t.getDueDate() != null && t.getDueDate().isBefore(today);
        boolean dueToday = !t.isDone() && t.getDueDate() != null && t.getDueDate().isEqual(today);

        CheckBox done = new CheckBox();
        done.setSelected(t.isDone());
        done.selectedProperty().addListener((o,a,b)-> cb.onToggleDone(t, b));

        Label title = new Label(t.getTitle());
        title.getStyleClass().add("task-title");
        if (t.isDone()) title.getStyleClass().add("task-done");

        Label note = new Label(t.getNote() == null ? "" : t.getNote());
        note.getStyleClass().add("task-note");
        note.setWrapText(true);
        note.setMaxWidth(320);

        Label prBadge = new Label(t.getPriority().name());
        prBadge.getStyleClass().addAll("badge", "badge-" + t.getPriority().name().toLowerCase());

        Label dueBadge = new Label();
        dueBadge.getStyleClass().add("badge");
        if (overdue) {
            dueBadge.setText("OVERDUE");
            dueBadge.getStyleClass().add("badge-overdue");
        } else if (dueToday) {
            dueBadge.setText("TODAY");
            dueBadge.getStyleClass().add("badge-today");
        } else {
            dueBadge.setText("");
            dueBadge.setVisible(false);
            dueBadge.setManaged(false);
        }

        String dateStr = t.getDueDate() == null ? "—" : t.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        Label date = new Label(dateStr);
        date.getStyleClass().add("task-date");

        Label tags = new Label(t.getTagsCsv() == null ? "" : t.getTagsCsv());
        tags.getStyleClass().add("meta");

        HBox meta = new HBox(10, prBadge, dueBadge, new Text("•"), date);
        meta.getStyleClass().add("meta");
        meta.setAlignment(Pos.CENTER_LEFT);

        VBox texts = new VBox(6, title, note, meta, tags);
        texts.setAlignment(Pos.CENTER_LEFT);

        Button edit = new Button("Düzenle");
        edit.getStyleClass().add("ghost-btn");
        edit.setOnAction(e -> cb.onEdit(t));

        Button del = new Button("Sil");
        del.getStyleClass().add("danger-btn");
        del.setOnAction(e -> cb.onDelete(t));

        HBox actions = new HBox(8, edit, del);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, done, texts, spacer, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.getStyleClass().add("task-card");

        if (t.isDone()) row.getStyleClass().add("task-card-done");
        if (overdue) row.getStyleClass().add("card-overdue");
        else if (dueToday) row.getStyleClass().add("card-today");

        setGraphic(row);

        // Double click => edit
        setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2 && ev.getButton() == MouseButton.PRIMARY) {
                cb.onEdit(t);
            }
        });

        // ===== DRAG & DROP =====
        setOnDragDetected(ev -> {
            if (!canDragDrop.getAsBoolean()) return;
            if (getItem() == null) return;
            Dragboard db = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(getItem().getId()));
            db.setContent(cc);
            ev.consume();
        });

        setOnDragOver(ev -> {
            if (!canDragDrop.getAsBoolean()) return;
            Dragboard db = ev.getDragboard();
            if (db.hasString() && getItem() != null) ev.acceptTransferModes(TransferMode.MOVE);
            ev.consume();
        });

        setOnDragDropped(ev -> {
            if (!canDragDrop.getAsBoolean()) return;
            Dragboard db = ev.getDragboard();
            if (!db.hasString()) return;

            int draggedId = Integer.parseInt(db.getString());
            Task dragged = getListView().getItems().stream().filter(x -> x.getId() == draggedId).findFirst().orElse(null);
            Task target = getItem();

            if (dragged != null && target != null && dragged != target) {
                cb.onReorder(dragged, target, true);
            }

            ev.setDropCompleted(true);
            ev.consume();
        });
    }
}
