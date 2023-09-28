package de.unituebingen.imageprocessor;

import de.unituebingen.dng.reader.util.Math;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Arrays;
import java.util.Properties;

public class ImageUtils {

    public static BufferedImage create16BitBufferedImage(double processedImageData[], int width, int length) {
        int[] imageData = Arrays.stream(processedImageData).parallel().mapToInt(pixel -> (int) Math.in(0, pixel * 65535, 65535)).toArray();
        int[] bandOffsets = {0, 1, 2};
        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, width, length, 3 * width, 3, bandOffsets, null);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_USHORT);
        Properties properties = new Properties();
        BufferedImage outimage = new BufferedImage(colorModel, raster, false, properties);
        raster.setPixels(0, 0, width, length, imageData);

        return outimage;
    }

    public static BufferedImage create8BitBufferedImage(double processedImageData[], int width, int length) {
        int[] imageData = Arrays.stream(processedImageData).parallel().mapToInt(pixel -> (int) Math.in(0, pixel * 255, 255)).toArray();
        int[] bandOffsets = {0, 1, 2};
        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, length, 3 * width, 3, bandOffsets, null);
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
        Properties properties = new Properties();
        BufferedImage outimage = new BufferedImage(colorModel, raster, false, properties);
        raster.setPixels(0, 0, width, length, imageData);

        return outimage;
    }

    public static BufferedImage rotate(BufferedImage srcImage, double angle) {
        double sin = java.lang.Math.abs(java.lang.Math.sin(java.lang.Math.toRadians(angle)));
        double cos = java.lang.Math.abs(java.lang.Math.cos(java.lang.Math.toRadians(angle)));
        int srcWidth = srcImage.getWidth();
        int srcHeight = srcImage.getHeight();
        int destWidth = (int) java.lang.Math.floor(srcWidth * cos + srcHeight * sin);
        int destHeight = (int) java.lang.Math.floor(srcHeight * cos + srcWidth * sin);
        BufferedImage destImage = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = destImage.createGraphics();
        graphics.translate((destWidth - srcWidth) / 2, (destHeight - srcHeight) / 2);
        graphics.rotate(java.lang.Math.toRadians(angle), srcWidth / 2, srcHeight / 2);
        graphics.drawRenderedImage(srcImage, null);
        graphics.dispose();

        return destImage;
    }

    public static BufferedImage rotate(BufferedImage image, Orientation orientation) {
        if (orientation == Orientation.VETICAL_RIGHT) {
            return rotate(image, -90);
        } else if (orientation == Orientation.VETICAL_LEFT) {
            return rotate(image, 90);
        } else if (orientation == Orientation.HORIZONTAL_BOTTOM) {
            return rotate(image, 180);
        } else {
            return image;
        }
    }

    public static BufferedImage rotateClockwise90(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();

        BufferedImage dest = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics2D = dest.createGraphics();
        graphics2D.translate((height - width) / 2, (height - width) / 2);
        graphics2D.rotate(java.lang.Math.PI / 2, height / 2, width / 2);
        graphics2D.drawRenderedImage(src, null);

        return dest;
    }

    public enum Orientation {
        HORIZONTAL_TOP(1),
        HORIZONTAL_BOTTOM(3),
        VETICAL_LEFT(6),
        VETICAL_RIGHT(8);

        private int tiffOrientation;

        Orientation(int tiffOrientation) {
            this.tiffOrientation = tiffOrientation;
        }

        public static Orientation getByTiffOrientation(int tiffOrientation) {
            return Arrays.stream(Orientation.values())
                    .filter(orientation -> orientation.tiffOrientation == tiffOrientation)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown TIFF Orientation: " + tiffOrientation));
        }
    }
}
