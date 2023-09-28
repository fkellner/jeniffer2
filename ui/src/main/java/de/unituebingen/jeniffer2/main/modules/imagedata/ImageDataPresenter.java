package de.unituebingen.jeniffer2.main.modules.imagedata;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

import javax.inject.Inject;

import de.unituebingen.jeniffer2.ApplicationData;
import de.unituebingen.jeniffer2.main.util.informationdialog.InformationDialogHelper;
import de.unituebingen.jeniffer2.util.PipelineConfiguration;
import de.unituebingen.dng.processor.util.AccelerationStrategy;
import de.unituebingen.dng.processor.demosaicingprocessor.DemosaicingProcessor.InterpolationMethod;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

public class ImageDataPresenter implements Initializable {

    @FXML
    private Label xPos;

    @FXML
    private Label yPos;

    @FXML
    private Label rValue;

    @FXML
    private Label gValue;

    @FXML
    private Label bValue;

    @FXML
    private Canvas lensCanvas;

    @FXML
    private ComboBox<Integer> lensSizeChooser;
    
    @FXML
    private ComboBox<LensMode> lensModeChooser;

    @FXML
    private ComboBox<CompareMode> compareModeChooser;

    @FXML
    private HBox comparisonConfig;

    private ComboBox<PipelineConfiguration> compareSlot1 = new ComboBox<PipelineConfiguration>();
    private ComboBox<PipelineConfiguration> compareSlot2 = new ComboBox<PipelineConfiguration>();
    private ComboBox<PipelineConfiguration> compareSlot3 = new ComboBox<PipelineConfiguration>();

    @Inject
    private ImageDataModel dataModel;

    @Inject
    private ApplicationData applicationData;

    private int imgHeight;
    private int imgWidth;
    private PixelReader pixelReader;
    private PixelReader pixelReader1;
    private PixelReader pixelReader2;
    private PixelReader pixelReader3;
    private String config;
    private String config1 = "-- choose --";
    private String config2 = "-- choose --";
    private String config3 = "-- choose --";


    private enum LensMode {
        RGB, R, G, B, ALL
    };

    private enum CompareMode {
        CURRENT, COMPARE
    };

    private LensMode lensMode = LensMode.RGB;
    private CompareMode compareMode = CompareMode.CURRENT;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            dataModel.posProperty().addListener(posListener);
            dataModel.imageUpdated().addListener(newImgListener);
            lensSizeChooser.getItems().addAll(1, 5, 9, 15, 27, 81);
            // 81 is laggy on my old laptop, but may work elsewhere
            lensSizeChooser.setValue(27);
            lensSizeChooser.setConverter(new StringConverter<>() {

                @Override
                public String toString(Integer size) {
                    return size + "x" + size + " px";
                }

                @Override
                public Integer fromString(String s) {
                    return Integer.parseInt(s.split("x")[0]);
                }
            });
            dataModel.sideLengthPropery().bind(lensSizeChooser.valueProperty());
            lensModeChooser.setValue(LensMode.RGB);
            lensModeChooser.getItems().addAll(LensMode.values());
            lensModeChooser.valueProperty().addListener((obs, old, n) -> {
                lensMode = n;
                if(lensMode == LensMode.ALL) {
                    compareModeChooser.getItems().remove(CompareMode.COMPARE);
                } else if(!compareModeChooser.getItems().contains(CompareMode.COMPARE)) {
                    compareModeChooser.getItems().add(CompareMode.COMPARE);
                }
            });
            compareModeChooser.setValue(compareMode);
            compareModeChooser.getItems().addAll(CompareMode.values());
            compareModeChooser.valueProperty().addListener((obs, old, nu) -> {
                compareMode = nu;
                if (compareMode == CompareMode.COMPARE) {
                    comparisonConfig.setVisible(true);                    
                    comparisonConfig.getChildren().addAll(
                        compareSlot1, compareSlot2, compareSlot3
                    );
                    updateAvailableConfigs();
                    lensModeChooser.getItems().remove(LensMode.ALL);
                } else {
                    comparisonConfig.setVisible(false);
                    comparisonConfig.getChildren().clear();
                    compareSlot1.setValue(null);
                    compareSlot2.setValue(null);
                    compareSlot3.setValue(null);
                    if(!lensModeChooser.getItems().contains(LensMode.ALL)) {
                        lensModeChooser.getItems().add(LensMode.ALL);
                    }
                }
            });
            compareSlot1.setConverter(configConverter);
            compareSlot1.setValue(null);
            compareSlot1.valueProperty().addListener(slotUpdater(1));
            compareSlot2.setConverter(configConverter);
            compareSlot2.setValue(null);
            compareSlot2.valueProperty().addListener(slotUpdater(2));
            compareSlot3.setConverter(configConverter);
            compareSlot3.setValue(null);
            compareSlot3.valueProperty().addListener(slotUpdater(3));
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
    private ChangeListener<ObservableList<Integer>> posListener = (obs, old, nu) -> {
        Integer x = nu.get(0);
        Integer y = nu.get(1);
        xPos.textProperty().set(x == null ? "-" : x.toString());
        yPos.textProperty().set(y == null ? "-" : y.toString());
        redrawLens();
    };

    private ChangeListener<PipelineConfiguration> slotUpdater(int slot) {
        return (obs, old, nu) -> {
            if (old == nu) return;
            Image img = applicationData.getConfigurations().get(nu);
            PixelReader rdr = null;
            if(img != null) {
                rdr = img.getPixelReader();
            }
            switch(slot) {
                case 1:
                    pixelReader1 = rdr;
                    config1 = configConverter.toString(nu);
                    return;
                case 2:
                    pixelReader2 = rdr;
                    config2 = configConverter.toString(nu);
                    return;
                case 3:
                    pixelReader3 = rdr;
                    config3 = configConverter.toString(nu);
                    return;
                default:
                    return;
            }
        };
    }

    private StringConverter<PipelineConfiguration> configConverter = new StringConverter<>() {

        @Override
        public String toString(PipelineConfiguration config) {
            if (config == null)
                return "-- choose --";
            return 
                (config.subStep() != null && config.subStep() != "" ? config.subStep() + "," : "") +
                config.interpolationMethod().toString() + "," +
                config.accelerationStrategy().toString();
        }

        @Override
        public PipelineConfiguration fromString(String s) {
            if (s == "-- choose --")
                return null;
            String[] parts = s.split(",");
            int hasSubstep = parts.length == 3 ? 1 : 0;
            return new PipelineConfiguration(
                InterpolationMethod.valueOf(parts[hasSubstep]),
                AccelerationStrategy.valueOf(parts[1 + hasSubstep]), 
                hasSubstep > 0 ? parts[0] : "");
        }
    };

    private ChangeListener<Boolean> newImgListener = (obs, old, nu) -> {
        if (old == nu) return;
        Image img = applicationData.getImageCached();
        imgWidth = (int)img.getWidth();
        imgHeight = (int)img.getHeight();
        pixelReader = img.getPixelReader();
        config = configConverter.toString(applicationData.getCurrentConfig());
        updateAvailableConfigs();
    };

    private void updateAvailableConfigs() {
        Set<PipelineConfiguration> configs = applicationData.getConfigurations().keySet();

        PipelineConfiguration old1 = compareSlot1.getValue();
        compareSlot1.getItems().clear();
        compareSlot1.getItems().addAll(configs);
        compareSlot1.getItems().add(null);
        compareSlot1.setValue(configs.contains(old1) ? old1 : null);

        PipelineConfiguration old2 = compareSlot2.getValue();
        compareSlot2.getItems().clear();
        compareSlot2.getItems().addAll(configs);
        compareSlot2.getItems().add(null);
        compareSlot2.setValue(configs.contains(old2) ? old2 : null);

        PipelineConfiguration old3 = compareSlot3.getValue();
        compareSlot3.getItems().clear();
        compareSlot3.getItems().addAll(configs);
        compareSlot3.getItems().add(null);
        compareSlot3.setValue(configs.contains(old3) ? old3 : null);
    }

    public void redrawLens() {
        // prepare canvas
        double width = lensCanvas.getWidth();
        double height = lensCanvas.getHeight();
        GraphicsContext context = lensCanvas.getGraphicsContext2D();
        context.clearRect(0, 0, width, height);

        // check if we need to draw
        int sideLength = dataModel.sideLengthPropery().get();
        Integer mouseX = dataModel.posProperty().get(0);
        Integer mouseY = dataModel.posProperty().get(1);
        int offset = (sideLength - (sideLength % 2)) / 2;
        if(mouseX == null || mouseY == null || 
           mouseX - offset < 0 || mouseX + offset >= imgWidth || 
           mouseY - offset < 0 || mouseY + offset >= imgHeight) {
            // update text
            rValue.textProperty().set("-");
            gValue.textProperty().set("-");
            bValue.textProperty().set("-");
            return;
        }

        // get pixel color values
        Color[] colors = new Color[sideLength * sideLength];
        Color[] colors1 = new Color[0];
        Color[] colors2 = new Color[0];
        Color[] colors3 = new Color[0];
        if(compareMode == CompareMode.COMPARE) {
            if(pixelReader1 != null) colors1 = new Color[sideLength * sideLength];
            if(pixelReader2 != null) colors2 = new Color[sideLength * sideLength];
            if(pixelReader3 != null) colors3 = new Color[sideLength * sideLength];
        }
        double maxDiff = 0.f;
        for (int i = 0; i < sideLength; i++) {
            for (int j = 0; j < sideLength; j++) {
                int xPos = mouseX - offset + j;
                int yPos = mouseY - offset + i;
                int idx = i * sideLength + j;
                Color color = pixelReader.getColor(xPos, yPos);
                colors[idx] = color;
                if(colors1.length > 0) {
                    colors1[idx] = pixelReader1.getColor(xPos, yPos);
                    if(colors3.length == 0) {
                        if(Math.abs(colors1[idx].getRed() - Math.abs(colors[idx].getRed())) > maxDiff) maxDiff = Math.abs(colors1[idx].getRed() - Math.abs(colors[idx].getRed()));
                        if(Math.abs(colors1[idx].getGreen() - Math.abs(colors[idx].getGreen())) > maxDiff) maxDiff = Math.abs(colors1[idx].getGreen() - Math.abs(colors[idx].getGreen()));
                        if(Math.abs(colors1[idx].getBlue() - Math.abs(colors[idx].getBlue())) > maxDiff) maxDiff = Math.abs(colors1[idx].getBlue() - Math.abs(colors[idx].getBlue()));
                    }
                }
                if(colors2.length > 0) {
                    colors2[idx] = pixelReader2.getColor(xPos, yPos);
                }
                if(colors3.length > 0) {
                    colors3[idx] = pixelReader3.getColor(xPos, yPos);
                }
            }
        }

        // update text
        int numPixels = colors.length;
        int center = (numPixels - (numPixels % 2)) / 2;
        Color firstColor = colors[center];
        rValue.textProperty().set(firstColor.getRed() + "");
        gValue.textProperty().set(firstColor.getGreen() + "");
        bValue.textProperty().set(firstColor.getBlue() + "");
        
        // draw magnified pixels
        if (lensMode == LensMode.ALL) {
            double step = width / (sideLength * 2.0f);
            double half = width / 2.0f;
            for (int x = 0; x < sideLength; x++) {
                for (int y = 0; y < sideLength; y++) {
                    double originX = x * step;
                    double originY = y * step;
                    int color = y * sideLength + x;
                    Color rgb = colors[color];
                    // paint rgb part
                    context.setFill(rgb);
                    context.fillRect(
                            originX,
                            originY,
                            step,
                            step);
                    // paint red part
                    double red = rgb.getRed();
                    Color redColor = Color.color(red, red / 1.1f, red / 1.1f);
                    context.setFill(redColor);
                    context.fillRect(
                            originX + half,
                            originY,
                            step,
                            step);
                    // paint green part
                    double green = rgb.getGreen();
                    Color greenColor = Color.color(green / 1.1f, green, green / 1.1f);
                    context.setFill(greenColor);
                    context.fillRect(
                            originX,
                            originY + half,
                            step,
                            step);
                    // paint blue part
                    double blue = rgb.getBlue();
                    Color blueColor = Color.color(blue / 1.1f, blue / 1.1f, blue);
                    context.setFill(blueColor);
                    context.fillRect(
                            originX + half,
                            originY + half,
                            step,
                            step);
                }
            }
        } else {
            // assume width == height
            double step = width / sideLength;
            if(compareMode == CompareMode.COMPARE) step = step/2.0f;
            double half = width / 2.0f;
            for (int x = 0; x < sideLength; x++) {
                for (int y = 0; y < sideLength; y++) {
                    int color = y * sideLength + x;
                    Color rgb = colors[color];
                    switch (lensMode) {
                        case R:
                            double red = rgb.getRed();
                            rgb = Color.color(red, red / 1.1f, red / 1.1f);
                            break;
                        case G:
                            double green = rgb.getGreen();
                            rgb = Color.color(green / 1.1f, green, green / 1.1f);
                            break;
                        case B:
                            double blue = rgb.getBlue();
                            rgb = Color.color(blue / 1.1f, blue / 1.1f, blue);
                            break;
                        default:
                    }
                    context.setFill(rgb);
                    double originX = x * step;
                    double originY = y * step;
                    context.fillRect(
                            originX,
                            originY,
                            step,
                            step);
                    if(compareMode == CompareMode.CURRENT) continue;
                    Color rgb1 = colors1.length > 0 ? colors1[color] : Color.WHITE;
                    Color rgb2 = colors2.length > 0 ? colors2[color] : Color.color(Math.abs(rgb.getRed() - rgb1.getRed()), Math.abs(rgb.getGreen() - rgb1.getGreen()), Math.abs(rgb.getBlue() - rgb1.getBlue())); // otherwise compare!
                    Color rgb3 = colors3.length > 0 ? colors3[color] : Color.color(Math.min(1.0f, rgb2.getRed() / maxDiff), Math.min(1.0f, rgb2.getGreen() / maxDiff), Math.min(1.0f, rgb2.getBlue() / maxDiff)); // otherwise show scaled version
                    switch (lensMode) {
                        case R:
                            double red1 = rgb1.getRed();
                            rgb1 = Color.color(red1, red1 / 1.1f, red1 / 1.1f);
                            double red2 = rgb2.getRed();
                            rgb2 = Color.color(red2, red2 / 1.1f, red2 / 1.1f);
                            double red3 = rgb3.getRed();
                            rgb3 = Color.color(red3, red3 / 1.1f, red3 / 1.1f);
                            break;
                        case G:
                            double green1 = rgb1.getGreen();
                            rgb1 = Color.color(green1 / 1.1f, green1, green1 / 1.1f);
                            double green2 = rgb2.getGreen();
                            rgb2 = Color.color(green2 / 1.1f, green2, green2 / 1.1f);
                            double green3 = rgb3.getGreen();
                            rgb3 = Color.color(green3 / 1.1f, green3, green3 / 1.1f);
                            break;
                        case B:
                            double blue1 = rgb1.getBlue();
                            rgb1 = Color.color(blue1 / 1.1f, blue1 / 1.1f, blue1);
                            double blue2 = rgb2.getBlue();
                            rgb2 = Color.color(blue2 / 1.1f, blue2 / 1.1f, blue2);
                            double blue3 = rgb3.getBlue();
                            rgb3 = Color.color(blue3 / 1.1f, blue3 / 1.1f, blue3);
                            break;
                        default:
                    }
                    context.setFill(rgb1);
                    double originX1 = x * step + half;
                    double originY1 = y * step;
                    context.fillRect(
                            originX1,
                            originY1,
                            step,
                            step);
                    context.setFill(rgb2);
                    double originX2 = x * step;
                    double originY2 = y * step + half;
                    context.fillRect(
                            originX2,
                            originY2,
                            step,
                            step);
                    context.setFill(rgb3);
                    double originX3 = x * step + half;
                    double originY3 = y * step + half;
                    context.fillRect(
                            originX3,
                            originY3,
                            step,
                            step);
                }
            }
            if(compareMode == CompareMode.COMPARE) {
                context.setFill(Color.BLACK);
                Font theFont = Font.font("sans-serif", FontWeight.NORMAL, half / 15.0f);
                context.setFont(theFont);
                context.setTextAlign(TextAlignment.LEFT);
                context.setTextBaseline(VPos.TOP);
                context.fillText(config, 0, 0, half);
                context.fillText(config1, half, 0, half);
                if(colors2.length > 0) {
                    context.fillText(config2, 0, half, half);
                } else {
                    context.setFill(Color.WHITE);
                    context.fillText("Difference", 0, half, half);
                };
                if(colors3.length > 0) {
                    context.fillText(config3, half, half, half);
                } else {
                    context.fillText("normalized", half, half, half);
                };
            }
        }
    }
}
