# JENIFFER2

This package contains a CLI for the DNG processor implemented in the `dng` project/`de.unituebingen.dng` module.

The CLI is useful for quick manual testing of the DNG processor, since it is easier to port features here 
than to integrate them in the GUI. 
Also, it helps with cross-platform testing without running into UI bugs/having stacktraces removed by the GUI framework.

To use it, run
```sh
mvn clean package
```

And then in `target`:
```sh
java -jar Jeniffer2-Cli-1.1-jar-with-dependencies.jar --help
# or on mac:
java -XStartOnFirstThread -jar Jeniffer2-Cli-1.1-jar-with-dependencies.jar --help
```
to get help on how to use it.

Here a sample output, which may already have changed:
```
Process a DNG raw image file into a TIFF file
Usage: CMD [<path-to-dng-file>] [OPTIONS]
CMD:
    - Linux and Windows: java -jar Jeniffer2-Cli-1.1-jar-with-Dependencies.jar
    - MacOS: java -XStartOnFirstThread -jar Jeniffer2-Cli-1.1-jar-with-Dependencies.jar
<path-to-dng-file>:
    Default is 'test.dng'
OPTIONS:
    --output FILEPATH
    -o FILEPATH
        default: Dng File path with extension changed to .tiff
    --interpolation METHOD
    -i METHOD
        Where METHOD is one of:
NONE NEAREST_NEIGHBOR BILINEAR_MEAN BILINEAR_MEDIAN BICUBIC MalvarHeCutler HAMILTON_ADAMS PPG RCD DLMMSE_CODE DLMMSE_PAPER DLMMSE_RCD DLMMSE_RCD_PAPER 
--acceleration STRATEGY
-a STRATEGY
    Where STRATEGY is one of:
NONE MULTITHREADING GPU_OPERATION_WISE GPU_TILE_WISE 
--log LOGGER
-l LOGGER
    Where LOGGER is one of:
CONSOLE CSV NOP CSV_AND_CONSOLE 
--substep STEP
-s STEP
    Where STEP is a numbered option whose availability depends
    on the chosen interpolation method and acceleration strategy:
GPU_OPERATION_WISE RCD:
0: 0. Raw data (do nothing)
1: 1. XY-Gradient
2: 2. Low Pass Filter at Red and Blue (Green=bilinear interp.)
3: 3. Finished Interpolation of Green Pixels
4: 4. PQ-Gradient at Red and Blue (Green=bilinear interp.)
5: 5. All done except RB at G
6: 6. Finished (but no post-processing)
GPU_TILE_WISE RCD:
0: 0. Raw data (do nothing)
1: 1. XY-Gradient
2: 2. Low Pass Filter at Red and Blue (Green=bilinear interp.)
3: 3. Finished Interpolation of Green Pixels
4: 4. PQ-Gradient at Red and Blue (Green=bilinear interp.)
5: 5. All done except RB at G
6: 6. Finished (but no post-processing)
```


## Legal notice

* This product includes DNG technology under license by Adobe.
* Dieses Produkt enthält die bei Adobe lizenzierte DNG-Technologie.
