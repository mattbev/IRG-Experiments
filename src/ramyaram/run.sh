#!/bin/bash

if [[ $# -eq 0 ]] ; then
echo 'Usage: ./run.sh <csv filename with results> <filename to save plot>'
echo 'Example: ./run.sh reward.csv plot.pdf'
exit 1
fi

#java Main $1
#path /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk;%path%

javac Main.java
echo -e "Main-Class: Main\n" > manifest.txt
jar cvfe gvgai-obt.jar Main *.class
#jar cfm gvgai-obt.jar manifest.txt Main.class
#jar cvf gvgai-obt.jar *
java -jar gvgai-obt.jar $1

#python plots.py ../../$1 $2
#open $2
