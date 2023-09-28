package de.unituebingen.dng.reader.dng.opcode;

public class FixBadPixelsList extends Opcode {

    private long bayerPhase;
    private int badPointCount;
    private int badRectCount;

    private long[][] badPoints;
    private long[][] badRects;

    public FixBadPixelsList(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        bayerPhase = data.getInt() & 0xFFFFFFFFL;
        badPointCount = (int) (data.getInt() & 0xFFFFFFFFL);
        badRectCount = (int) (data.getInt() & 0xFFFFFFFFL);

        badPoints = new long[badPointCount][2];
        for (int i = 0; i < badPointCount; i++) {
            badPoints[i][0] = data.getInt() & 0xFFFFFFFFL;
            badPoints[i][1] = data.getInt() & 0xFFFFFFFFL;
        }

        badRects = new long[badRectCount][4];
        for (int i = 0; i < badRectCount; i++) {
            badRects[i][0] = data.getInt() & 0xFFFFFFFFL;
            badRects[i][1] = data.getInt() & 0xFFFFFFFFL;
            badRects[i][2] = data.getInt() & 0xFFFFFFFFL;
            badRects[i][3] = data.getInt() & 0xFFFFFFFFL;
        }
    }

    public long getBayerPhase() {
        return bayerPhase;
    }

    public int getBadPointCount() {
        return badPointCount;
    }

    public int getBadRectCount() {
        return badRectCount;
    }

    public long[][] getBadPoints() {
        return badPoints;
    }

    public long[][] getBadRects() {
        return badRects;
    }
}
