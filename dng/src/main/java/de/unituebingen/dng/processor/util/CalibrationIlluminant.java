package de.unituebingen.dng.processor.util;

import java.util.Arrays;

/**
 * @author Eugen Ljavin
 * <p>
 * See DNG Specification 1.5.0.0, page 30 and 31.
 */
public enum CalibrationIlluminant {

    UNKNOWN("Unknown", 0, 0),
    DAYLIGHT("Daylight", 1, 5503),
    FLUORESCENT("Fluorescent", 2, 4230),
    TUNGSTEN("Tungsten (incandescent light)", 3, 2856),
    FLASH("Flash", 4, 5500),
    FINE_WEATHER("Fine weather", 9, 5503),
    CLOUDY_WEATHER("Cloudy weather", 10, 6504),
    SHADE("Shade", 11, 7504),
    DAYLIGHT_FLUORESCENT("Daylight fluorescent", 12, 6430),
    DAY_WHITE_FLUORESCENT("Day white fluorescent", 13, 4940),
    COOL_WHITE_FLUORESCENT("Cool white fluorescent", 14, 4230),
    WHITE_FLUORESCENT("White fluorescent", 15, 3450),
    STANDARD_LIGHT_A("Standard light A", 17, 2856),
    STANDARD_LIGHT_B("Standard light B", 18, 4874),
    STANDARD_LIGHT_C("Standard light C", 19, 6774),
    D55("D55", 20, 5503),
    D65("D65", 21, 6504),
    D75("D75", 22, 7504),
    D50("D50", 23, 5003),
    ISO_STUDIO_TUNGSTEN("ISO studio tungsten", 24, 3200),
    OTHER_LIGHT_SOURCE("Other light source", 255, 0);

    private String label;
    private int id;
    private int cct;

    CalibrationIlluminant(String label, int id, int cct) {
        this.label = label;
        this.id = id;
        this.cct = cct;
    }

    public int getCCT() {
        return cct;
    }

    /**
     * Returns the CalibrationIlluminant by given ID.
     *
     * @param id The ID of the CalibrationIlluminant
     * @return The CalibrationIlluminant with the given ID
     */
    public static CalibrationIlluminant getByID(int id) {
        return Arrays.stream(CalibrationIlluminant.values())
                .filter(source -> source.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("CalibrationIlluminant with ID %s does not exist.", id)));
    }

    /**
     * Creates a default CalibrationIlluminant object.
     *
     * @return The default CalibrationIlluminant object
     */
    public static CalibrationIlluminant createDefault() {
        return CalibrationIlluminant.UNKNOWN;
    }
}
