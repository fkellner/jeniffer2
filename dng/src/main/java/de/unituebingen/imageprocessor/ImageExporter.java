package de.unituebingen.imageprocessor;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ImageExporter {

    private static final String PNG_EXTENSION = "png";
    private static final String TIFF_EXTENSION = "tiff";
    private static final String JPG_EXTENSION = "jpg";

    public static void saveAsTIFF(BufferedImage image, File file) throws IOException {
        final ImageWriter writer = ImageIO.getImageWritersByFormatName(TIFF_EXTENSION).next();
        writer.setOutput(new FileImageOutputStream(file));
        writer.write(null, new IIOImage(image, null, null), null);
    }

    public static void saveAsPNG(BufferedImage image, File file) throws IOException {
        final ImageWriter writer = ImageIO.getImageWritersByFormatName(PNG_EXTENSION).next();
        writer.setOutput(new FileImageOutputStream(file));
        writer.write(null, new IIOImage(image, null, null), null);
    }

    public static void saveAsJPEG(BufferedImage image, File file, float compression) throws IOException {
        JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
        jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpegParams.setCompressionQuality(compression);

        final ImageWriter writer = ImageIO.getImageWritersByFormatName(JPG_EXTENSION).next();
        writer.setOutput(new FileImageOutputStream(file));
        writer.write(null, new IIOImage(image, null, null), jpegParams);
    }

    /**
     * Create an 8 Bit BufferedImage from an existing image
     * @param image A BufferedImage based on an unsigned short buffer (DataBufferUShort)
     * @return A BufferedImage based on a byte buffer (DataBufferByte)
     */
    public static BufferedImage create8BitBufferedImage(BufferedImage image) {
        WritableRaster origRaster = image.getRaster();
        if(origRaster.getTransferType() != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException("conversion only implemented from Images based on DataBufferUShort/TYPE_USHORT");
        }
        short[] samples = ((DataBufferUShort) origRaster.getDataBuffer()).getData();

        int translateX = image.getData().getSampleModelTranslateX();
        int translateY = image.getData().getSampleModelTranslateY();

        int width = origRaster.getParent().getWidth();
        int height = origRaster.getParent().getHeight();
        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 3 * width, 3, new int[]{0, 1, 2}, new Point(0, 0));

        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(colorSpace, false, false, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
        BufferedImage bit8Image = new BufferedImage(colorModel, raster, false, new Properties());
        byte[] bit8Samples = ((DataBufferByte) bit8Image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < samples.length; i++) {
            bit8Samples[i] = (byte) (((samples[i] & 0xFFFF) / 65535.0) * 255);
        }

        return bit8Image.getSubimage(Math.abs(translateX), Math.abs(translateY), image.getWidth(), image.getHeight());
    }
}
