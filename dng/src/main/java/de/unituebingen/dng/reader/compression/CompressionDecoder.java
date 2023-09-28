package de.unituebingen.dng.reader.compression;

/**
 * @author Eugen Ljavin
 *
 * Implement this interface if you want to create a particular compression decoder.
 */
public interface CompressionDecoder {

    /**
     * Decodes the given data.
     *
     * @param data The data which should be decoded
     * @return The decoded data
     * @throws CompressionDecoderException If something goes wrong during decoding
     */
    int[] decode(byte[] data) throws CompressionDecoderException;
}
