package de.unituebingen.jeniffer2;

import de.unituebingen.dng.reader.DNGFile;
import javafx.beans.property.*;
import javafx.scene.control.TreeItem;

public class WorkflowManager {

    private ObjectProperty<TreeItem<String>> selectedDirectory = new SimpleObjectProperty<>();
    private ObjectProperty<DNGFile> clickedImageProperty = new SimpleObjectProperty<>();
    private ObjectProperty<DNGFile> doubleClickedImageProperty = new SimpleObjectProperty<>();
    private BooleanProperty interpolationExecutedProperty = new SimpleBooleanProperty();
    private BooleanProperty canvasLoadedProperty = new SimpleBooleanProperty();

    public ObjectProperty<DNGFile> clickedImageProperty() {
        return clickedImageProperty;
    }

    public ObjectProperty<DNGFile> doubleClickedImageProperty() {
        return doubleClickedImageProperty;
    }

    public ObjectProperty<TreeItem<String>> selectedDirectoryProperty() {
        return selectedDirectory;
    }

    public BooleanProperty interpolationExecutedProperty() {
        return interpolationExecutedProperty;
    }

    public BooleanProperty canvasLoadedProperty() {
        return canvasLoadedProperty;
    }

}
