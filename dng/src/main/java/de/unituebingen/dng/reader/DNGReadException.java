package de.unituebingen.dng.reader;

/**
 * @author Eugen Ljavin
 *
 * This Exception should be thrown if any error occurs while reading the TIFF file.
 */
public class DNGReadException extends Exception {


    public DNGReadException(String message) {
        super(message);
    }
}
