package de.unituebingen.jeniffer2.main.modules.metadata;

import de.unituebingen.dng.reader.*;
import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import javax.inject.Inject;
import java.io.EOFException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MetadataPresenter implements Initializable {

    @FXML
    private TableView<ImageFileDirectoryEntry> exifTable;

    @FXML
    private TableColumn<ImageFileDirectoryEntry, String> exifTableColumn1;

    @FXML
    private TableColumn<ImageFileDirectoryEntry, String> exifTableColumn2;

    @FXML
    private TableView<ImageFileDirectoryEntry> baselineTable;

    @FXML
    private TableColumn<ImageFileDirectoryEntry, String> baselineTableColumn1;

    @FXML
    private TableColumn<ImageFileDirectoryEntry, String> baselineTableColumn2;

    @FXML
    private TableView<ImageFileDirectoryEntry> hrTable;

    @FXML
    private TableColumn<ImageFileDirectoryEntry, String> hrTableColumn1;

    @FXML
    private TableColumn<ImageFileDirectoryEntry, String> hrTableColumn2;


    @Inject
    private ApplicationData applicationData;

    @Inject
    private WorkflowManager workflowManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            workflowManager.clickedImageProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    applicationData.setTiffReader(newValue);
                    loadContent();
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

    private void loadContent() {
        DNGFile tiffReader = applicationData.getTiffReader();
        try {
            ImageFileDirectory exifIFD = tiffReader.getExifImageFileDirectory();
            Map<Integer, ImageFileDirectoryEntry> exifIFDEntries = exifIFD.getImageFileDirectoryEntries();
            constructTable(exifIFDEntries, exifTable, exifTableColumn1, exifTableColumn2);
            ImageFileDirectory baselineIFD = tiffReader.get0thImageFileDirectory();
            Map<Integer, ImageFileDirectoryEntry> baselineIFDEntries = baselineIFD.getImageFileDirectoryEntries();
            constructTable(baselineIFDEntries, baselineTable, baselineTableColumn1, baselineTableColumn2);
            ImageFileDirectory hrIFD = tiffReader.getRAWImageFileDirectory();
            Map<Integer, ImageFileDirectoryEntry> hrIFDEntries = hrIFD.getImageFileDirectoryEntries();
            constructTable(hrIFDEntries, hrTable, hrTableColumn1, hrTableColumn2);
        } catch (EOFException e) {
            Map<String, String> context = new HashMap<>();
            context.put("message", e.getMessage());
            InformationDialogView informationDialogView = new InformationDialogView(context::get);
            DialogPane dialog = (DialogPane) informationDialogView.getView();
            Dialog<ButtonType> dia = new Dialog<>();
            dia.setDialogPane(dialog);
            dia.showAndWait();
        } catch (DNGReadException e) {
            Map<String, String> context = new HashMap<>();
            context.put("message", e.getMessage());
            context.put("title", "An error occurred reading the DNG file");
            InformationDialogView informationDialogView = new InformationDialogView(context::get);
            DialogPane dialog = (DialogPane) informationDialogView.getView();
            Dialog<ButtonType> dia = new Dialog<>();
            dia.setDialogPane(dialog);
            dia.showAndWait();
        }
    }

    private void constructTable(Map<Integer, ImageFileDirectoryEntry> entries, TableView<ImageFileDirectoryEntry> table,
                                TableColumn<ImageFileDirectoryEntry, String> column1, TableColumn<ImageFileDirectoryEntry, String> column2) {
        ObservableList<ImageFileDirectoryEntry> data = FXCollections.observableArrayList(entries.values());
        table.setItems(data);
        column1.setCellValueFactory(p -> {
            DNGTag tag = p.getValue().getTag();
            String tagString = tag == null ? String.valueOf(p.getValue().getTagId()) : tag.getLabel();
            return new SimpleStringProperty(tagString);
        });
        column2.setCellValueFactory(p -> {
            return new SimpleStringProperty(p.getValue().valuesAsString());
        });
    }
}
