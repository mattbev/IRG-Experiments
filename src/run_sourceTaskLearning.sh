#!/bin/bash
for game in "M0" "K0" "H0"; do
    ./run.sh run -s $game -ns 10000 -c qs,rs -a 50 -i 10 &
done;