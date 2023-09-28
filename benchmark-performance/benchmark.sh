#!/bin/bash

# copy in CLI
# cd ../cli
# mvn package
# cp target/Jeniffer2-Cli-1.1-jar-with-dependencies.jar ../benchmark-performance/jeniffer2-cli.jar
# cd ../benchmark-performance

# exit 0

##### test algorithms on GPU

# for alg in BILINEAR_MEAN RCD
# do

# test without tiling
# for acc in GPU_OPERATION_WISE GPU_TILE_WISE
# do
# java -Xmx4g -jar jeniffer2-cli.jar -a $acc -i $alg pier.dng
# done # /for acc in ...

# test with tiling
# for acc in GPU_TILE_WISE
# do
# for TILE_SIZE in 512 256 128 64 32
# do
# java -Xmx4g -jar jeniffer2-cli.jar -a $acc -i $alg pier.dng

# done # /for size in ...
# done # /for acc in ...

# done # /for alg in ...

# exit 0

##### Test all Algorithms on CPU

for alg in NONE NEAREST_NEIGHBOR BILINEAR_MEAN BILINEAR_MEDIAN BICUBIC MALVAR_HE_CUTLER HAMILTON_ADAMS PPG RCD DLMMSE_CODE DLMMSE_PAPER DLMMSE_RCD_CODE DLMMSE_RCD_PAPER
do

echo "### $alg"

# test without tiling
# for acc in NONE MULTITHREADING
# do
# java -Xmx4g -jar jeniffer2-cli.jar -a $acc -i $alg pier.dng
# done # /for acc in ...

# test with tiling
for acc in CPU_MT_TILING CPU_MT_TILING_MT # CPU_TILING CPU_TILING_MT
do
for size in 512 256 128 64 32
do
TILE_SIZE=$size java -Xmx4g -jar jeniffer2-cli.jar -a $acc -i $alg pier.dng

done # /for size in ...
done # /for acc in ...

done # /for alg in ...
