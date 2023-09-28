module de.unituebingen.dng {
    requires com.github.oshi;
    requires org.lwjgl.opengl;
    requires org.lwjgl.glfw;
    requires org.slf4j.nop;
    requires commons.math3;
    requires tornado.api;
    requires transitive java.desktop;


    exports de.unituebingen.dng;
    exports de.unituebingen.dng.reader;
    exports de.unituebingen.dng.processor.util;
    exports de.unituebingen.imageprocessor;
    exports de.unituebingen.dng.reader.compression;
    exports de.unituebingen.dng.reader.dng.util;
    exports de.unituebingen.dng.processor.demosaicingprocessor;
}
