#!/bin/bash
for game in "M0" "K0" "H0"; do
    ./run.sh run -s $game -ns 10000000 -c qs -a 1 -i 10000 &
done;