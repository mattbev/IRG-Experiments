#!/bin/bash

# This program takes 1 argument, a number between 1 and 4 that specifies the ordering of the test games.
# For a pair of participants, you can either choose 1 and 2 OR 3 and 4, not any other pair because participants can never play the same testing game at the same time.

order="$1"

declare -a arr1=("X0" "Y0" "Z0" "L0" "V0" "U0" "Q0")
declare -a arr2=("X0" "Y0" "Z0" "V0" "L0" "Q0" "U0")
declare -a arr3=("X0" "Y0" "Z0" "V0" "L0" "U0" "Q0")
declare -a arr4=("X0" "Y0" "Z0" "L0" "V0" "Q0" "U0")

arrStr="arr$order"
tmp="$arrStr[@]"
arrTmp=${!tmp}
arr=( $arrTmp )

i=0
while [ "$i" -lt "${#arr[@]}" ]; do
	while [ "$i" -gt 0 ]; do
		read -p "Are you ready to continue?" y
		case $y in
			[Yy]* ) break;;
			* ) echo "Please answer 'y' when ready.";;
		esac
	done
	echo "Playing game $i"
	./run.sh play -g "${arr[i]}"
	i=$((i+1))
done