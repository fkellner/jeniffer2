package de.unituebingen.jeniffer2.main.menubar;

import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogView;
import de.unituebingen.jeniffer2.main.util.saveparameterdialog.SaveParameterDialogModel;
import de.unituebingen.jeniffer2.main.util.saveparameterdialog.SaveParameterDialogView;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MenuBarPresenter implements Initializable {

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem saveAsMenuItem;

    @Inject
    private WorkflowManager workflowManager;

    @Inject
    private ApplicationData applicationData;

    @Inject
    private SaveParameterDialogModel saveParameterDialogModel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            saveAsMenuItem.disableProperty().bind(workflowManager.canvasLoadedProperty().not());
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

    public void onOpen() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter dngFilter = new FileChooser.ExtensionFilter("DNG", "*.dng");
        fileChooser.getExtensionFilters().addAll(dngFilter);
        fileChooser.setTitle("Open DNG File");
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                DNGFile tiffReader = new DNGFile(file);
                workflowManager.clickedImageProperty().set(tiffReader);
                workflowManager.doubleClickedImageProperty().set(tiffReader);
            } catch (IOException e) {
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
    }

    public void onSaveAs() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter tiffFilter = new FileChooser.ExtensionFilter("TIFF", "*.tiff", "*.tif");
        FileChooser.ExtensionFilter jpgFilter = new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg");
        FileChooser.ExtensionFilter pngFilter = new FileChooser.ExtensionFilter("PNG", "*.png");
        fileChooser.getExtensionFilters().addAll(tiffFilter, jpgFilter, pngFilter);
        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            FileChooser.ExtensionFilter selectedExtensionFilter = fileChooser.getSelectedExtensionFilter();
            Map<String, String> context = new HashMap<>();
            if (selectedExtensionFilter.equals(tiffFilter)) {
                context.put("type", "tiff");
            } else if (selectedExtensionFilter.equals(pngFilter)) {
                context.put("type", "png");
            } else {
                context.put("type", "jpg");
            }
            SaveParameterDialogView saveParameterDialogView = new SaveParameterDialogView(context::get);
            DialogPane dialog = (DialogPane) saveParameterDialogView.getView();
            Dialog<ButtonType> dia = new Dialog<>();
            dia.setDialogPane(dialog);
            dia.showAndWait()
                    .filter(response -> response == ButtonType.APPLY)
                    .ifPresent(response -> save(context.get("type"), file));
        }
    }

    private void save(String type, File file) {
        ObjectProperty<Integer> colorDepthProperty = saveParameterDialogModel.colorDepthPropertyProperty();
        try {
            if (type == "tiff") {
                if (colorDepthProperty.getValue() == 8) {
                    applicationData.saveAsTiff8(file);
                } else {
                    applicationData.saveAsTiff16(file);
                }
            } else if (type == "png") {
                if (colorDepthProperty.getValue() == 8) {
                    applicationData.saveAsPNG8(file);
                } else {
                    applicationData.saveAsPNG16(file);
                }
            } else {
                applicationData.saveAsJPEG(file, saveParameterDialogModel.compressionProperty().floatValue());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();

            Map<String, String> context = new HashMap<>();
            context.put("messageKey", "error.message.filesaving"); // defined in the informationdialog properties
            context.put("titleKey", "error.title.filesaving");
            InformationDialogView informationDialogView = new InformationDialogView(context::get);
            DialogPane dialog = (DialogPane) informationDialogView.getView();
            Dialog<ButtonType> dia = new Dialog<>();
            dia.setDialogPane(dialog);
            dia.showAndWait();
        }
        Map<String, String> context = new HashMap<>();
        context.put("messageKey", "success.message.filesaving"); // defined in the informationdialog properties
        context.put("titleKey", "success.title.filesaving");
        context.put("type", "success");
        InformationDialogView informationDialogView = new InformationDialogView(context::get);
        DialogPane dialog = (DialogPane) informationDialogView.getView();
        Dialog<ButtonType> dia = new Dialog<>();
        dia.setDialogPane(dialog);
        dia.showAndWait();
    }
}
