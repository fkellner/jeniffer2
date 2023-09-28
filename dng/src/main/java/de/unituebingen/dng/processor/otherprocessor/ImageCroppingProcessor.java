package de.unituebingen.dng.processor.otherprocessor;

import de.unituebingen.dng.processor.Processor;
import de.unituebingen.dng.reader.ImageFileDirectory;
import de.unituebingen.dng.reader.util.Rational;

import java.awt.image.BufferedImage;

public class ImageCroppingProcessor implements Processor<BufferedImage> {

    private long[] activeArea;
    private Rational[] defaultCropOrigin;
    private int width;
    private int length;

    public ImageCroppingProcessor(ImageFileDirectory hrIfd) {
        this.activeArea = hrIfd.getActiveArea();
        this.defaultCropOrigin = hrIfd.getDefaultCropOrigin();
        Rational[] defaultCropSize = hrIfd.getDefaultCropSize();

        this.activeArea = this.activeArea != null ? this.activeArea : new long[]{0, 0, hrIfd.getImageLength(), hrIfd.getImageWidth()};
        this.defaultCropOrigin = this.defaultCropOrigin != null ? this.defaultCropOrigin :
                new Rational[]{new Rational(0, 1), new Rational(0, 1)};
        defaultCropSize = defaultCropSize != null ? defaultCropSize :
                new Rational[] { new Rational(hrIfd.getImageWidth(), 1), new Rational(hrIfd.getImageLength(), 1) };
        width = defaultCropSize[0].intValue();
        length = defaultCropSize[1].intValue();
    }

    @Override
    public BufferedImage process(BufferedImage bufferedImage) {
        int startWidth = (int) (defaultCropOrigin[0].intValue() + activeArea[1]);
        int startLength = (int) (defaultCropOrigin[1].intValue() + activeArea[0]);

        return bufferedImage.getSubimage(startWidth, startLength, width, length);
    }
    
    public boolean isNonPOTCrop() {
        return width % 2 != 0; // || length % 2 != 0;
    }
}
