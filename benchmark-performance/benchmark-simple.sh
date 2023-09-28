#!/bin/bash

for alg in BILINEAR_MEAN RCD
do

for acc in NONE MULTITHREADING GPU_TILE_WISE GPU_OPERATION_WISE CPU_TILING_MT
do
java -Xmx4g -jar jeniffer2-cli.jar -a $acc -i $alg pier.dng
done # /for acc in ...

done # /for alg in ..