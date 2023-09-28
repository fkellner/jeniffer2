package de.unituebingen.jeniffer2.main.footer.statusbar;

import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class StatusBarPresenter implements Initializable {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressValue;

    @FXML
    private Label progressDescription;

    @Inject
    private StatusBarModel statusBarModel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
//        progressBar.translateXProperty().bind(layoutManager.tabHeaderLengthProperty());
        try {
            progressBar.progressProperty().bind(statusBarModel.getProgressProperty());
            progressDescription.textProperty().bind(statusBarModel.getProgressDescriptionProperty());
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
