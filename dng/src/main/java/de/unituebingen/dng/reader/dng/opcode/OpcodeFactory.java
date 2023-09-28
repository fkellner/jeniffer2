package de.unituebingen.dng.reader.dng.opcode;

public class OpcodeFactory {

    private OpcodeFactory() {
        //empty on purpose
    }

    public static Opcode getOpcodeByID(long id, long dngSpecVersion, long flags, long numberOfBytes, byte[] data) {
        Opcode opcode;

        if (id == 1) {
            opcode = new WarpRectilinear(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 2) {
            opcode = new WarpFisheye(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 3) {
            opcode = new FixVignetteRadial(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 4) {
            opcode = new FixBadPixelsConstant(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 5) {
            opcode = new FixBadPixelsList(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 6) {
            opcode = new TrimBounds(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 7) {
            opcode = new MapTable(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 8) {
            opcode = new MapPolynomial(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 9) {
            opcode = new GainMap(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 10) {
            opcode = new DeltaPerRow(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 11) {
            opcode = new DeltaPerColumn(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 12) {
            opcode = new ScalesPerRow(id, dngSpecVersion, flags, numberOfBytes, data);
        } else if (id == 13) {
            opcode = new ScalesPerColumn(id, dngSpecVersion, flags, numberOfBytes, data);
        } else {
            throw new IllegalArgumentException("Could not find Opcode with id " + id);
        }

        return opcode;
    }
}
