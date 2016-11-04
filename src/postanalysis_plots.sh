#!/bin/bash

echo "Running make_plots"
echo ${1}/*/
for D in ${1}/*/; do
	if [ -d $D ]; then
		javac ramyaram/AllRewardAnalysis.java && java ramyaram/AllRewardAnalysis $D
		python plots.py $D "postanalysis_reward.csv"
	fi
done
