package de.unituebingen.dng.processor;

public class GlobalValues {

    private GlobalValues() {
        //empty on purpose
    }

    private static double cct;

    public static void setCCT(double cct) {
        GlobalValues.cct = cct;
    }

    public static double getCCT() {
        return GlobalValues.cct;
    }
}
