package de.unituebingen.jeniffer2.main.modules.histogram;

// import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.WorkflowManager;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
// import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
// import javafx.scene.chart.XYChart;

import javax.inject.Inject;
// import java.awt.image.BufferedImage;
// import java.awt.image.Raster;
import java.net.URL;
import java.util.ResourceBundle;

public class HistogramPresenter implements Initializable {

    @FXML
    private AreaChart<Integer, Integer> histogram;

    // @Inject
    // private ApplicationData applicationData;

    @Inject
    private WorkflowManager workflowManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            histogram.setCreateSymbols(false);
            workflowManager.canvasLoadedProperty().addListener((observableValue, aBoolean, t1) -> {
                if (t1) {
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
        histogram.getData().clear();
/*
        BufferedImage image = applicationData.getImage();
        XYChart.Series rSeries = new XYChart.Series();
        XYChart.Series gSeries = new XYChart.Series();
        XYChart.Series bSeries = new XYChart.Series();

        ObservableList<XYChart.Data> rData = rSeries.getData();
        ObservableList<XYChart.Data> gData = gSeries.getData();
        ObservableList<XYChart.Data> bData = bSeries.getData();

        int[] r = new int[256];
        int[] g = new int[256];
        int[] b = new int[256];

        Raster raster = image.getData();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rSample = (int) ((raster.getSample(x, y, 0) / 65535.0) * 255);
                int gSample = (int) ((raster.getSample(x, y, 1) / 65535.0) * 255);
                int bSample = (int) ((raster.getSample(x, y, 2) / 65535.0) * 255);
                r[rSample] = ++r[rSample];
                g[gSample] = ++g[gSample];
                b[bSample] = ++b[bSample];
            }
        }
h
        int factor = 1;
        for (int i = 0; i < r.length / factor; i++) {
            rData.add(new XYChart.Data(i * factor, r[i * factor]));
            gData.add(new XYChart.Data(i * factor, g[i * factor]));
            bData.add(new XYChart.Data(i * factor, b[i * factor]));
        }
        histogram.getData().addAll(rSeries, gSeries, bSeries);
        raster = null;
        image = null;
        System.gc();

 */
    }
}
