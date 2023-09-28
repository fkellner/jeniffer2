package de.unituebingen.dng.processor.util;

public enum AccelerationStrategy {
    NONE("None"),
    AUTO_SMALL("Optimal for small image"), // TODO: Unite
    AUTO_BIG("Optimal for big image"),     // TODO: Unite
    MULTITHREADING("Multithreading"),
    CPU_TILING("CPU Tiling"),
    CPU_TILING_MT("Thread-distributed CPU Tiling"),
    CPU_MT_TILING("CPU Tiling with MT"),
    CPU_MT_TILING_MT("Thread-distributed CPU Tiling with MT"),
    GPU_OPERATION_WISE("GPU (Operation Wise)"),
    GPU_TILE_WISE("GPU (Tile by Tile)"); //,
    // TORNADO("TornadoVM (OpenCL)");

    private String label;

    AccelerationStrategy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
