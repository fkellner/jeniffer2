package de.unituebingen.opengl;

public class PositionInImage {
    
    /**
     * origin position in the databuffer of the image this texture belongs to
     */
    public int originX;
    public int originY;

    /**
     * rectangle of pixels starts at origin minus overlap
     */
    public int overlapTop;
    public int overlapBottom;
    public int overlapLeft;
    public int overlapRight;

    /**
     * How a rectangle of pixels is situated with regards to an image
     * The overlaps describe how much to discard when copying back to the image
     * 
     * @param originX where the pixels at x=overlapLeft are in the image
     * @param originY where the pixels at y=overlapTop are in the image
     * @param overlapTop 
     * @param overlapBottom
     * @param overlapLeft
     * @param overlapRight
     */
    protected PositionInImage(
        int originX, int originY, 
        int overlapTop, int overlapBottom, int overlapLeft, int overlapRight
    ) {
        this.originX = originX;
        this.originY = originY;
        this.overlapTop = overlapTop;
        this.overlapBottom = overlapBottom;
        this.overlapLeft = overlapLeft;
        this.overlapRight = overlapRight;
    }


}
