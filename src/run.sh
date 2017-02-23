#!/bin/bash

if [ $# -le 0 ]; then
echo 'Please run with the following notation:'	
echo "args[1] - play or run"
echo "args[2] and on - can include any of these flags:"
echo "\"-g\": specifies game and level, usage: <game><index> (e.g., \"-g W5\" which means run game W level 5)"
echo "\"-n\": number of total episodes to run the game, usage: <number of episodes to run the game for> (e.g., \"-n 5000\" which means run game for 5000 episodes)"
echo "\"-nl\": number of episodes the agent learns before being evaluated, usage: <number of learning episodes> (e.g., \"-nl 10\" which means learn for 10 episodes, evaluate for ne episodes, then again learn for 10 episodes, evaluate until total # of episodes)"
echo "\"-ne\": number of episodes agent is evaluated (no learning takes place), usage: <number of evaluation episodes> (e.g., \"-ne 50\" which means you run 50 episodes in each evaluation period and average the reward to get the current performance of agent)"
echo "\"-c\": which conditions to run, usage: <condition1>,<condition2>... (e.g., "-c ofq,obt" which means you run only the specified conditions), options are: of-q on source (qs), of-q on target (qt), obt on target (tt), random on target (rt), random on source (rs))"
echo "\"-a\": number of runs to average over, usage: <number of runs> (e.g., \"-a 50\" which means run 50 runs and average over them)"
echo "\"-f\": file with saved model to read from, usage: <file directory> (e.g., \"-f src/F5\" which means read from directory src/F5)"
echo "\"-m\": mapping between two tasks, usage: {<target obj itype>:<source obj itype>,<target obj itype>:<source obj itype>} (e.g., \"-m {3:4,5:16}\" which means target obj class itype 3 is mapped to 4 from earlier task and similarly 5 is mapped to 16)"
echo "\"-v\": watch the agent play the game, usage: <true or false> (e.g., \"-v true\" which means visuals is turned on, default is no visuals)"
exit 1
fi

args="$@"
dir="$1" # first part of directory name is 'run' or 'play'

shift  # shift the 'run/play' argument.
while : ; do
case "$1" in
-g)
dir+="_$2"; # add game to directory name
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

javac ramyaram/Main.java
java ramyaram/Main $newdir "$args"
python plots.py $newdir "reward.csv"
