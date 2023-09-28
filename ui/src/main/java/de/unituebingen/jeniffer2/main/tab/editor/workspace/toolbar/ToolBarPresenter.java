package de.unituebingen.jeniffer2.main.tab.editor.workspace.toolbar;

import de.unituebingen.dng.DNGProcessor;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.footer.statusbar.StatusBarModel;
import de.unituebingen.jeniffer2.main.tab.editor.workspace.WorkspaceModel;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import de.unituebingen.jeniffer2.util.PercentConverterRounded;
import de.unituebingen.jeniffer2.util.PipelineConfiguration;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class ToolBarPresenter implements Initializable {

    @FXML
    private Button zoomAdaptButton;

    @FXML
    private Button zoomOutButton;

    @FXML
    private Button zoomInButton;

    @FXML
    private Button computeButton;

    @FXML
    private Button computingButton;

    @FXML
    private TextField zoomFactorInput;

    @FXML
    private ComboBox<InterpolationMethod> interpolationMethods;

    @FXML
    private ComboBox<AccelerationStrategy> accelerationStrategies;

    @FXML
    private ComboBox<String> subSteps;

    // @FXML
    // private Label subStepsLabel; 

    @Inject
    private WorkspaceModel workspaceModel;

    @Inject
    private WorkflowManager workflowManager;

    @Inject
    private ApplicationData applicationData;

    @Inject StatusBarModel statusBarModel;

    private boolean addedListeners = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // set up zoom controls
            zoomAdaptButton.disableProperty().bind(workflowManager.canvasLoadedProperty().not());
            zoomOutButton.disableProperty().bind(workflowManager.canvasLoadedProperty().not());
            zoomInButton.disableProperty().bind(workflowManager.canvasLoadedProperty().not());
            zoomFactorInput.disableProperty().bind(workflowManager.canvasLoadedProperty().not());

            zoomFactorInput.textProperty().addListener((ob, o, n) -> {
                zoomFactorInput.setPrefWidth(
                        TextUtils.computeTextWidth(zoomFactorInput.getFont(), zoomFactorInput.getText(), 0.0D) + 16);
            });

            zoomFactorInput.textProperty().bindBidirectional(workspaceModel.zoomFactorProperty(),
                    new PercentConverterRounded());

            
            // initializing compute button
            computeButton.setVisible(false);
            computingButton.setVisible(false);
            statusBarModel.getProgressProperty().addListener((obs, old, nu) -> {
                if(nu.intValue() == 0) {
                    computingButton.setVisible(false);
                } else {
                    computingButton.setVisible(true);
                }
            });
            interpolationMethods.setCellFactory(p -> {
                return new ListCell<>() {         
                    @Override protected void updateItem(InterpolationMethod item, boolean empty) {
                        super.updateItem(item, empty);
           
                        if (item == null || empty) return;
                        setText(item.getLabel());
                        PipelineConfiguration config = new PipelineConfiguration(
                            item, 
                            accelerationStrategies.getValue(),
                            "");
                        if(interpolationMethods.getValue() == item) {
                            setStyle("-fx-background-color: lightblue");
                        } else if(applicationData.getConfigurations().get(config) == null) {
                            setStyle("-fx-background-color: lightgray");
                        } else {
                            setStyle("-fx-background-color: lightgreen");
                        }                  

                    }
                };
            });
            subSteps.setCellFactory(p -> {
                return new ListCell<>() {         
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
           
                        if (item == null || empty) return;
                        setText(item);
                        PipelineConfiguration config = new PipelineConfiguration(
                            interpolationMethods.getValue(), 
                            accelerationStrategies.getValue(),
                            item);
                        if(subSteps.getValue() == item) {
                            setStyle("-fx-background-color: lightblue");
                        } else if(applicationData.getConfigurations().get(config) == null) {
                            setStyle("-fx-background-color: lightgray");
                        } else {
                            setStyle("-fx-background-color: lightgreen");
                        }                  

                    }
                };
            });

            // initializing correct values
            workflowManager.canvasLoadedProperty().addListener((obs, old, nu) -> {
                if(!nu) {
                    // remove listeners so we can reset values in peace
                    interpolationMethods.valueProperty().removeListener(interpolationListener);
                    accelerationStrategies.valueProperty().removeListener(accelerationListener);
                    subSteps.valueProperty().removeListener(substepListener);
                    addedListeners = false;
                    return;
                }
                if(addedListeners) return;
                // set up config controls
                // initializing interpolation method
                interpolationMethods.getItems().clear();
                interpolationMethods.getItems()
                    .addAll(InterpolationMethod.values());
                interpolationMethods.setValue(applicationData.getInterpolationMethod());
                interpolationMethods.valueProperty().addListener(interpolationListener);

                // initializing acceleration strategy
                accelerationStrategies.getItems().addAll(FXCollections.observableArrayList(AccelerationStrategy.values()));
                accelerationStrategies.setConverter(accelerationConverter);
                accelerationStrategies.setValue(applicationData.getAccelerationStrategy());
                accelerationStrategies.valueProperty().addListener(accelerationListener);
                // initializing substeps
                updateAvailableSubsteps();
                subSteps.valueProperty().addListener(substepListener);
                addedListeners = true;

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
    // listeners
    private ChangeListener<InterpolationMethod> interpolationListener = (observableValue, interpolationMethod, t1) -> {
        applicationData.setInterpolationMethod(t1);
        updateAvailableSubsteps();
        updateOrOfferComputation();
    };

    private ChangeListener<AccelerationStrategy> accelerationListener = (observableValue, interpolationMethod, t1) -> {
        applicationData.setAccelerationStrategy(t1);
        // force re-render of list cells
        interpolationMethods.valueProperty().removeListener(interpolationListener);
        interpolationMethods.getItems().clear();
        interpolationMethods.getItems()
            .addAll(InterpolationMethod.values());
        interpolationMethods.setValue(applicationData.getInterpolationMethod());
        interpolationMethods.valueProperty().addListener(interpolationListener);

        updateAvailableSubsteps();
        updateOrOfferComputation();
    };

    private ChangeListener<String> substepListener = (o, i, t1) -> {
        applicationData.setSubstep(t1);
        updateOrOfferComputation();
    };

    private StringConverter<AccelerationStrategy> accelerationConverter = new StringConverter<>() {

        @Override
        public String toString(AccelerationStrategy accelerationStrategy) {
            return accelerationStrategy.getLabel();
        }

        @Override
        public AccelerationStrategy fromString(String s) {
            for(AccelerationStrategy a: AccelerationStrategy.values()) {
                if(a.getLabel() == s) return a;
            }
            // safe default
            System.out.println("Error translating back Acceleration Strategy!");
            return AccelerationStrategy.MULTITHREADING;
        }
    };



    // Zoom controls

    public void onZoomIn() {
        workspaceModel.zoomInProperty().set(!workspaceModel.zoomInProperty().get());
    }

    public void onZoomOut() {
        workspaceModel.zoomOutProperty().set(!workspaceModel.zoomOutProperty().get());
    }

    public void onZoomAdapt() {
        workspaceModel.zoomAdaptProperty().set(!workspaceModel.zoomAdaptProperty().get());
    }

    // config controls
    private void updateAvailableSubsteps() {
        String oldSubstep = applicationData.getSubStep();
        subSteps.getItems().clear();
        String[] availableSubsteps = DNGProcessor.getAvailableSubsteps(
                applicationData.getInterpolationMethod(),
                applicationData.getAccelerationStrategy());
        subSteps.getItems().addAll(availableSubsteps);
        // subStepsLabel.setVisible(availableSubsteps.length > 0);
        subSteps.setVisible(availableSubsteps.length > 0);
        subSteps.getItems().add("");
        if (oldSubstep != null && subSteps.getItems().contains(oldSubstep)) {
            subSteps.setValue(oldSubstep);
        } else {
            subSteps.setValue("");
        }
    }
    
    public void updateImage() {
        Platform.runLater(() -> {
            computeButton.setVisible(false);
        });
        applicationData.triggerImageUpdate();
    }

    private void updateOrOfferComputation() {
        if (applicationData.isCurrentConfigComputed()) {
            computeButton.setVisible(false);
            updateImage();
        } else {
            computeButton.setVisible(true);
        }
    }
}
