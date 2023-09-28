package de.unituebingen.jeniffer2.main.tab.editor.aside;

import de.unituebingen.jeniffer2.WorkflowManager;
// import de.unituebingen.jeniffer2.main.modules.histogram.HistogramView;
import de.unituebingen.jeniffer2.main.modules.imagedata.ImageDataView;
import de.unituebingen.jeniffer2.main.modules.metadata.MetadataView;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class AsidePresenter implements Initializable {

    @FXML
    private SplitPane content;

    @Inject
    private WorkflowManager workflowManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            ImageDataView imageDataView = new ImageDataView();
            content.getItems().add(imageDataView.getView());
            MetadataView metadataView = new MetadataView();
            metadataView.getViewAsync(content.getItems()::add);

            workflowManager.interpolationExecutedProperty().addListener((observableValue, aBoolean, t1) -> {
                if (t1) {
                    //HistogramView histogramView = new HistogramView();
                    //histogramView.getViewAsync(content.getItems()::add);
                    //content.getItems().add(histogramView.getView());
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
