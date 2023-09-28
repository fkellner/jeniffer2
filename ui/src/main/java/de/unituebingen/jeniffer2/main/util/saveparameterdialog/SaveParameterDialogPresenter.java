package de.unituebingen.jeniffer2.main.util.saveparameterdialog;

import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import de.unituebingen.jeniffer2.util.PercentConverter;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class SaveParameterDialogPresenter implements Initializable {

    @FXML
    private Separator separator;

    @FXML
    private ComboBox<Integer> colorDepth;

    @FXML
    private Label compressionLabel;

    @FXML
    private Slider compressionSlider;

    @FXML
    private TextField percent;

    @Inject
    private SaveParameterDialogModel model;

    @Inject
    private String type;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            model.colorDepthProperty.bind(colorDepth.valueProperty());
            model.compressionProperty().bind(compressionSlider.valueProperty());
            percent.textProperty().bindBidirectional(compressionSlider.valueProperty(), new PercentConverter());

            colorDepth.getItems().addAll(8, 16);
            colorDepth.setValue(16);
            compressionSlider.setValue(1);


            BooleanBinding typeBinding = Bindings.equal("jpg", new SimpleStringProperty(type));
            colorDepth.disableProperty().bind(typeBinding);
            separator.visibleProperty().bind(typeBinding);
            separator.managedProperty().bind(typeBinding);
            compressionLabel.visibleProperty().bind(typeBinding);
            compressionLabel.managedProperty().bind(typeBinding);
            compressionSlider.visibleProperty().bind(typeBinding);
            compressionSlider.managedProperty().bind(typeBinding);
            percent.visibleProperty().bind(typeBinding);
            percent.managedProperty().bind(typeBinding);

            if (type == "jpg") {
                colorDepth.setValue(8);
            }
        } catch (Exception e) {
            // catch-all to get stacktrace
            System.out.println(e);
            System.out.println(e.getMessage());
            e.printStackTrace();
            // show error message
            InformationDialogHelper.openErrorDialog(
                "error.title.dngprocessing", "error.message.dngprocessing");
        }
    }
}
