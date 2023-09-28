package de.unituebingen.dng.reader;

import de.unituebingen.dng.reader.compression.CompressionDecoder;
import de.unituebingen.dng.reader.compression.CompressionDecoderException;
import de.unituebingen.dng.reader.compression.LosslessJPEGDecoder;
import de.unituebingen.dng.reader.compression.UncompressedDecoder;
import de.unituebingen.dng.reader.io.DNGByteReader;
import de.unituebingen.dng.reader.util.Rational;
import de.unituebingen.dng.reader.util.SignedRational;
import de.unituebingen.imageprocessor.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;

/**
 * @author Eugen Ljavin
 * <p>
 * A class which allows to read and parse tiff files. <br/>
 * <p>
 * Currently following compression methods are supported:
 * <ul>
 *     <li>Uncompressed</li>
 *     <li>Lossless JPEG (Huffman) compression</li>
 * </ul>
 * 
 * LEGAL NOTICE
 * 
 * This product includes DNG technology under license by Adobe.
 * Dieses Produkt enthaelt die bei Adobe lizenzierte DNG-Technologie.
 */
public class DNGFile {

    private DNGByteReader reader;
    private ImageFileHeader imageFileHeader;
    private CompressionDecoder decoder;
    private File file;
    private List<ImageFileDirectory> imageFileDirectories;

    public DNGFile(File file) throws IOException, DNGReadException {
        this.file = Objects.requireNonNull(file);
        reader = new DNGByteReader(file);
        imageFileHeader = readImageFileHeader();
        reader.setByteOrder(imageFileHeader.getByteOrder());
    }

    public DNGFile(String path) throws IOException, DNGReadException {
        this(new File(path));
    }

    /**
     * Reads the baseline image file directories as well as some private image file directories:
     * <ul>
     *     <li>EXIF image file directories</li>
     *     <li>Sub image file directories</li>
     * </ul>
     * <p>
     * Use the {@link DNGFile#readImageFileDirectory(long)} method if you want to read an other e.g. private image
     * file  directory by providing the offset.
     *
     * @return All known image file directories
     * @throws DNGReadException If something goes wrong during tiff parsing
     * @throws EOFException     If the end of file has been reached
     */
    public List<ImageFileDirectory> getImageFileDirectories() throws EOFException, DNGReadException {
        if (imageFileDirectories == null) {
            List<ImageFileDirectory> imageFileDirectories = new ArrayList<ImageFileDirectory>();
            List<ImageFileDirectory> privateImageFileDirectories = new ArrayList<ImageFileDirectory>();
            //baseline
            for (long offsetIFD = imageFileHeader.getFirstIFDOffset(); offsetIFD != 0; offsetIFD = (int) reader.readUnsignedLong()) {
                ImageFileDirectory ifd = readImageFileDirectory(offsetIFD);
                ifd.setType(ImageFileDirectoryType.BASELINE);
                imageFileDirectories.add(ifd);
            }

            for (ImageFileDirectory ifd : imageFileDirectories) {
                //exif
                if (ifd.hasEntry(DNGTag.EXIF_IFD)) {
                    ImageFileDirectory exifIFD = readImageFileDirectory((long) ifd.getIFDEntry(DNGTag.EXIF_IFD).getValues());
                    exifIFD.setType(ImageFileDirectoryType.EXIF);
                    privateImageFileDirectories.add(exifIFD);
                }

                //subifd
                if (ifd.hasEntry(DNGTag.SUB_IFDS)) {
                    ImageFileDirectoryEntry subIfds = ifd.getIFDEntry(DNGTag.SUB_IFDS);
                    if (subIfds.hasMultipleValues()) {
                        long[] subIFDPointers = (long[]) subIfds.getValues();
                        for (Long subIFDPointer : subIFDPointers) {
                            ImageFileDirectory subIFD = readImageFileDirectory(subIFDPointer);
                            subIFD.setType(ImageFileDirectoryType.SUB);
                            privateImageFileDirectories.add(subIFD);
                        }
                    } else {
                        ImageFileDirectory subIFD = readImageFileDirectory((long) subIfds.getValues());
                        subIFD.setType(ImageFileDirectoryType.SUB);
                        privateImageFileDirectories.add(subIFD);
                    }
                }
            }

            imageFileDirectories.addAll(privateImageFileDirectories);
            this.imageFileDirectories = imageFileDirectories;
        }

        return imageFileDirectories;
    }

    /**
     * Finds the first image file directory of multiple given image file directories for a given SubfileType.
     * The available SubfileTypes are defined in the TIFF 6.0 specification.
     *
     * @param ifds           The image file directories to be searched
     * @param newSubfileType The SubfileType to search for
     * @return The image file directory for the given subfile type
     */
    public ImageFileDirectory getRasterIFD(List<ImageFileDirectory> ifds, long newSubfileType) {
        return ifds.stream()
                .filter(ifd -> ifd.hasEntry(DNGTag.NEW_SUBFILE_TYPE) && (long) ifd.getIFDEntry(DNGTag.NEW_SUBFILE_TYPE).getValues() == newSubfileType)
                .findFirst()
                .orElse(null);
    }

    public ImageFileDirectory getImageFileDirectoryByType(List<ImageFileDirectory> ifds, ImageFileDirectoryType type) {
        return ifds.stream()
                .filter(ifd -> ifd.getType() == type)
                .findFirst()
                .orElse(null);
    }

    public ImageFileDirectory getExifImageFileDirectory() throws EOFException, DNGReadException {
        List<ImageFileDirectory> imageFileDirectories = getImageFileDirectories();
        return getImageFileDirectoryByType(imageFileDirectories, ImageFileDirectoryType.EXIF);
    }

    public ImageFileDirectory get0thImageFileDirectory() throws EOFException, DNGReadException {
        List<ImageFileDirectory> imageFileDirectories = getImageFileDirectories();
        return getImageFileDirectoryByType(imageFileDirectories, ImageFileDirectoryType.BASELINE);
    }

    public ImageFileDirectory getRAWImageFileDirectory() throws EOFException, DNGReadException {
        List<ImageFileDirectory> imageFileDirectories = getImageFileDirectories();
        return getRasterIFD(imageFileDirectories, DNGTagConstants.NEW_SUBFILE_TYPE__HIGH_RESOLUTION);
    }

    /**
     * Reads an image raster for a given image file directory. <br/>
     * Striped and tiled rasters can be read if they are uncompressed or lossy JPEG compressed. Currently only
     * the PlanarConfiguration type 'chunky' is supported. <br/>
     * The only supported Photometric Interpretation types are 'Color Filter Array', 'RGB' and 'Linear RAW'.
     *
     * @param imageFileDirectory The image file directory from which the image raster should be read
     * @return The image raster of the given image file directory
     * @throws CompressionDecoderException If something goes wrong during image decoding
     * @throws DNGReadException            If something goes wrong during tiff parsing
     * @throws EOFException                If the end of file has been reached
     */
    public int[] parseRasterOfImageFileDirectory(ImageFileDirectory imageFileDirectory) throws
            DNGReadException, CompressionDecoderException, EOFException {
        Objects.requireNonNull(imageFileDirectory);
        if (!isPlanarConfigurationValid()) {
            throw new DNGReadException("The PlanarConfiguration is currently not supported");
        }

        if (!isPhotometricInterpretationValid()) {
            throw new DNGReadException("The PhotometricInterpretation is currently not supported");
        }

        int compression = imageFileDirectory.getCompression();
        int imageWidth = (int) imageFileDirectory.getImageWidth();
        int imageLength = (int) imageFileDirectory.getImageLength();
        int samplesPerPixel = imageFileDirectory.getSamplesPerPixel();
        int bitsPerSample = imageFileDirectory.getBitsPerSample()[0];

        assignCompressionDecoder(compression, bitsPerSample);

        int[] image = new int[imageWidth * imageLength * samplesPerPixel];
        if (imageFileDirectory.hasEntry(DNGTag.STRIP_OFFSETS)) {
            image = parseStrippedImage(imageFileDirectory, image, imageLength);
        } else {
            image = parseTiledImage(imageFileDirectory, image, imageWidth, imageLength);
        }

        return image;
    }

    public boolean isPhotometricInterpretationValid() throws EOFException, DNGReadException {
        ImageFileDirectory imageFileDirectory = getRAWImageFileDirectory();
        int photometricInterpretation = imageFileDirectory.getPhotometricInterpretation();
        if (photometricInterpretation != DNGTagConstants.PHOTOMETRIC_INTERPRETATION__CFA
                && photometricInterpretation != DNGTagConstants.PHOTOMETRIC_INTERPRETATION__RGB) {
            return false;
        }
        return true;
    }

    public boolean isPlanarConfigurationValid() throws EOFException, DNGReadException {
        ImageFileDirectory imageFileDirectory = getRAWImageFileDirectory();
        int planarConfiguration = imageFileDirectory.getPlanarConfiguration();
        if (planarConfiguration != DNGTagConstants.PLANAR_CONFIGURATION__CHUNKY) {
            return false;
        }
        return true;
    }

    /**
     * This is only a workaround which allows to read thumbnail images for DNG files which are usually located in the
     * first image file directory. The problem is, that thumbnails sometimes lossy JPEG encoded. Thats a compression
     * method which we are currently not supporting. <br/>
     * Since the Java TIFF readers is able to read the image of the first image file directory (which is a low resolution
     * preview image for DNG files) we use the Java TIFF reader to read this image.
     * <p>
     * At some later point (when all respectively lossy JPEG decoding is implemented) the {@link DNGFile#parseRasterOfImageFileDirectory(ImageFileDirectory)}
     * method should be able to read the thumbnail raster as well making this method obsolete and  deprecated.
     *
     * @return The thumbnail raster as {@link BufferedImage}
     * @throws DNGReadException If something goes wrong during tiff parsing
     * @throws IOException      If the end of file has been reached
     */
    public BufferedImage getThumbnailImage() throws IOException, DNGReadException {
        BufferedImage img = ImageIO.read(file);

        int orientation = get0thImageFileDirectory().getOrientation();
        return ImageUtils.rotate(img, ImageUtils.Orientation.getByTiffOrientation(orientation));
    }

    public int[] getRAWImage() throws EOFException, DNGReadException, CompressionDecoderException {
        return parseRasterOfImageFileDirectory(getRAWImageFileDirectory());
    }

    /**
     * Reads an image file directory starting from the given offset. <br/>
     * Use this method if you want to read an private image file directory. <br/>
     * <p>
     * Note that it is very likely that a TiffReadException or EOFException exception will be thrown for an invalid
     * offset or an offset which is not pointing at an image file directory.
     *
     * @param offset The offset of the image file directory
     * @return The image file directory starting from the given offset
     * @throws DNGReadException If something goes wrong during tiff parsing
     * @throws EOFException     If the end of file has been reached
     */
    public ImageFileDirectory readImageFileDirectory(long offset) throws DNGReadException, EOFException {
        Map<Integer, ImageFileDirectoryEntry> ifdEntries = readIFDEntries(offset);
        return new ImageFileDirectory(ifdEntries);
    }

    private ImageFileHeader readImageFileHeader() throws EOFException, DNGReadException {
        Objects.requireNonNull(reader);
        reader.reset();

        byte[] byteData = new byte[2];
        if (reader.read(byteData) < 2) {
            throw new DNGReadException("Could not read tiff header. This is probably not a tiff file.");
        }
        ByteOrder byteOrder;
        if (byteData[0] == ImageFileHeader.LITTLE_ENDIAN_IDENTIFIER && byteData[1] == ImageFileHeader.LITTLE_ENDIAN_IDENTIFIER) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else if (byteData[0] == ImageFileHeader.BIG_ENDIAN_IDENTIFIER && byteData[1] == ImageFileHeader.BIG_ENDIAN_IDENTIFIER) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else {
            throw new DNGReadException("Invalid byte order. This is probably not a tiff file.");
        }
        reader.setByteOrder(byteOrder);

        reader.read(byteData);
        if ((byteOrder == ByteOrder.LITTLE_ENDIAN && byteData[0] != ImageFileHeader.TIFF_FILE_IDENTIFIER)
                || (byteOrder == ByteOrder.BIG_ENDIAN && byteData[1] != ImageFileHeader.TIFF_FILE_IDENTIFIER)) {
            throw new DNGReadException("Invalid tiff file identifier. This is probably not a tiff file.");
        }

        long firstIFDOffset = reader.readUnsignedLong();

        return new ImageFileHeader(byteOrder, firstIFDOffset);
    }

    private Map<Integer, ImageFileDirectoryEntry> readIFDEntries(long offset) throws
            EOFException, DNGReadException {
        reader.reset();
        reader.skipNBytes(offset);
        Map<Integer, ImageFileDirectoryEntry> ifdEntries = new HashMap<Integer, ImageFileDirectoryEntry>();
        int numDirEntries = reader.readUnsignedShort();
        for (int currentDirEntryNum = 0; currentDirEntryNum < numDirEntries; currentDirEntryNum++) {
            int tag = reader.readUnsignedShort();
            DNGFieldType fieldType = DNGFieldType.getFieldType(reader.readUnsignedShort());
            if (fieldType != null) {
                long count = reader.readUnsignedLong();
                Object value;
                if ((long) fieldType.bytes * count > DNGConstants.IFD_ENTRY_FIELD_VALUE_OFFSET_SIZE) {
                    long valueOffset = reader.readUnsignedLong();
                    long markedPosition = reader.markCurrentPosition();
                    reader.reset();
                    reader.skipNBytes(valueOffset);
                    if (count == 1) {
                        value = reader.read(fieldType);
                    } else {
                        value = readValues(fieldType, count);
                    }
                    reader.reset();
                    reader.skipNBytes(markedPosition);
                } else {
                    if (count == 1) {
                        value = reader.read(fieldType);
                    } else {
                        value = readValues(fieldType, count);
                    }
                    reader.skipNBytes(DNGConstants.IFD_ENTRY_FIELD_VALUE_OFFSET_SIZE - (fieldType.bytes * count));
                }
                ifdEntries.put(tag, new ImageFileDirectoryEntry(tag, fieldType, count, value));
            }
        }
        return ifdEntries;
    }

    private Object readValues(DNGFieldType fieldType, long count) throws EOFException {
        Object value;
        if (fieldType == DNGFieldType.ASCII) {
            String[] values = new String[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readAscii();
            }
            value = values;
        } else if (fieldType == DNGFieldType.SHORT) {
            int[] values = new int[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readUnsignedShort();
            }
            value = values;
        } else if (fieldType == DNGFieldType.LONG) {
            long[] values = new long[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readUnsignedLong();
            }
            value = values;
        } else if (fieldType == DNGFieldType.RATIONAL) {
            Rational[] values = new Rational[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readRational();
            }
            value = values;
        } else if (fieldType == DNGFieldType.SBYTE) {
            byte[] values = new byte[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readSignedByte();
            }
            value = values;
        } else if (fieldType == DNGFieldType.SSHORT) {
            short[] values = new short[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readSignedShort();
            }
            value = values;
        } else if (fieldType == DNGFieldType.SLONG) {
            int[] values = new int[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readSignedLong();
            }
            value = values;
        } else if (fieldType == DNGFieldType.SRATIONAL) {
            SignedRational[] values = new SignedRational[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readSignedRational();
            }
            value = values;
        } else if (fieldType == DNGFieldType.SRATIONAL) {
            SignedRational[] values = new SignedRational[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readSignedRational();
            }
            value = values;
        } else if (fieldType == DNGFieldType.FLOAT) {
            float[] values = new float[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readFloat();
            }
            value = values;
        } else if (fieldType == DNGFieldType.DOUBLE) {
            double[] values = new double[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readDouble();
            }
            value = values;
        } else {
            short[] values = new short[(int) count];
            for (int i = 0; i < count; i++) {
                values[i] = reader.readUnsignedByte();
            }
            value = values;
        }
        return value;
    }

    private void assignCompressionDecoder(int compression, int bitsPerSample) throws DNGReadException {
        switch (compression) {
            case DNGTagConstants.COMPRESSION__UNCOMPRESSED:
                decoder = new UncompressedDecoder(bitsPerSample, reader.getByteOrder());
                break;
            case DNGTagConstants.COMPRESSION__JPEG_DCT_OR_LOSSLESS:
                decoder = new LosslessJPEGDecoder();
                break;
            case DNGTagConstants.COMPRESSION__CCITT_1D:
            case DNGTagConstants.COMPRESSION__DEFLATE:
            case DNGTagConstants.COMPRESSION__GROUP_3_FAX:
            case DNGTagConstants.COMPRESSION__GROUP_4_FAX:
            case DNGTagConstants.COMPRESSION__JPEG:
            case DNGTagConstants.COMPRESSION__JPEG_LOSSY:
            case DNGTagConstants.COMPRESSION__LZW:
            case DNGTagConstants.COMPRESSION__PACKBITS:
            default:
                throw new DNGReadException(String.format("The Compression %s is currently not supported", compression));
        }
    }

    private int[] parseStrippedImage(ImageFileDirectory ifd, int[] image, long imageLength) throws EOFException, CompressionDecoderException {
        long[] stripOffsets = ifd.getStripOffsets();
        long[] stripByteCounts = ifd.getStripByteCounts();
        // long rowsPerStrip = ifd.getRowsPerStrip();
        // int stripsPerImage = (int) java.lang.Math.floor((imageLength + rowsPerStrip - 1) / (double) rowsPerStrip);

        //we assume that all values of bitsPerSample are equal since it is very uncommon that they differ
        //currently only stripsPerImage = 1 is supported
        for (int i = 0; i < stripOffsets.length; i++) {
            long stripOffset = stripOffsets[i];
            long stripByteCount = stripByteCounts[i];
            reader.reset();
            reader.skipNBytes(stripOffset);

            byte[] strip = new byte[(int) stripByteCount];
            reader.read(strip);
            int[] decodedStrip = decoder.decode(strip);
            int offset = i * decodedStrip.length;
            int remainingSpace = image.length - offset;
            System.arraycopy(decodedStrip, 0, image, offset, Math.min(remainingSpace, decodedStrip.length));
        }

        return image;
    }

    private int[] parseTiledImage(ImageFileDirectory imageFileDirectory, int[] image, long imageWidth,
                                  long imageLength)
            throws EOFException, CompressionDecoderException {
        long tileWidth = imageFileDirectory.getTileWidth();
        long tileLength = imageFileDirectory.getTileLength();
        long[] tileOffsets = imageFileDirectory.getTileOffsets();
        long[] tileByteCounts = imageFileDirectory.getTileByteCounts();

        //see TIFF 6 specification, page 67
        int tilesAcross = (int) ((imageWidth + tileWidth - 1) / tileWidth);
        int tilesDown = (int) ((imageLength + tileLength - 1) / tileLength);

        int horizontalPadding = (int) ((tileWidth * tilesAcross) % imageWidth);
        int verticalPadding = (int) ((tileLength * tilesDown) % imageLength);

        for (int tileNum = 0; tileNum < tileOffsets.length; tileNum++) {
            long tileOffset = tileOffsets[tileNum];
            long tileByteCount = tileByteCounts[tileNum];
            reader.reset();
            reader.skipNBytes(tileOffset);

            byte[] tile = new byte[(int) tileByteCount];
            reader.read(tile);
            int[] decodedTile = decoder.decode(tile);

            int currentTileRow = tileNum / tilesAcross;
            int offset = (int) (currentTileRow * tileWidth * tileLength * tilesAcross);
            int tileNumInRow = (int) ((tileNum % tilesAcross) * tileWidth);
            for (int j = 0; j < tileLength; j++) {
                int currentRow = (int) (j + currentTileRow * tileLength);
                if (currentRow >= (tilesDown * tileLength) - verticalPadding - 1) {
                    break;
                }
                int startIndex = tileNumInRow + (int) (j * tileWidth * tilesAcross) + offset - (currentRow * horizontalPadding);
                System.arraycopy(decodedTile, (int) (j * tileWidth), image, startIndex, (int) tileWidth);
            }
        }

        return image;
    }

    public File getFile() {
        return file;
    }
}
