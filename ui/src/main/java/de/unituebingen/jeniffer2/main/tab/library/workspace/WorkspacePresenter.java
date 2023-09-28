package de.unituebingen.jeniffer2.main.tab.library.workspace;

import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.modules.navigation.NavigationTreeItem;
import de.unituebingen.jeniffer2.main.tab.library.PreviewService;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.TilePane;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class WorkspacePresenter implements Initializable {

    @FXML
    private TilePane tilePane;

    @Inject
    private WorkflowManager workflowManager;

    @Inject
    private PreviewService service;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            service.setPane(tilePane);

            workflowManager.selectedDirectoryProperty().addListener((observable, oldValue, newValue) -> {
                tilePane.getChildren().clear();
                if (newValue instanceof NavigationTreeItem) {
                    NavigationTreeItem selectedItem = (NavigationTreeItem) newValue;
                    service.setFile(selectedItem.getFile());
                    service.restart();
                }
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
