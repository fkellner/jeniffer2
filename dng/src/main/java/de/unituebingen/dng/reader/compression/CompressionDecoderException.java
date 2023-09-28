package de.unituebingen.dng.reader.compression;

/**
 * @author Eugen Ljavin
 *
 * Throw this exception when something goes wrong during decoding.
 */
public class CompressionDecoderException extends Exception {

    public CompressionDecoderException(String message) {
        super(message);
    }
}
