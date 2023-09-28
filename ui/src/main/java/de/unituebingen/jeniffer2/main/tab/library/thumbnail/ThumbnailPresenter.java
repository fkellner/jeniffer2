package de.unituebingen.jeniffer2.main.tab.library.thumbnail;

import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.dng.reader.DNGReadException;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogView;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ThumbnailPresenter implements Initializable {

    @FXML
    private Button button;

    @FXML
    private ImageView imageView;

    @FXML
    private Label fileName;

    @Inject
    private WorkflowManager manager;

    @Inject
    private String filePath;

    private DNGFile tiffReader;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            tiffReader = new DNGFile(filePath);
            BufferedImage thumbnail = tiffReader.getThumbnailImage();
            fileName.setText(tiffReader.getFile().getName());
            Image fxImage = SwingFXUtils.toFXImage(thumbnail, null);

            thumbnail = null;
            System.gc();
            imageView.setImage(fxImage);
            throw new IOException();
        } catch (IOException e) {

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

        button.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                int clickCount = mouseEvent.getClickCount();
                if (clickCount == 1) {
                    manager.clickedImageProperty().set(tiffReader);
                } else {
                    manager.doubleClickedImageProperty().set(tiffReader);
                }
            }
        });
    }
}
