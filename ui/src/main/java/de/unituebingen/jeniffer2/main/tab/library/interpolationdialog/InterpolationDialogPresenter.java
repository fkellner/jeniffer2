package de.unituebingen.jeniffer2.main.tab.library.interpolationdialog;

import de.unituebingen.dng.DNGProcessor;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogView;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class InterpolationDialogPresenter implements Initializable {

    @FXML
    private ImageView imageView;

    @FXML
    private ComboBox<InterpolationMethod> interpolationMethods;

    @FXML
    private ComboBox<AccelerationStrategy> accelerationStrategies;

    @FXML
    private ComboBox<String> subSteps;

    @FXML
    private Label subStepsLabel; 

    @Inject
    private ApplicationData applicationData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // initializing interpolation method
            interpolationMethods.getItems()
                    .addAll(FXCollections.observableArrayList(InterpolationMethod.values()));
            interpolationMethods.setConverter(new StringConverter<>() {

                @Override
                public String toString(InterpolationMethod interpolationMethod) {
                    return interpolationMethod.getLabel();
                }

                @Override
                public InterpolationMethod fromString(String s) {
                    for(InterpolationMethod m : InterpolationMethod.values()) {
                        if(m.getLabel() == s) return m;
                    }
                    return null;
                }
            });

            interpolationMethods.setValue(applicationData.getInterpolationMethod());
            interpolationMethods.valueProperty().addListener((observableValue, interpolationMethod, t1) -> {
                applicationData.setInterpolationMethod(t1);
                updateAvailableSubsteps();
            });

            // initializing acceleration strategy
            accelerationStrategies.getItems().addAll(FXCollections.observableArrayList(AccelerationStrategy.values()));
            accelerationStrategies.setConverter(new StringConverter<>() {

                @Override
                public String toString(AccelerationStrategy accelerationStrategy) {
                    return accelerationStrategy.getLabel();
                }

                @Override
                public AccelerationStrategy fromString(String s) {
                    for(AccelerationStrategy a : AccelerationStrategy.values()) {
                        if(a.getLabel() == s) return a;
                    }
                    // safe default
                    System.out.println("Error translating back Acceleration Strategy!");
                    return AccelerationStrategy.MULTITHREADING;
                }
            });
            accelerationStrategies.setValue(applicationData.getAccelerationStrategy());
            accelerationStrategies.valueProperty().addListener((observableValue, interpolationMethod, t1) -> {
                applicationData.setAccelerationStrategy(t1);
                updateAvailableSubsteps();
            });
            // initializing substeps
            updateAvailableSubsteps();
            subSteps.valueProperty().addListener((o, i, t1) -> {
                applicationData.setSubstep(t1);
            });

            try {
                BufferedImage thumbnail = applicationData.getTiffReader().getThumbnailImage();
                Image fxImage = SwingFXUtils.toFXImage(thumbnail, null);
                imageView.setImage(fxImage);
                imageView.setFitHeight(thumbnail.getHeight());
            } catch (IOException e) {
                Map<String, String> context = new HashMap<>();
                context.put("message", e.getMessage());
                context.put("title", "An error occurred reading the DNG file");
                InformationDialogView informationDialogView = new InformationDialogView(context::get);
                DialogPane dialog = (DialogPane) informationDialogView.getView();
                Dialog<ButtonType> dia = new Dialog<>();
                dia.setDialogPane(dialog);
                dia.showAndWait();
            } catch (Exception e) {
                Map<String, String> context = new HashMap<>();
                context.put("message", e.getMessage());
                context.put("title", "An error occurred reading the DNG file");
                InformationDialogView informationDialogView = new InformationDialogView(context::get);
                DialogPane dialog = (DialogPane) informationDialogView.getView();
                Dialog<ButtonType> dia = new Dialog<>();
                dia.setDialogPane(dialog);
                dia.showAndWait();
            }
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
    
    private void updateAvailableSubsteps() {        
        String oldSubstep = applicationData.getSubStep();
        subSteps.getItems().clear();
        String[] availableSubsteps = DNGProcessor.getAvailableSubsteps(
            applicationData.getInterpolationMethod(),
            applicationData.getAccelerationStrategy());
        subSteps.getItems().addAll(availableSubsteps);
        subStepsLabel.setVisible(availableSubsteps.length > 0);
        subSteps.setVisible(availableSubsteps.length > 0);
        subSteps.getItems().add("");
        if (oldSubstep != null && subSteps.getItems().contains(oldSubstep)) {
            subSteps.setValue(oldSubstep);
        } else {
            subSteps.setValue("");
        }
    }
}
