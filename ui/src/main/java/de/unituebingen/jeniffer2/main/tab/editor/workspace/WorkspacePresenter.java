package de.unituebingen.jeniffer2.main.tab.editor.workspace;

import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.LayoutManager;
import de.unituebingen.jeniffer2.main.tab.editor.workspace.canvas.CanvasView;
import de.unituebingen.jeniffer2.main.tab.editor.workspace.informationbar.InformationBarView;
import de.unituebingen.jeniffer2.main.tab.editor.workspace.toolbar.ToolBarView;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class WorkspacePresenter implements Initializable {

    @FXML
    private BorderPane workspace;

    @FXML
    private Pane canvas;

    @Inject
    private WorkflowManager workflowManager;

    @Inject
    private LayoutManager layoutManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            layoutManager.canvasHeightProperty().bind(canvas.heightProperty());
            layoutManager.canvasWidthProperty().bind(canvas.widthProperty());
            ToolBarView toolBarView = new ToolBarView();
            InformationBarView informationBarView = new InformationBarView();
            informationBarView.toFront();
            informationBarView.getView().toFront();
            workspace.setBottom(informationBarView.getView());
            workspace.setTop(toolBarView.getView());

            workflowManager.interpolationExecutedProperty().addListener((observableValue, aBoolean, t1) -> {
                if (t1) {
                    createCanvasView();
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

    private void createCanvasView() {
        workflowManager.canvasLoadedProperty().set(false);
        canvas.getChildren().clear();
        CanvasView canvasView = new CanvasView();
        canvasView.getViewAsync(canvas.getChildren()::add);
    }
}
