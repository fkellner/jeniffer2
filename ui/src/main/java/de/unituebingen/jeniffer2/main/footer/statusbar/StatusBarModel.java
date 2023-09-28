package de.unituebingen.jeniffer2.main.footer.statusbar;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StatusBarModel {

    private DoubleProperty progressProperty = new SimpleDoubleProperty();
    private StringProperty progressDescriptionProperty = new SimpleStringProperty();

    public DoubleProperty getProgressProperty() {
        return progressProperty;
    }

    public StringProperty getProgressDescriptionProperty() {
        return progressDescriptionProperty;
    }
}
