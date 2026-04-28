package todoappfx_modern;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class EditTaskDialog {

    public static boolean show(Task task, java.util.List<String> categories) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Görevi Düzenle");

        ButtonType saveBtn = new ButtonType("Kaydet", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        TextField title = new TextField(task.getTitle());
        TextArea note = new TextArea(task.getNote());
        note.setPrefRowCount(3);

        ComboBox<Task.Priority> pr = new ComboBox<>();
        pr.getItems().addAll(Task.Priority.values());
        pr.setValue(task.getPriority());

        ComboBox<String> cat = new ComboBox<>();
        cat.getItems().addAll(categories);
        cat.setValue(task.getCategory());

        DatePicker dp = new DatePicker(task.getDueDate());

        TextField tags = new TextField(task.getTagsCsv());
        tags.setPromptText("#spor,#ödev");

        CheckBox done = new CheckBox("Tamamlandı");
        done.setSelected(task.isDone());

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.setPadding(new Insets(14));

        gp.add(new Label("Başlık"), 0,0); gp.add(title, 1,0);
        gp.add(new Label("Not"), 0,1); gp.add(note, 1,1);
        gp.add(new Label("Öncelik"), 0,2); gp.add(pr, 1,2);
        gp.add(new Label("Kategori"), 0,3); gp.add(cat, 1,3);
        gp.add(new Label("Tarih"), 0,4); gp.add(dp, 1,4);
        gp.add(new Label("Etiketler"), 0,5); gp.add(tags, 1,5);
        gp.add(done, 1,6);

        dialog.getDialogPane().setContent(gp);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        okButton.disableProperty().bind(title.textProperty().isEmpty());

        var result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            task.setTitle(title.getText().trim());
            task.setNote(note.getText());
            task.setPriority(pr.getValue());
            task.setCategory(cat.getValue());
            task.setDueDate(dp.getValue());
            task.setTagsCsv(tags.getText());
            task.setDone(done.isSelected());
            return true;
        }
        return false;
    }
}
