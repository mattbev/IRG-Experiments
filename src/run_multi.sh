#!/bin/bash
for game in "aliens0" "missilecommand0" "sheriff0" "solarfox0" "butterflies0" "firestorms0" "firecaster0"; do 
# for game in "plaqueattack0" "defender0" "avoidgeorge0" "witnessprotection0" "jaws0" "waves0" "crossfire0"; do 
  ./run.sh run -s $game -ns 10000 -c qs,rs -a 20 -i 10 &
done;
