package todoappfx_modern;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Task {
    public enum Priority { DUSUK, ORTA, YUKSEK }

    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty note = new SimpleStringProperty("");
    private final ObjectProperty<Priority> priority = new SimpleObjectProperty<>(Priority.ORTA);
    private final ObjectProperty<LocalDate> dueDate = new SimpleObjectProperty<>(null);
    private final BooleanProperty done = new SimpleBooleanProperty(false);
    private final StringProperty category = new SimpleStringProperty("Work");
    private final StringProperty tagsCsv = new SimpleStringProperty(""); // "#spor,#ödev"

    public Task(int id, String category, String title, String note, boolean done, Priority priority, LocalDate dueDate, String tagsCsv) {
        this.id.set(id);
        this.category.set(category == null || category.isBlank() ? "Work" : category);
        this.title.set(title == null ? "" : title);
        this.note.set(note == null ? "" : note);
        this.done.set(done);
        this.priority.set(priority == null ? Priority.ORTA : priority);
        this.dueDate.set(dueDate);
        this.tagsCsv.set(tagsCsv == null ? "" : tagsCsv);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getTitle() { return title.get(); }
    public void setTitle(String v) { title.set(v); }
    public StringProperty titleProperty() { return title; }

    public String getNote() { return note.get(); }
    public void setNote(String v) { note.set(v == null ? "" : v); }
    public StringProperty noteProperty() { return note; }

    public Priority getPriority() { return priority.get(); }
    public void setPriority(Priority v) { priority.set(v); }
    public ObjectProperty<Priority> priorityProperty() { return priority; }

    public LocalDate getDueDate() { return dueDate.get(); }
    public void setDueDate(LocalDate v) { dueDate.set(v); }
    public ObjectProperty<LocalDate> dueDateProperty() { return dueDate; }

    public boolean isDone() { return done.get(); }
    public void setDone(boolean v) { done.set(v); }
    public BooleanProperty doneProperty() { return done; }

    public String getCategory() { return category.get(); }
    public void setCategory(String v) { category.set(v == null || v.isBlank() ? "Work" : v); }
    public StringProperty categoryProperty() { return category; }

    public String getTagsCsv() { return tagsCsv.get(); }
    public void setTagsCsv(String v) { tagsCsv.set(v == null ? "" : v); }
    public StringProperty tagsCsvProperty() { return tagsCsv; }

    public List<String> getTagsNormalized() {
        String s = getTagsCsv();
        if (s == null || s.isBlank()) return new ArrayList<>();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .map(x -> x.startsWith("#") ? x : "#" + x)
                .distinct()
                .toList();
    }
}
