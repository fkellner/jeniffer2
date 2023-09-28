package de.unituebingen.jeniffer2.main;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class LayoutManager {

    private DoubleProperty canvasWidthProperty = new SimpleDoubleProperty();
    private DoubleProperty canvasHeightProperty = new SimpleDoubleProperty();

    public DoubleProperty canvasWidthProperty() {
        return canvasWidthProperty;
    }

    public DoubleProperty canvasHeightProperty() {
        return canvasHeightProperty;
    }
}
