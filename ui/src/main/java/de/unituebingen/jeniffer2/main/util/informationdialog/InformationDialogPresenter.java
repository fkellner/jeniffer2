package de.unituebingen.jeniffer2.main.util.informationdialog;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import javax.inject.Inject;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class InformationDialogPresenter implements Initializable {

    @FXML
    private Label title;

    @FXML
    private Label message;

    @FXML
    private StackPane iconWrapper;

    @Inject
    private String titleKey;

    @Inject
    private String messageKey;

    @Inject
    private String type;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            if (type == "success") {
                iconWrapper.getChildren().clear();
                iconWrapper.getChildren().add(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CHECK_CIRCLE, "3em"));
                iconWrapper.getChildren().get(0).setStyle("-fx-fill: #00FF2A; height: 3em; width: 3em;");
            }
            title.setText(resourceBundle.containsKey(titleKey) ? resourceBundle.getString(titleKey) : "Usertext for " + titleKey + " (not found)");
            message.setText(resourceBundle.containsKey(messageKey) ? resourceBundle.getString(messageKey) : "Usertext for " + messageKey + " (not found)");
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
