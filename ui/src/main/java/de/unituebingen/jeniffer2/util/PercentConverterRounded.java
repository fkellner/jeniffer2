package de.unituebingen.jeniffer2.util;

import javafx.util.StringConverter;

public class PercentConverterRounded extends StringConverter<Number> {

    @Override
    public String toString(Number number) {
        return String.valueOf(Math.round(number.doubleValue() * 100));
    }

    @Override
    public Number fromString(String s) {
        if (s != null && !s.trim().isEmpty()) {
            return Double.parseDouble(s) / 100;
        }
        return 1;
    }
}

