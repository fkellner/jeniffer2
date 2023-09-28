package de.unituebingen.dng.reader.dng.util;

import java.util.Arrays;

public enum CFAPattern {

    GRBG(new short[]{1, 0, 2, 1}),
    GBRG(new short[]{1, 2, 0, 1}),
    RGGB(new short[]{0, 1, 1, 2}),
    BGGR(new short[]{2, 1, 1, 0});

    private final short[] cfaPattern;

    CFAPattern(short[] cfaPattern) {
        this.cfaPattern = cfaPattern;
    }

    public static CFAPattern getByCFAPattern(short[] cfaPattern) {
        //changed for PNG test
        //return RGGB;
        return Arrays.stream(CFAPattern.values())
                .filter(pattern -> Arrays.equals(pattern.cfaPattern, cfaPattern))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown CFA Pattern: " + cfaPattern));
    }

    public short[] getCfaPattern() {
        return cfaPattern;
    }
}
