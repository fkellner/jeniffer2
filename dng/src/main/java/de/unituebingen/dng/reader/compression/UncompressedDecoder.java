package de.unituebingen.dng.reader.compression;

import de.unituebingen.dng.reader.io.BitReader;
import de.unituebingen.dng.reader.util.ByteUtil;

import java.io.EOFException;
import java.nio.ByteOrder;

/**
 * @author Eugen Ljavin
 * <p>
 * A decoder which reads uncompressed raw image data.
 */
public class UncompressedDecoder implements CompressionDecoder {

    private int bitsPerSample;
    private ByteOrder byteOrder;

    public UncompressedDecoder(int bitsPerSample, ByteOrder byteOrder) {
        this.bitsPerSample = bitsPerSample;
        this.byteOrder = byteOrder;
    }

    @Override
    public int[] decode(byte[] data) {
        return readStrip(data, bitsPerSample);
    }

    private int[] readStrip(byte[] strip, int bitsPerSample) {
        int[] image;
        if (bitsPerSample == 8) {
            image = read8BitStrip(strip);
        } else if (bitsPerSample == 16) {
            image = read16BitStrip(strip);
        } else {
            image = readOtherBitStrip(strip, bitsPerSample);
        }
        return image;
    }

    private int[] read16BitStrip(byte[] strip) {
        int[] image = new int[(strip.length / 2)];
        for (int i = 0; i < strip.length; i = i + 2) {
            image[i >> 1] = ByteUtil.toUnsignedShort(byteOrder, strip[i], strip[i + 1]);
        }
        return image;
    }

    private int[] read8BitStrip(byte[] strip) {
        int[] decodedStrip = new int[strip.length];
        for (int i = 0; i < strip.length; i++) {
            decodedStrip[i] = strip[i] & 0xff;
        }
        return decodedStrip;
    }

    private int[] readOtherBitStrip(byte[] strip, int bitsPerSample) {
        int[] image = new int[(strip.length / bitsPerSample) * 8];
        BitReader bitReader = new BitReader(strip);
        for (int i = 0; i < image.length; i++) {
            try {
                image[i] = bitReader.readUnsignedBits(bitsPerSample);
            } catch (EOFException e) {
                e.printStackTrace();
            }
        }
        return image;
    }
}
