package de.unituebingen.dng.processor;

import de.unituebingen.dng.reader.util.Math;

public abstract class Operation {

    protected int width;
    protected int length;

    public Operation(int imageWidth, int imageHeight) {
        this.width = imageWidth; 
        this.length = imageHeight; 

    }

    public int translateX(int x) {
        return Math.in(0, x, width - 1);
    }
    public int translateY(int y) {
        return Math.in(0, y, length - 1);
    }

    public int getXByIndex(int index) {
        return index % width;
    }

    public int getYByIndex(int index) {
        return index / width;
    }

    public int getIndexByXY(int x, int y) {
        return y * width + x;
    }

}

