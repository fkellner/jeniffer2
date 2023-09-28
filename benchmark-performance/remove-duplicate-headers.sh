head -n1 jeniffer2-logs/timing.csv > jeniffer2-logs/timing-header.csv
cat jeniffer2-logs/timing.csv | sed 's/systemId.*$//g' > jeniffer2-logs/timing-noheaders.csv
cat jeniffer2-logs/timing-header.csv > jeniffer2-logs/timing.csv
cat jeniffer2-logs/timing-noheaders.csv >> jeniffer2-logs/timing.csv