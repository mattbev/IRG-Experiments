#!/bin/bash

if [ $# -le 0 ] || [ $# -gt 7 ]; then
echo 'Please run with the following notation:'	
echo './run.sh <first game> <(optional) second game> <(optional) mapping between object itypes in the two games> <(optional) number of runs to average over> <(optional) number of total episodes per run> <(optional) interval for recording (e.g., 10 means record every 10 episodes)> <(optional) jar file>'
echo 'To play a game (e.g., aliens level 0): ./run.sh aliens0'
echo 'To run transfer between two games (e.g., aliens level0 to sheriff level 0) with a given fixed mapping (ids are itypes of objects in the game): ./run.sh aliens5 sheriff0 {9:3,5:4,1:0}'
echo 'To run transfer between two games (e.g., aliens level 0 to sheriff level 0) where the agent learns a mapping: ./run.sh aliens0 sheriff0'
exit 1
fi

dir=$1
if [ $# -ge 2 ]; then
	dir+="_$2"
fi
if [ $# -ge 3 ]; then
	dir+="_fixed"
fi
count=0
newdir="${dir}_${count}"
while [ -d "$newdir" ]; do
	count=$((count+1))
	newdir="${dir}_${count}"
done
echo "$newdir"
mkdir "$newdir"

if [ $# -ge 6 ]; then
    java -jar $7 $newdir $1 $2 $3 $4 $5 $6
else
    javac ramyaram/Main.java
    java ramyaram/Main $newdir $1 $2 $3 $4 $5 $6
fi
python plots.py $newdir "reward.csv"
