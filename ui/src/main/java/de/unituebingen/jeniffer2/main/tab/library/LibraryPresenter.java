package de.unituebingen.jeniffer2.main.tab.library;

import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.tab.library.aside.AsideView;
import de.unituebingen.jeniffer2.main.tab.library.interpolationdialog.InterpolationDialogView;
import de.unituebingen.jeniffer2.main.tab.library.workspace.WorkspaceView;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.SplitPane;

import javax.inject.Inject;
import java.io.EOFException;
import java.net.URL;
import java.util.ResourceBundle;

public class LibraryPresenter implements Initializable {

    @FXML
    private SplitPane center;

    @Inject
    private WorkflowManager workflowManager;

    @Inject
    private ApplicationData applicationData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            AsideView asideView = new AsideView();
            WorkspaceView workspaceView = new WorkspaceView();
            center.getItems().addAll(asideView.getView(), workspaceView.getView());

            workflowManager.doubleClickedImageProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    applicationData.setTiffReader(newValue);
                    try {
                        if (newValue.isPhotometricInterpretationValid() && newValue.isPlanarConfigurationValid()) {
                            InterpolationDialogView dialogView = new InterpolationDialogView();
                            DialogPane dialog = (DialogPane) dialogView.getView();
                            Dialog<ButtonType> dia = new Dialog<>();
                            dia.setDialogPane(dialog);
                            dia.showAndWait()
                                    .filter(response -> response == ButtonType.APPLY)
                                    .ifPresent(response -> workflowManager.interpolationExecutedProperty().set(true));
                        } else {
                            InformationDialogHelper.openInfoDialog("info.title.photometric", "info.message.photometric");
                        }
                    } catch (EOFException e) {
                        e.printStackTrace();
                    } catch (DNGReadException e) {
                        e.printStackTrace();
                    }
                }
                workflowManager.doubleClickedImageProperty().set(null);
            });
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
