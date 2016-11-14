#!/bin/bash

if [ $# -le 0 ]; then
echo 'Please run with the following notation:'	
echo "args[1] - play or run"
echo "args[2] and on - can include any of these flags:"
echo "\"-s\": source task flag, usage: <game><index>-<number of episodes to run it for> (e.g., \"-s W5-5000\" which means run source task: game W level 5 for 5000 episodes)"
echo "\"-t\": target task flag, usage: <game><index>-<number of episodes to run it for> (e.g., \"-t W5-5000\" which means run target task: game W level 5 for 5000 episodes)"
echo "\"-m\": mapping between two tasks, usage: {<target obj itype>:<source obj itype>,<target obj itype>:<source obj itype>} (e.g., \"-m {3:4,5:16}\" which means target obj class itype 3 is mapped to 4 from earlier task and similarly 5 is mapped to 16)"
echo "\"-a\": number of runs to average over flag, usage: <number of runs> (e.g., \"-a 50\" which means run 50 runs and average over them)"
echo "\"-i\": interval for recording, usage: <interval> (e.g., \"-i 10\" which means record reward every 10 episodes)"
echo "\"-f\": file with saved model to read from, usage: <interval> (e.g., \"-f src/F5\" which means read from directory src/F5)"
echo "\"-v\": watch the agent play the game a certain number of times, usage: <number of times you see agent play the game> (e.g., \"-v 5\" which means you see the agent play 5 times before continuing learning)"
exit 1
fi

args="$@"
dir="$1" # first part of directory name is 'run' or 'play'

shift  # shift the 'run/play' argument.
while : ; do
case "$1" in
-s)
dir+="_s_$2"; # add source game, if exists, to directory name
shift 2 ;;
-t)
dir+="_t_$2"; # add target game, if exists, to directory name
shift 2 ;;
*)
break ;;
esac
done

count=0
newdir="${dir}_${count}"
while [ -d "$newdir" ]; do
	count=$((count+1))
	newdir="${dir}_${count}"
done
echo "$newdir"
mkdir "$newdir"

#if [ "$jar" != "" ]; then
#    java -jar "$jar" $newdir "$args"
#else
javac ramyaram/Main.java
java -cp ../bin/ ramyaram/Main $newdir "$args"
#fi
python plots.py $newdir "reward.csv"
