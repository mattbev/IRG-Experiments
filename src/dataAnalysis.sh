#!/bin/bash

if [ $# -le 0 ] || [ $# -gt 1 ]; then
echo 'Please run with the following notation:'
echo './dataAnalysis.sh <directory with one or more results directories>'
echo '(e.g., ./dataAnalysis.sh resultsDir/ where resultsDir has several directories, such as P5_S5_fixed_0/, with reward files)'
exit 1
fi

echo ${1}/*/
for D in ${1}/*/; do
	if [ -d $D ]; then
		javac ramyaram/DataAnalysis.java && java ramyaram/DataAnalysis $D
		python plots.py $D "dataAnalysis_reward.csv"
	fi
done
