package de.unituebingen.jeniffer2.main.modules.imagedata;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

public class ImageDataModel {

    private SimpleListProperty<Integer> posProperty = new SimpleListProperty<Integer>(
        FXCollections.observableArrayList(null, null)
    );

    private IntegerProperty sideLengthProperty = new SimpleIntegerProperty(9);
    
    private BooleanProperty imageUpdated = new SimpleBooleanProperty(false);

    public SimpleListProperty<Integer> posProperty() {
        return posProperty;
    }

    public IntegerProperty sideLengthPropery() {
        return sideLengthProperty;
    }

    public BooleanProperty imageUpdated() {
        return imageUpdated;
    }
}
