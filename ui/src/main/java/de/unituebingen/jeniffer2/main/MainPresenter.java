package de.unituebingen.jeniffer2.main;

import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.footer.statusbar.StatusBarView;
import de.unituebingen.jeniffer2.main.menubar.MenuBarView;
import de.unituebingen.jeniffer2.main.tab.editor.EditorView;
import de.unituebingen.jeniffer2.main.tab.library.LibraryView;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class MainPresenter implements Initializable {

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab libraryTab;

    @FXML
    private Tab editorTab;

    @FXML
    private BorderPane borderPane;

    @Inject
    private WorkflowManager workflowManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            LibraryView libraryView = new LibraryView();
            libraryTab.setContent(libraryView.getView());

            EditorView editorView = new EditorView();
            editorView.getViewAsync(editorTab::setContent);

            MenuBarView menuBarView = new MenuBarView();
            StatusBarView statusBarView = new StatusBarView();
            borderPane.setBottom(statusBarView.getView());
            borderPane.setTop(menuBarView.getView());
            workflowManager.interpolationExecutedProperty().addListener((observableValue, aBoolean, t1) -> {
                if (t1) {
                    tabPane.getSelectionModel().select(editorTab);
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
