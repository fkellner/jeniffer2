package de.unituebingen.jeniffer2.main.tab.library;

import de.unituebingen.jeniffer2.main.tab.library.thumbnail.ThumbnailView;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import javafx.scene.Parent;
import javafx.scene.layout.Pane;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PreviewService extends Service<Void> {

    private File file;
    private Pane pane;

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                File[] files = file.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isFile() && files[i].getName().toLowerCase().endsWith(".dng")) { //this line weeds out other directories/folders
                            Map<String, Object> context = new HashMap<>();
                            context.put("filePath", files[i].getAbsolutePath());
                            ThumbnailView view = new ThumbnailView(context::get);
                            addEntry(view.getView());
                        }
                    }
                }
                return null;
            }
        };
    }

    private void addEntry(Parent entry) {
        Platform.runLater(() -> pane.getChildren().add(entry));
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setPane(Pane pane) {
        this.pane = pane;
    }
}
