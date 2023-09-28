package de.unituebingen.dng.reader.compression;

import de.unituebingen.dng.reader.io.BitReader;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * @author Eugen Ljavin
 * <p>
 * A JPEG decoder in losselss mode as specified in JPEG specification. <br/>
 * It decodes the whole compressed image data. Currently only prediction value 1 (Px = Ra, see JPEG specification
 * page 133) is supported since it is the most common prediction which is used in JPEG compressed DNG files. <br/>
 * Hierarchical mode of operation is not supported since TIFF doesnt allow that mode of operation. <br/>
 * Note that some header fields are skipped since we dont need all of the header fields.
 */
public class LosslessJPEGDecoder implements CompressionDecoder {

    private static final int SOI_MARKER = 0xFFD8;
    private static final int SOF3_MARKER = 0xFFC3;
    private static final int SOS_MARKER = 0xFFDA;
    private static final int EOI_MARKER = 0xFFD9;
    private static final int DHT_MARKER = 0xFFC4;

    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN; //Default JPEG Byte order

    private int precisionP;
    private int numOfLinesY;
    private int numOfSamplesPerLineX;
    private int numOfImageCompInFrameFn;

    private JPEGHuffmanDecoder[] JPEGHuffmanDecoders;
    private int[] horizontalPredictor; //predictor of type 1, see JPEG specification, page 133
    private int[] decodedImg;

    @Override
    public int[] decode(byte[] data) throws CompressionDecoderException {
        Objects.requireNonNull(data);
        ByteBuffer buffer = ByteBuffer.wrap(data).order(byteOrder);

        int startOfImage = buffer.getShort() & 0xFFFF;
        if (startOfImage != SOI_MARKER) {
            throw new CompressionDecoderException("An error occurred during lossless JPEG decoding. The SOI marker is not set. " +
                    "The data is probably not JPEG encoded");
        }

        JPEGHuffmanDecoder[] dcTables = new JPEGHuffmanDecoder[4];

        boolean loop = true;
        while (loop) {
            int tag = buffer.getShort() & 0xffff;
            int tagLength = buffer.getShort() & 0xffff;

            switch (tag) {
                case SOF3_MARKER: //JPEG Specification, page
                    precisionP = buffer.get() & 0xff;
                    numOfLinesY = buffer.getShort() & 0xffff;
                    numOfSamplesPerLineX = buffer.getShort() & 0xffff;
                    numOfImageCompInFrameFn = buffer.get() & 0xff;

                    decodedImg = new int[numOfLinesY * numOfSamplesPerLineX * numOfImageCompInFrameFn];
                    horizontalPredictor = new int[numOfImageCompInFrameFn];
                    for (int i = 0; i < horizontalPredictor.length; i++) {
                        horizontalPredictor[i] = 1 << (precisionP - 1); //See JPEG Specification, page 133
                    }

                    //we dont need the other fields, so skip them
                    buffer.position(buffer.position() + (numOfImageCompInFrameFn * 3));
                    break;
                case DHT_MARKER: //JPEG Specification, page 40
                    int remainingBytes = tagLength - 2;
                    while (remainingBytes > 0) {
                        int destinationIdentifier = buffer.get() & 0xff;
                        remainingBytes--;
                        byte[] bits = new byte[16];

                        int amountHuffValues = 0;
                        for (int i = 0; i < bits.length; i++) {
                            remainingBytes--;
                            bits[i] = buffer.get();
                            amountHuffValues += bits[i];
                        }
                        byte[] huffValues = new byte[amountHuffValues];
                        for (int i = 0; i < huffValues.length; i++) {
                            huffValues[i] = buffer.get();
                            remainingBytes--;
                        }

                        dcTables[destinationIdentifier] = new JPEGHuffmanDecoder(bits, huffValues);
                    }
                    break;
                case SOS_MARKER: //JPEG Specification, page
                    int numOfImageCompInScanFn = buffer.get() & 0xff;
                    JPEGHuffmanDecoders = new JPEGHuffmanDecoder[numOfImageCompInScanFn];

                    for (int i = 0; i < numOfImageCompInScanFn; i++) {
                        final int selectedScanComp = buffer.get() & 0xff;
                        final int dcAcSelector = buffer.get() & 0xff;
                        final int dcSelector = dcAcSelector >> 4;
                        final int acSelector = dcAcSelector & 0xf;
                        JPEGHuffmanDecoders[selectedScanComp] = dcTables[dcSelector];
                    }
                    final int predictorSelection = buffer.get() & 0xff;
                    buffer.position(buffer.position() + 2);
                    loop = false;
                    break;
                default:
                    break;
            }
        }

        BitReader bitReader = new BitReader(buffer, true);
        int scan = 0;
        for (int line = 0; line < numOfLinesY; line++) {
            for (int sample = 0; sample < numOfSamplesPerLineX; sample++) {
                for (int comp = 0; comp < numOfImageCompInFrameFn; comp++) {
                    final JPEGHuffmanDecoder decoder = JPEGHuffmanDecoders[comp];
                    int huffValue;
                    int diff;
                    try {
                        huffValue = decoder.decode(bitReader);
                        diff = bitReader.readSignedBits(huffValue);
                    } catch (EOFException e) {
                        throw new CompressionDecoderException("The end of file has been reached unexpectedly.");
                    }
                    decodedImg[scan] = ((sample == 0) ? (horizontalPredictor[comp] += diff) : (decodedImg[scan - numOfImageCompInFrameFn] + diff));
                    scan++;
                }
            }
        }
        return decodedImg;
    }
}
