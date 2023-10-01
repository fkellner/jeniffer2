# DNG Reader / Processor

This package contains all logic pertaining to the processing of DNG images. 
It provides the `de.unituebingen.dng` platform module, which is intended to be used from the
`Facade` class.

## Dependencies

Dependencies are specified both in the `pom.xml` to be picked up by Maven and in the `src/main/java/module-info.java` file to be picked up by the Java Module System. 

More on the Java Module System, which exists since Java 11 and can be used to better package applications
can be found here:
https://dev.java/learn/introduction-to-modules-in-java/
https://dev.java/learn/incremental-modularization-with-automatic-modules/

The main motivation for packaging this as a module is to make it better usable by the ui module,
since the framework used in the ui module requires the use of modules.

We currently depend on:

- [**Apache Commons (Math)**](https://commons.apache.org/proper/commons-math/index.html) for mathematical calculations - the module name is automatically inferred from the file name, which should be the same as the artifact name but may change on your system!
- [**LWJGL**](https://www.lwjgl.org/) for its `opengl` and `glfw` modules to create a hidden window and use its OpenGL context to (optionally) speed up some processors. This requires some native libraries which are included for most platforms in the `pom.xml` in order to create a fat jar that runs on those platforms.
- [**OSHI**](https://github.com/oshi/oshi) for obtaining and logging host system information ([how to include as module](https://github.com/oshi/oshi/blob/master/src/site/markdown/FAQ.md)) - it uses the [JNA](https://github.com/java-native-access/jna) package, which in turn uses JNI calls. This package also requires us to include a SLF4J implementation, since this is not really needed the [**`NOP`**](https://mvnrepository.com/artifact/org.slf4j/slf4j-nop/2.0.6) implementation which ignores all calls has been included.
- [**TornadoVM**](https://tornadovm.readthedocs.io/en/latest/installation.html) for OpenCL/CUDA/PTX acceleration, where the relevant acceleration strategy has been implemented. For this, you need to install TornadoVM and run Jeniffer2 as JAR not with `java`, but `tornadovm`. Methods that can be executed as Tasks are currently limited to static methods without multiple conditionals or Objects (see `ReadableRCD` for an example). Currently, TornadoVM seems to have issues with Reflection in the GUI and can only be tested from the Cli.

## Code Formatting

Code Formatter settings are in `eclipse-formatter.xml`. If you start VSCode in this folder, it should pick up the settings in `.vscode` automatically.

## Installing without a compatible graphics card

This Package contains some tests that depend on an OpenGL-compatible Graphics Card to be present.
However, DNG processing also works without a Graphics Card. In order to compile this module on 
a system without an OpenGL-compatible Graphics Card, you can exclude the corresponding tests 
with:
```sh
mvn install '-Dtest=!de.unituebingen.opengl.**'
```

## Testing

In order to just execute one specific test, run
```sh
mvn test -Dtest=*TestClassName
```

For a quick build without tests (to e.g. check stuff visually in the GUI), run
```sh
mvn install -Dmaven.test.skip
```

## Legal notice

* This product includes DNG technology under license by Adobe.
* Dieses Produkt enthält die bei Adobe lizenzierte DNG-Technologie.
