package de.unituebingen.dng.reader.compression;

import de.unituebingen.dng.reader.io.BitReader;

import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Eugen Ljavin
 *
 * A Huffman Decoder as specified in the JPEG specification. <br/>
 * Currently this class is only used for lossless JPEG decoding mode. It might be that the class has to be
 * extended/adapted if you want to use it for other JPEG decoding modes. <br/>
 * See JPEG specification (page 40ff, 50ff, 132ff) for more informations.
 */
public class JPEGHuffmanDecoder {

    private int[] huffSize;
    private int[] huffCodes;
    private byte[] huffValues;
    private byte[] bits;

    private Map<Integer, Map<Integer, Integer>> huffmanDecoder = new HashMap<Integer, Map<Integer, Integer>>();

    public JPEGHuffmanDecoder(byte[] bits, byte[] huffValues) {
        this.bits = Objects.requireNonNull(bits);
        this.huffValues = Objects.requireNonNull(huffValues);
        huffSize = generateHuffmanCodeSizes();
        huffCodes = generateHuffmanCodes();
    }

    /**
     * Decodes a given BitBuffer. <br/>
     * This method returns the huffman value for the first matching huffman code.
     *
     * @param bitReader The BitBuffer which should be decoded.
     * @return The huffman value for the first matching huffman code.
     */
    public int decode(BitReader bitReader) throws EOFException {
        int length = 1;

        int bit = bitReader.readBit();
        Integer huffValue = getHuffValue(bit, length);
        while (huffValue == null) {
            bit = (bit << 1) | bitReader.readBit();
            huffValue = getHuffValue(bit, ++length);
        }
        return huffValue;
    }

    private int[] generateHuffmanCodeSizes() {
        Objects.requireNonNull(bits);
        int[] huffsize = new int[huffValues.length + 1];

        int k = 0;
        int j = 1;
        for (int i = 0; i < bits.length; i++) {
            while (j <= (bits[i] & 0xff)) {
                huffsize[k] = i + 1;
                k++;
                j++;
            }
            j = 1;
        }
        huffsize[k] = 0;

        return huffsize;
    }

    private int[] generateHuffmanCodes() {
        Objects.requireNonNull(huffSize);
        int[] huffcode = new int[huffSize.length];

        int k = 0;
        int code = 0;
        int si = huffSize[0];

        while (true) {
            do {
                huffcode[k] = code;
                Map<Integer, Integer> codes = huffmanDecoder.get(huffSize[k]);
                if (codes == null) {
                    codes = new HashMap<Integer, Integer>();
                    huffmanDecoder.put(huffSize[k], codes);
                }
                codes.put(code++, (int) huffValues[k++]);
            } while (huffSize[k] == si);

            if (huffSize[k] == 0) {
                Map<Integer, Integer> codes = huffmanDecoder.get(huffSize[k - 1]);
                codes.put(code, 0);
                break;
            }

            do {
                code = code << 1;
                si++;
            } while (huffSize[k] != si);
        }

        return huffcode;
    }

    private Integer getHuffValue(int bit, int length) {
        return huffmanDecoder.get(length) == null ? null : huffmanDecoder.get(length).get(bit);
    }
}
