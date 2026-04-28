package todoappfx_modern;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class FileStorageTxt {
    private final File file;

    public static class LoadedData {
        public String theme = "dark";  // dark/light
        public String accent = "blue"; // blue/purple
        public List<String> categories = new ArrayList<>(List.of("Work","School","Personal"));
        public List<Task> tasks = new ArrayList<>();
    }

    public FileStorageTxt(String path) {
        this.file = new File(path);
    }

    public LoadedData load() {
        LoadedData out = new LoadedData();
        if (!file.exists()) return out;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("#SETTINGS|")) {
                    // #SETTINGS|theme=dark|accent=blue
                    String[] parts = line.split("\\|");
                    for (String p : parts) {
                        if (p.startsWith("theme=")) out.theme = p.substring("theme=".length()).trim();
                        if (p.startsWith("accent=")) out.accent = p.substring("accent=".length()).trim();
                    }
                    continue;
                }

                if (line.startsWith("#CATEGORIES|")) {
                    String rest = line.substring("#CATEGORIES|".length()).trim();
                    if (!rest.isBlank()) {
                        List<String> cats = new ArrayList<>();
                        for (String c : rest.split(",")) {
                            String x = c.trim();
                            if (!x.isBlank()) cats.add(x);
                        }
                        if (!cats.isEmpty()) out.categories = cats;
                    }
                    continue;
                }

                // task line: id|category|title|note|done|priority|dueDate|tagsCsv
                String[] p = line.split("\\|", -1);
                if (p.length < 8) continue;

                int id = Integer.parseInt(p[0]);
                String category = p[1];
                String title = p[2];
                String note = p[3];
                boolean done = Boolean.parseBoolean(p[4]);
                Task.Priority pr = Task.Priority.valueOf(p[5]);

                LocalDate due = null;
                if (!p[6].isBlank()) due = LocalDate.parse(p[6]);

                String tagsCsv = p[7];

                out.tasks.add(new Task(id, category, title, note, done, pr, due, tagsCsv));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // categories içinde task kategorileri yoksa ekle (robust)
        Set<String> catSet = new LinkedHashSet<>(out.categories);
        for (Task t : out.tasks) catSet.add(t.getCategory());
        out.categories = new ArrayList<>(catSet);

        return out;
    }

    public void saveWithBackup(String theme, String accent, List<String> categories, List<Task> tasksInOrder) {
        // backup
        File backup = new File(file.getParentFile() == null ? "" : file.getParentFile().getPath(),
                file.getName().replace(".txt", "") + "_backup.txt");

        try {
            if (file.exists()) copyFile(file, backup);
        } catch (Exception ignored) {}

        save(theme, accent, categories, tasksInOrder);
    }

    private void save(String theme, String accent, List<String> categories, List<Task> tasksInOrder) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

            bw.write("#SETTINGS|theme=" + safe(theme) + "|accent=" + safe(accent));
            bw.newLine();

            bw.write("#CATEGORIES|" + String.join(",", categories));
            bw.newLine();

            for (Task t : tasksInOrder) {
                String due = t.getDueDate() == null ? "" : t.getDueDate().toString();
                bw.write(t.getId() + "|" +
                        safe(t.getCategory()) + "|" +
                        safe(t.getTitle()) + "|" +
                        safe(t.getNote()) + "|" +
                        t.isDone() + "|" +
                        t.getPriority().name() + "|" +
                        due + "|" +
                        safe(t.getTagsCsv()));
                bw.newLine();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("|", "/").replace("\n", " ").replace("\r", " ");
    }

    private void copyFile(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
    }
}
