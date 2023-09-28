#!/bin/bash

# copy in CLI
cd ../cli
mvn package
cp target/Jeniffer2-Cli-1.1-jar-with-dependencies.jar ../benchmark-accuracy/jeniffer2-cli.jar
cd ../benchmark-accuracy

# directory for results
test -d results || mkdir results

test -e benchmark.csv || echo "alg,dataset,image,mse,psnr,mssim" > benchmark.csv
BM_ROOT=$(pwd)

######## demosaic images on CPU

for alg in NONE NEAREST_NEIGHBOR BILINEAR_MEAN BILINEAR_MEDIAN BICUBIC MalvarHeCutler HAMILTON_ADAMS PPG RCD RCD_REWRITE DLMMSE_REWRITE_CODE DLMMSE_REWRITE_PAPER DLMMSE_CODE DLMMSE_PAPER DLMMSE_RCD DLMMSE_RCD_PAPER DLMMSE_RCD_REWRITE DLMMSE_RCD_REWRITE_PAPER
do

echo "Handling algorithm $alg"

test -d results/$alg || mkdir results/$alg

for dataset in cambridge # kodak mcmaster
do

echo "$alg, Dataset: $dataset"

mkdir results/$alg/$dataset

cd mosaic/$dataset
for f in *.png
do

echo "$alg, $dataset, file: $f"
# compute image
echo "java -jar $BM_ROOT/jeniffer2-cli.jar $f -a REORDER -i $alg -p RGGB -l NOP -o $BM_ROOT/results/$alg/$dataset/$f"
java -jar $BM_ROOT/jeniffer2-cli.jar $f -a REORDER -i $alg -p RGGB -l NOP -o $BM_ROOT/results/$alg/$dataset/$f

echo "MARGIN=$(java -jar $BM_ROOT/jeniffer2-cli.jar -i $alg -m)"
MARGIN=$(java -jar $BM_ROOT/jeniffer2-cli.jar -i $alg -m)

# compute metrics
echo "adding metrics"
echo "$alg,$dataset,$f,$($BM_ROOT/dlmmse/imdiff $BM_ROOT/truth/$dataset/$f $BM_ROOT/results/$alg/$dataset/$f -p $MARGIN -m mse),$($BM_ROOT/dlmmse/imdiff $BM_ROOT/truth/$dataset/$f $BM_ROOT/results/$alg/$dataset/$f -p $MARGIN -m psnr),$($BM_ROOT/dlmmse/imdiff $BM_ROOT/truth/$dataset/$f $BM_ROOT/results/$alg/$dataset/$f -p $MARGIN -m mssim)" >> $BM_ROOT/benchmark.csv

done # /for f in ...

cd ../..

done # /for dataset in ...

done # /for alg in ...


########## demosaic images on GPU

for alg in BILINEAR_MEAN RCD
do

test -d results/$alg-GPU || mkdir results/$alg-GPU

for dataset in cambridge # kodak mcmaster
do

mkdir results/$alg-GPU/$dataset

cd mosaic/$dataset
for f in *.png
do
# compute image
java -jar $BM_ROOT/jeniffer2-cli.jar $f -a GPU_TILE_WISE -i $alg -p RGGB -l NOP -o $BM_ROOT/results/$alg-GPU/$dataset/$f
MARGIN=$(java -jar $BM_ROOT/jeniffer2-cli.jar -i $alg -m)
# compute metrics
echo "$alg-GPU,$dataset,$f,$($BM_ROOT/dlmmse/imdiff $BM_ROOT/truth/$dataset/$f $BM_ROOT/results/$alg-GPU/$dataset/$f -p $MARGIN -m mse),$($BM_ROOT/dlmmse/imdiff $BM_ROOT/truth/$dataset/$f $BM_ROOT/results/$alg-GPU/$dataset/$f -p $MARGIN -m psnr),$($BM_ROOT/dlmmse/imdiff $BM_ROOT/truth/$dataset/$f $BM_ROOT/results/$alg-GPU/$dataset/$f -p $MARGIN -m mssim)" >> $BM_ROOT/benchmark.csv

done # /for f in ...

cd ../..

done # /for dataset in ...

done # /for alg in ...