# JENIFFER2

![Jeniffer2 Logo](jeniffer2-logo.png)

![Build Status](https://github.com/fkellner/jeniffer2/actions/workflows/publish.yml/badge.svg) ![OpenJDK 17](https://badgen.net/static/OpenJDK/17/green?icon=github) ![License: GPLv3](https://badgen.net/static/License/GPLv3/green?icon=github)

Jeniffer2 is a raw image processor written in Java. It offers
- a big choice of demosaicing algorithms
- the option to export to TIFF, JPEG or PNG
- tools to inspect metadata
- tools for comparing the results of different algorithms

It has been designed to be extensible (at least regarding demosaicing algorithms)
and portable. 

Jeniffer2 has been developed as part of three master theses at 
Tübingen University, which can be downloaded [here](https://github.com/fkellner/jeniffer2-theses).

_Fun fact: JENIFFER stands for **J**ava **E**nhanced **N**ef **I**mage **F**ile **F**ormat **E**dito**R**. Version 2 is using the Adobe licensed open DNG format as input instead of the proprietary Nikon NEF format._

## Installing and Running Jeniffer2

- [User Manual (PDF, en)](https://github.com/fkellner/jeniffer2/releases/download/latest/USER_MANUAL.pdf)
- [User Manual (PDF, de)](https://github.com/fkellner/jeniffer2/releases/download/latest/USER_MANUAL.de.pdf)
- [Pre-Compiled JARs of GUI and CLI, and experimental stand-alone binaries](https://github.com/fkellner/jeniffer2/releases/tag/latest)

## Demo

![Jeniffer2 algorithm comparison demo](Jeniffer2-demo.mp4)

# Development

## Subprojects

### `/dng`

DNG processing library with a few tests.

### `/cli`

CLI frontend for the library, for manual testing.

### `/ui`

Distributable GUI frontend. 

### `benchmark-accuracy`, `benchmark-performance`

Shell-Scripts using the CLI and some example images to test the accuracy of the different algorithms
and the performance of their implementation, and generate some plots from the collected data.

## Build instructions

### Prerequisites

This java project uses [Apache Maven](https://maven.apache.org) as a build tool.
The language version specified in the project is 17, so you need JDK 17 or higher
(check with `java -version` (note the single dash!) from the command line/shell
of your convenience).

_Tip: On Debian-based Linux systems, you can install a specific java version via `apt install openjdk-<version>-jdk` (at the time of writing, the latest stable version is 17)._

### Building and packaging using `make`

For your convenience, you can build the project using `make build` if you have CMake or something similar installed.

You need to have it installed in order to run the (hopefully) platform-independent cross-platform
package script with `make package`, which uses the great [`warp-packer`](https://github.com/fintermobilityas/warp#quickstart-with-java) to produce standalone binaries (`*.bin` and `*.exe`). A helper for this are the `run-<os>.sh` and `run.bat` files.

For the windows binary, it may happen that unpacking the archive fails. Then you need to manually go to `C -> Users -> <Username> -> AppData -> Local` (hidden folder, setting to show hidden files can be found in the "View" Tab in the "Show/Hide" section in your explorer) and delete the `warp` folder.

Note that this downloads the `warp-packer` tool as well as JREs for each platform, although they are not committed to version control, they will not be re-downloaded if they already are present.

### Building the subprojects using maven

The following instructions assume you use Maven from the command line. You can
of course run the same Maven Actions from your IDE, if supported.

**Building the DNG reader**

Switch to the `dng` folder/project and run `mvn install` to publish the package to your local repository, so that it
can be imported and used by the UI. If you want to run a specific Test, you can do so with `mvn test -Dtest=*TestName`.
If you want to skip tests e.g. for trying something out visually, you can do `mvn install -Dmaven.test.skip`.

**Building the CLI**

Switch to the `cli` folder/project and run `mvn [clean] package`.
Details in Sub-README.

**Building the UI**

Switch to the `ui` folder/project and run `mvn package`. The compiled JAR can be
found in the generated `target` folder.
Run it with `java -jar Jennifer2-<version>-jar-with-dependencies.jar`. A test
DNG file can be downloaded e.g.
[here](https://www.vesta.uni-tuebingen.de/webfoto/20212/Bild20212_01.dng).
On MacOS, you max need to add the `-XStartOnFirstThread` flag if you want to use GPU, like so:
```sh
java -XStartOnFirstThread -jar Jennifer2-<version>-jar-with-dependencies.jar
```

### Creating the User Manual PDFs

The Markdown files `USER_MANUAL*.md` can be converted to PDF using [pandoc](https://pandoc.org/).

The commands that need to be run are:
```sh
pandoc -f markdown -t latex -V linkcolor:lightblue -o USER_MANUAL.pdf USER_MANUAL.md
pandoc -f markdown -t latex -V linkcolor:lightblue -o USER_MANUAL.de.pdf USER_MANUAL.de.md
```
Alternatively, if you have Make available, you can simply run
```sh
make manual
```

## Contributing

Feel free to open an issue or pull request! There is currently one
single maintainer to this project, so you may expect a response time
of several days. 

This project is probably too small for a formal 
code of conduct, but please be civil to each other and do not discriminate based
on race, sexuality, gender or anything else. 

# Credits

Jeniffer2 has been developed under the supervision of [Prof. Dr. Thomas Walter](https://uni-tuebingen.de/fakultaeten/mathematisch-naturwissenschaftliche-fakultaet/fachbereiche/informatik/lehrstuehle/informationsdienste/team/thomas-walter/) at Tübingen University.

Credits go to:

- Eugen Ljavin (who wrote the DNG processing and original AfterburnerFX/JavaFX app)
- Joachim Groß
- Michael Kessler
- Claudia Grosch
- Andreas Reiter (who added the more advanced demosaicing algorithms)
- [Florian Kellner](https://github.com/fkellner) (who added comparison tools, experimental GPU support and maintains this repo)

# Legal notice

* This product includes DNG technology under license by Adobe.
* Dieses Produkt enthält die bei Adobe lizenzierte DNG-Technologie.
