package de.unituebingen.jeniffer2.main.tab.editor.workspace.canvas;

import de.unituebingen.dng.reader.DNGFile;
import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.LayoutManager;
import de.unituebingen.jeniffer2.main.footer.statusbar.StatusBarModel;
import de.unituebingen.jeniffer2.main.modules.imagedata.ImageDataModel;
import de.unituebingen.jeniffer2.main.tab.editor.workspace.WorkspaceModel;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class CanvasPresenter implements Initializable {

    private static final double PADDING = 16;
    private static boolean addedListener = false;

    private double orgSceneX;
    private double orgSceneY;
    private double orgTranslateX;
    private double orgTranslateY;
    private int height;
    private int width;

    private DoubleProperty zoomFactorProperty = new SimpleDoubleProperty();
    private DoubleProperty zoomTranslationXProperty = new SimpleDoubleProperty();

    @FXML
    private ImageView imageView;

    @FXML
    private Pane wrapper;

    @Inject
    private ApplicationData applicationData;

    @Inject
    private LayoutManager layoutManager;

    @Inject
    private WorkspaceModel workspaceModel;

    @Inject
    private StatusBarModel statusBarModel;

    @Inject
    private ImageDataModel imageDataModel;

    @Inject
    private WorkflowManager workflowManager;

    private ResourceBundle resourceBundle;
    private String fileName;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        if (addedListener == false) {
            workspaceModel.zoomInProperty().addListener((observableValue, aBoolean, t1) -> zoom(1));
            workspaceModel.zoomOutProperty().addListener((observableValue, aBoolean, t1) -> zoom(-1));
            workspaceModel.zoomAdaptProperty().addListener((observableValue, aBoolean, t1) -> {
                zoomFactorProperty.set(getZoomFactorByCanvasSize(height));
                imageView.setTranslateX(0);
                imageView.setTranslateY(0);
            });
            addedListener = true;
        }

        zoomFactorProperty.bindBidirectional(workspaceModel.zoomFactorProperty());

        DNGFile tiffReader = applicationData.getTiffReader();
        fileName = tiffReader.getFile().getName();
        try {
            Platform.runLater(() -> {
                statusBarModel.getProgressProperty().set(ProgressBar.INDETERMINATE_PROGRESS);
                statusBarModel.getProgressDescriptionProperty().set(resourceBundle.getString("processing"));
            });
            Image fxImage = applicationData.getImage();
            width = (int)fxImage.getWidth();
            height = (int)fxImage.getHeight();
            zoomFactorProperty.setValue(getZoomFactorByCanvasSize(height));

            imageView.setImage(fxImage);
            imageView.fitWidthProperty().bind(zoomFactorProperty.multiply(width));
            imageView.fitHeightProperty().bind(zoomFactorProperty.multiply(height));
            imageView.setCursor(Cursor.MOVE);
            imageView.setSmooth(false);

            Platform.runLater(() -> {
                workspaceModel.fileNameProperty().set(
                        fileName + " (" + applicationData.getCurrentConfig() + ")");
                statusBarModel.getProgressProperty().set(0);
                statusBarModel.getProgressDescriptionProperty().set(resourceBundle.getString("ready"));
                workflowManager.interpolationExecutedProperty().setValue(false);
                workflowManager.canvasLoadedProperty().set(true);
                imageDataModel.imageUpdated().set(!imageDataModel.imageUpdated().get());
            });

            wrapper.translateXProperty()
                    .bind(((layoutManager.canvasWidthProperty().subtract(imageView.fitWidthProperty())).divide(2))
                            .add(zoomTranslationXProperty));
            wrapper.translateYProperty()
                    .bind((layoutManager.canvasHeightProperty().subtract(imageView.fitHeightProperty())).divide(2));

        } catch (Exception e) {
            // catch-all to get stacktrace
            System.out.println(e);
            System.out.println(e.getMessage());
            e.printStackTrace();
            // show error message
            InformationDialogHelper.openErrorDialog(
                    "error.title.dngprocessing", "error.message.dngprocessing");
            Platform.runLater(() -> {
                statusBarModel.getProgressProperty().set(0);
                statusBarModel.getProgressDescriptionProperty().set(resourceBundle.getString("ready"));
                workflowManager.interpolationExecutedProperty().setValue(false);
                workflowManager.canvasLoadedProperty().set(false);
            });
        }
        // update image if config is changed
        applicationData.getUpdateImage().addListener((obs, old, n) -> {
            if (old != n)
                updateImage();
        });
    }
    
    private void updateImage() {        
        Platform.runLater(() -> {
            statusBarModel.getProgressProperty().set(ProgressBar.INDETERMINATE_PROGRESS);
            statusBarModel.getProgressDescriptionProperty().set(resourceBundle.getString("processing"));
        });
        Task<Void> task = new Task<Void>() {
            @Override public Void call() {
                try {    
                    imageView.setImage(applicationData.getImage());
                                            
                    Platform.runLater(() -> {
                        workspaceModel.fileNameProperty().set(
                                fileName + " (" + applicationData.getCurrentConfig() + ")");
                        statusBarModel.getProgressProperty().set(0);
                        statusBarModel.getProgressDescriptionProperty().set(resourceBundle.getString("ready"));
                        imageDataModel.imageUpdated().set(!imageDataModel.imageUpdated().get());
                    });
                } catch (Exception e) {
                    // catch-all to get stacktrace
                    System.out.println(e);
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    // show error message
                    InformationDialogHelper.openErrorDialog(
                            "error.title.dngprocessing", "error.message.dngprocessing");
                    Platform.runLater(() -> {
                        statusBarModel.getProgressProperty().set(0);
                        statusBarModel.getProgressDescriptionProperty().set(resourceBundle.getString("ready"));
                    });
                }
                return null;
            }
        };
        new Thread(task).start();
        
    }

    private double getZoomFactorByCanvasSize(long imageLength) {
        double canvasHeight = layoutManager.canvasHeightProperty().doubleValue();
        return round((canvasHeight - 2 * PADDING) / imageLength, 2);
    }

    public void onMouseMoved(MouseEvent event) {
        int x = (int) (event.getX() / zoomFactorProperty.get());
        int y = (int) (event.getY() / zoomFactorProperty.get());
        imageDataModel.posProperty().set(0, x);
        imageDataModel.posProperty().set(1, y);
    }

    public void onMouseExit() {
        imageDataModel.posProperty().set(0, null);
        imageDataModel.posProperty().set(1, null);
    }

    public void onMousePressed(MouseEvent event) {
        orgSceneX = event.getSceneX();
        orgSceneY = event.getSceneY();
        orgTranslateX = ((ImageView) (event.getSource())).getTranslateX();
        orgTranslateY = ((ImageView) (event.getSource())).getTranslateY();
    }

    public void onMouseDragged(MouseEvent event) {
        double offsetX = event.getSceneX() - orgSceneX;
        double offsetY = event.getSceneY() - orgSceneY;
        double newTranslateX = orgTranslateX + offsetX;
        double newTranslateY = orgTranslateY + offsetY;

        ((ImageView) (event.getSource())).setTranslateX(newTranslateX);
        ((ImageView) (event.getSource())).setTranslateY(newTranslateY);
    }

    public void onScroll(ScrollEvent event) {
        zoom(event.getDeltaY());
    }

    /*
    //original
    //wenn zoomFactorProperty = 0.01 dann ist 0.01*sqrt(2)= 0.014 und nach round wieder =0.01
    //dann kann man nicht merh rein/rauszoomen
    public void zoom(double sign) {
        double zoomFactor = round(zoomFactorProperty.get() * Math.sqrt(2), 3);
        if (sign < 0) {
            zoomFactor = round(zoomFactorProperty.get() / Math.sqrt(2), 3);
        }
        zoomFactorProperty.set(in(0.01, round(zoomFactor, 2), 32));
    }
     */

    public void zoom(double sign) {
        if(zoomFactorProperty.get() == 0.01) {
            if (sign > 0) {
                zoomFactorProperty.set(0.02);
            }
        } else {
            double zoomFactor = round(zoomFactorProperty.get() * Math.sqrt(2), 3);
            if (sign < 0) {
                zoomFactor = round(zoomFactorProperty.get() / Math.sqrt(2), 3);
            }
            zoomFactorProperty.set(in(0.01, round(zoomFactor, 2), 32));
        }
        //System.out.println("zoomFactorProperty: " + zoomFactorProperty);
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public static double in(double min, double x, double max) {
        return java.lang.Math.max(min, java.lang.Math.min(x, max));
    }
}
