package de.unituebingen.jeniffer2.main.util.saveparameterdialog;

import javafx.beans.property.*;

public class SaveParameterDialogModel {

    ObjectProperty<Integer> colorDepthProperty = new SimpleObjectProperty<>();
    DoubleProperty compression = new SimpleDoubleProperty();

    public ObjectProperty<Integer> colorDepthPropertyProperty() {
        return colorDepthProperty;
    }

    public DoubleProperty compressionProperty() {
        return compression;
    }
}
