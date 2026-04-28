package todoapp;

public class Task {
    private int id;
    private String title;
    private boolean done;
    private String priority;   // DUSUK, ORTA, YUKSEK
    private String dueDate;    // yyyy-MM-dd veya ""

    public Task(int id, String title, boolean done, String priority, String dueDate) {
        this.id = id;
        this.title = title;
        this.done = done;
        this.priority = priority;
        this.dueDate = dueDate == null ? "" : dueDate;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public boolean isDone() { return done; }
    public String getPriority() { return priority; }
    public String getDueDate() { return dueDate; }

    public void setTitle(String title) { this.title = title; }
    public void setDone(boolean done) { this.done = done; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate == null ? "" : dueDate; }

    // Listede güzel görünmesi için
    @Override
    public String toString() {
        String status = done ? "✅" : "⬜";
        String datePart = (dueDate == null || dueDate.isBlank()) ? "" : " | " + dueDate;
        return status + " [" + priority + "] " + title + datePart + " (#" + id + ")";
    }
}
