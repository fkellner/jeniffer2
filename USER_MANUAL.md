# JENIFFER2 User Manual

![Jeniffer2 Logo](jeniffer2-logo.png)

_Deutsche Version ist [hier verfügbar](USER_MANUAL.de.md)._

## About

JENIFFER stands for **J**ava **E**nhanced **N**ef **I**mage **F**ile **F**ormat **E**dito**R**.
Version 2 is using the Adobe licensed open DNG format as input instead of the proprietary Nikon NEF format.

JENIFFER2 is Open Source Software for developing DNG files to TIFF, JPEG or PNG,
offering a big choice of demosaicing algorithms.

## Prerequisites

### Pre-Packaged Binaries

We currently provide 3 binaries:

- `jeniffer2-linux-x64.bin` for Linux Systems with Intel or AMD processors
- `jeniffer2-windows-x64.exe` for Windows Systems with Intel or AMD processors
- `jeniffer2-mac-x64.bin` for MacOS Systems with Intel processors

And an experimental binary for when you are on a mac with aarch64 (Apple Silicon/M1 chips).

If you are on an ARM architecture or a mac with aarch64 (Apple Silicon/M1) you
need to use the Jar distribution.

_If you are on an entirely different OS or use 32bit-Architecture, you need the source code to add some native libraries and compile Jeniffer2 yourself._

### Jar Distribution

JENIFFER requires you to have a Java Runtime Environment (JRE) of Version 17 or
higher installed.

You can check the version of your installed JRE by opening a command prompt
(Windows) or terminal (MacOS/Linux) of your choice and running `java -version`.
Modern Linux distributions should already have a version of Java installed.

We recommend using the OpenJDK JRE:

- On Linux, most distributions already provide the `openjdk-17-jre` or `openjdk-19-jre` package, which can e.g. on Debian-based systems installed via `sudo apt-get install openjdk-17-jre`.
- Eclipse Temurin provides [Downloadable Installers](https://adoptium.net/temurin/releases/) for all operating systems as well as Installation Instructions ([Windows](https://adoptium.net/installation/windows), [MacOS](https://adoptium.net/installation/macOS), [Linux](https://adoptium.net/installation/linux))

Windows users should make sure to check the box to **update the `JAVA_HOME` environment variable** during the installation process in order to be able to run Java from the command line and pass options such as allocating more memory.

### Raw images in DNG format

Most proprietary RAW image formats like `.NEF` can be converted to DNG either via
the [Adobe DNG Converter](https://helpx.adobe.com/de/camera-raw/using/adobe-dng-converter.html)
(runs on MacOS and Windows) or an online tool like [this one](https://www.onlineconverter.com/image).

## Running JENIFFER2

How you run Jeniffer2 depends on your OS and Distribution:

### Binary Distribution

You can simply double-click the executable to run it (_on MacOS/Linux, you may need to make the file executable with `chmod +x` if this does not work_). If you want to see some interesting timing output or debug issues, you can run it from a terminal/command prompt.

**Known Issues:**
- If running on Windows fails with an Error like `Error: Custom ( kind: Other, error: StringError("no tarball found inside binary") )`, going to `C -> Users -> <Username> -> AppData -> Local` (hidden folder, setting to show hidden files can be found in the "View" Tab in the "Show/Hide" section in your explorer) and deleting the `warp` folder may help, then you can retry.

### Jar Distribution

Tu run the Java Archive, open a terminal/command prompt
in the folder where you saved it and run
```sh
# on linux and windows
java -jar jeniffer2.jar
# on MacOS
java -XstartOnFirstThread -jar jeniffer2.jar
```

### Using the right graphics card

Some Laptops have both an energy efficient and a high performance graphics card built in, and the OS decides which one to use depending on energy profile and other settings. If you test the algorithms running on the GPU and get the error box telling you to look at your console output, and there is the `OpenGL Error 1285`, you are probably using the wrong graphics card.

On Windows, you can set which graphics card to use on a per-Application basis: Just go to `Settings -> Graphics settings`, then in "Choose an app to set preference" choose "Desktop app" and click on browse to select the Java Binary of your Runtime environment.

This will most probably be somewhere in your `C:\Users\YOUR_NAME\AppData` directory, so you need to unhide hidden folders in you Explorer -> View settings tab at the top. If you used the binary distribution, your path will be `C:\Users\YOUR_NAME\AppData\Local\warp\packages\jeniffer2-windows-x64.exe\jre\bin\java.exe`.

### Adding more RAM

Java programs have a fixed maximum memory allocation (heap size), the default on
your system may be too low. To allow Jennifer2 to use more memory, you can explicitly
set the maximum, e.g. like this:
```sh
java -Xmx8192M -jar Jeniffer2.1.jar
```
for 8GB of RAM or like this:
```sh
java -Xmx4096M -jar Jeniffer2.1.jar
```
for 4GB of RAM. **Never set this value to the amount of RAM available on your system!**
Other programs as well as your operating system need some RAM, too.

### Saved GUI state

Jeniffer2 writes a hidden `folderSave` file to the folder it is run in, where it
stores the last location you opened in the file tree view, so that it will be expanded
when you open the program again.

### Logs

Jeniffer2 creates a `jeniffer2-logs` folder in the folder it is run in. It stores
performance and system information in CSV format. You can open and inspect these
files with a text editor, python, R, Excel...if you are curious, but if you want
to contribute to the research concerning Jeniffer2 development, please do not edit
them before handing them in.

## Credits

Jeniffer2 has been developed under the supervision of Prof. Thomas Walter at Tübingen University.

Credits go to:

- Eugen Ljavin
- Joachim Groß
- Michael Kessler
- Claudia Grosch
- Andreas Reiter
- Florian Kellner

## Source Code

We are currently working on making the source code of Jeniffer2 public.
In the meantime, you can contact [Florian Kellner](mailto:mr.florian.kellner@posteo.de)
for the source code or with any issues you might have.
