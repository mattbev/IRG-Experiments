#!/bin/bash

if [[ $# -eq 0 ]] ; then
echo 'Usage: ./run.sh <csv filename with results> <filename to save plot>'
echo 'Example: ./run.sh reward.csv plot.pdf'
exit 1
fi

javac Main.java
java Main

python plots.py ../../$1 $2
open $2
