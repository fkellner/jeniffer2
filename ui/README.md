# JENIFFER2

This package contains the GUI for the DNG processor implemented in the `dng` project/`de.unituebingen.dng` module.

## Dependencies

Dependencies are specified both in the `pom.xml` to be picked up by Maven and in the `src/main/java/module-info.java` file to be picked up by the Java Module System. 

More on the Java Module System, which exists since Java 11 and can be used to better package applications
can be found here:
https://dev.java/learn/introduction-to-modules-in-java/
https://dev.java/learn/incremental-modularization-with-automatic-modules/

The main motivation for packaging this as a module is that JavaFX requires it, and it hopefully helps with packaging.

We currently depend on:

- Our DNG processor, which needs to be installed via the `mvn install` command to be available on your system. The DNG processor packs some native libraries and uses JNI access for info about the hardware.
- [**JavaFX**](https://openjfx.io/) for the UI
- [**Airhacks Afterburner FX**](https://github.com/AdamBien/afterburner.fx), more info [here](https://training.cherriz.de/cherriz-training/1.0.0/oberflaechen/afterburner.fx.html) as JavaFX framework in order to e.g. be able to use properties and auto-inject dependencies. This framework is responsible for the component structure of the project. This component heavily relies on reflection. Its module name is inferred from the file name, which should be that same as the maven artifact name, but may change on your system!
- [**FontawesomeFX**](https://www.jensd.de/wordpress/?tag=fontawesomefx) in order to be able to use Fontawesome Icons in the GUI
- [**JavaX Annotation API**](https://github.com/javaee/javax.annotation) may be needed by Afterburner??

## Legal notice

* This product includes DNG technology under license by Adobe.
* Dieses Produkt enthält die bei Adobe lizenzierte DNG-Technologie.
