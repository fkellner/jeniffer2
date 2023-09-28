module de.unituebingen.jeniffer2 {
    requires transitive de.unituebingen.dng;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.swing;
    requires afterburner.fx;
    requires de.jensd.fx.glyphs.commons;
    requires de.jensd.fx.glyphs.fontawesome;
    requires java.annotation;
    // requires jakarta.annotation;

    // open controllers for reflection so they can be loaded by javafx.fxml
    opens de.unituebingen.jeniffer2;
    
    // export to be accessible by at least javafx.graphics
    exports de.unituebingen.jeniffer2;
    exports de.unituebingen.jeniffer2.util;
}