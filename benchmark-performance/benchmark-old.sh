# Benchmark old version of Jeniffer2, before the grand refactoring (no linearization/saved intermediate results)
# jeniffer2-cli-old.jar needs to be obtained e.g. from commit 7de31c1 ("Nice plots")

test -d old || mkdir old
cp pier.dng old
cd old

for alg in NONE NEAREST_NEIGHBOR BILINEAR_MEAN BILINEAR_MEDIAN BICUBIC MalvarHeCutler HAMILTON_ADAMS PPG RCD DLMMSE_CODE DLMMSE_PAPER DLMMSE_RCD DLMMSE_RCD_PAPER
do

echo "### $alg"

# test without tiling
for acc in NONE MULTITHREADING
do
java -Xmx4g -jar jeniffer2-cli-old.jar -a $acc -i $alg pier.dng

# append logs with acc. method and task changed
cat jeniffer2-logs/timing.csv | tail -n+2 | sed 's/4,"None/4,"None-OLD/g' | sed 's/Multithreading/Multithreading-OLD/g' | sed "s/DemosaicingProcessor/$alg-alg/g" | sed 's/MalvarHeCutler-alg/MALVAR_HE_CUTLER/g' | sed 's/-alg//g' >> ../jeniffer2-logs/timing.csv
# delete timing logs, but keep file to speed up things
head -n1 jeniffer2-logs/timing.csv > jeniffer2-logs/timing.csv

done # /for acc in ....

done # /for alg in ...

cd ..
