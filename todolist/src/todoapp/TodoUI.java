package todoapp;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TodoUI extends JFrame {

    private final DefaultListModel<Task> listModel = new DefaultListModel<>();
    private final JList<Task> taskList = new JList<>(listModel);

    private final JTextField txtTitle = new JTextField();
    private final JComboBox<String> cmbPriority = new JComboBox<>(new String[]{"DUSUK", "ORTA", "YUKSEK"});
    private final JTextField txtDueDate = new JTextField(); // yyyy-MM-dd

    private final JTextField txtSearch = new JTextField();
    private final JComboBox<String> cmbFilter = new JComboBox<>(new String[]{"Tümü", "Aktif", "Tamamlanan"});

    private final JButton btnAdd = new JButton("Ekle");
    private final JButton btnToggle = new JButton("Tamamla/Aç");
    private final JButton btnDelete = new JButton("Sil");
    private final JButton btnSave = new JButton("Kaydet");

    private final List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    private final FileStorage storage;

    public TodoUI(FileStorage storage) {
        super("To-Do List (TXT)");
        this.storage = storage;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 480);
        setLocationRelativeTo(null);

        buildUI();
        wireEvents();

        loadFromFile();
        refreshList();
    }

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        searchPanel.add(new JLabel("Ara:"), BorderLayout.WEST);
        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(cmbFilter, BorderLayout.EAST);

        top.add(searchPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // Sol: ekleme paneli
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createTitledBorder("Görev Ekle"));

        left.add(new JLabel("Başlık"));
        left.add(txtTitle);
        left.add(Box.createVerticalStrut(8));

        left.add(new JLabel("Öncelik"));
        left.add(cmbPriority);
        left.add(Box.createVerticalStrut(8));

        left.add(new JLabel("Bitiş Tarihi (yyyy-MM-dd) (opsiyonel)"));
        left.add(txtDueDate);
        left.add(Box.createVerticalStrut(12));

        JPanel leftBtns = new JPanel(new GridLayout(2, 2, 8, 8));
        leftBtns.add(btnAdd);
        leftBtns.add(btnToggle);
        leftBtns.add(btnDelete);
        leftBtns.add(btnSave);

        left.add(leftBtns);

        // Sağ: liste
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Görevler"));

        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        right.add(new JScrollPane(taskList), BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);

        // solda genişlik
        left.setPreferredSize(new Dimension(300, 0));
    }

    private void wireEvents() {
        btnAdd.addActionListener(e -> addTask());
        btnDelete.addActionListener(e -> deleteSelected());
        btnToggle.addActionListener(e -> toggleSelected());
        btnSave.addActionListener(e -> saveToFile());

        txtSearch.getDocument().addDocumentListener((SimpleDocumentListener) e -> refreshList());
        cmbFilter.addActionListener(e -> refreshList());
    }

    private void addTask() {
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Başlık boş olamaz!");
            return;
        }

        String pr = (String) cmbPriority.getSelectedItem();
        String due = txtDueDate.getText().trim();

        Task t = new Task(nextId++, title, false, pr, due);
        tasks.add(t);

        txtTitle.setText("");
        txtDueDate.setText("");

        refreshList();
    }

    private void deleteSelected() {
        Task selected = taskList.getSelectedValue();
        if (selected == null) return;

        tasks.removeIf(t -> t.getId() == selected.getId());
        refreshList();
    }

    private void toggleSelected() {
        Task selected = taskList.getSelectedValue();
        if (selected == null) return;

        for (Task t : tasks) {
            if (t.getId() == selected.getId()) {
                t.setDone(!t.isDone());
                break;
            }
        }
        refreshList();
    }

    private void refreshList() {
        String q = txtSearch.getText().trim().toLowerCase();
        String filter = (String) cmbFilter.getSelectedItem();

        listModel.clear();
        for (Task t : tasks) {
            boolean matchesSearch = q.isEmpty() || t.getTitle().toLowerCase().contains(q);
            boolean matchesFilter =
                    "Tümü".equals(filter) ||
                    ("Aktif".equals(filter) && !t.isDone()) ||
                    ("Tamamlanan".equals(filter) && t.isDone());

            if (matchesSearch && matchesFilter) listModel.addElement(t);
        }
    }

    private void loadFromFile() {
        List<Task> loaded = storage.load();
        tasks.clear();
        tasks.addAll(loaded);

        // nextId hesapla
        int maxId = 0;
        for (Task t : tasks) maxId = Math.max(maxId, t.getId());
        nextId = maxId + 1;
    }

    private void saveToFile() {
        storage.save(tasks);
        JOptionPane.showMessageDialog(this, "Kaydedildi ✅");
    }

    // Küçük yardımcı: DocumentListener’ı lambda ile kullanmak için
    @FunctionalInterface
    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}
