package de.unituebingen.dng.reader.dng.opcode;

public class FixBadPixelsConstant extends Opcode {

    private long constant;
    private long bayerPhase;

    public FixBadPixelsConstant(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        super(id, dngSpecVersion, flags, numberOfBytes, data);
        init();
    }

    private void init() {
        constant = data.getInt() & 0xFFFFFFFFL;
        bayerPhase = data.getInt() & 0xFFFFFFFFL;
    }

    public long getConstant() {
        return constant;
    }

    public long getBayerPhase() {
        return bayerPhase;
    }
}
