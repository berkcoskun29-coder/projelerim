package todoapp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileStorage {
    private final File file;

    public FileStorage(String path) {
        this.file = new File(path);
    }

    public List<Task> load() {
        List<Task> tasks = new ArrayList<>();
        if (!file.exists()) return tasks;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // id|title|done|priority|dueDate
                String[] p = line.split("\\|", -1);
                if (p.length < 5) continue;

                int id = Integer.parseInt(p[0]);
                String title = p[1];
                boolean done = Boolean.parseBoolean(p[2]);
                String priority = p[3];
                String dueDate = p[4];

                tasks.add(new Task(id, title, done, priority, dueDate));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tasks;
    }

    public void save(List<Task> tasks) {
        // klasör yoksa oluştur (gerekirse)
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            for (Task t : tasks) {
                String safeTitle = t.getTitle().replace("|", "/"); // ayırıcı bozulmasın
                String safeDate = (t.getDueDate() == null) ? "" : t.getDueDate().replace("|", "/");

                bw.write(t.getId() + "|" + safeTitle + "|" + t.isDone() + "|" + t.getPriority() + "|" + safeDate);
                bw.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
