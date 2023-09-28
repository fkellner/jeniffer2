package de.unituebingen.jeniffer2.main.tab.editor;


import de.unituebingen.jeniffer2.main.tab.editor.aside.AsideView;
import de.unituebingen.jeniffer2.main.tab.editor.workspace.WorkspaceView;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;

import java.net.URL;
import java.util.ResourceBundle;

public class EditorPresenter implements Initializable {

    @FXML
    private SplitPane center;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            AsideView asideView = new AsideView();
            WorkspaceView workspaceView = new WorkspaceView();
            center.getItems().addAll(asideView.getView(), workspaceView.getView());
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
