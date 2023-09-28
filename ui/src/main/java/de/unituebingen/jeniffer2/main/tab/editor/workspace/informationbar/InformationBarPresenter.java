package de.unituebingen.jeniffer2.main.tab.editor.workspace.informationbar;

import de.unituebingen.jeniffer2.main.tab.editor.workspace.WorkspaceModel;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class InformationBarPresenter implements Initializable {

    @FXML
    private Label fileName;

    @Inject
    private WorkspaceModel workspaceModel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            fileName.textProperty().bind(workspaceModel.fileNameProperty());
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
