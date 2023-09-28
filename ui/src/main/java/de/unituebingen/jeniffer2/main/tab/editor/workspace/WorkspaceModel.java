package de.unituebingen.jeniffer2.main.tab.editor.workspace;

import javafx.beans.property.*;

public class WorkspaceModel {

    private StringProperty fileNameProperty = new SimpleStringProperty();
    private DoubleProperty zoomFactorProperty = new SimpleDoubleProperty();
    private BooleanProperty zoomInProperty = new SimpleBooleanProperty();
    private BooleanProperty zoomOutProperty = new SimpleBooleanProperty();
    private BooleanProperty zoomAdaptProperty = new SimpleBooleanProperty();

    public StringProperty fileNameProperty() {
        return fileNameProperty;
    }

    public DoubleProperty zoomFactorProperty() {
        return zoomFactorProperty;
    }

    public BooleanProperty zoomInProperty() {
        return zoomInProperty;
    }

    public BooleanProperty zoomOutProperty() {
        return zoomOutProperty;
    }

    public BooleanProperty zoomAdaptProperty() {
        return zoomAdaptProperty;
    }
}
