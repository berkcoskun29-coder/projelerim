package todoapp;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // proje klasörüne todo.txt kaydeder
            FileStorage storage = new FileStorage("todo.txt");
            new TodoUI(storage).setVisible(true);
        });
    }
}
