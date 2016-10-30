'''plot reward curve'''

import sys, argparse
import matplotlib.pyplot as plt
import math
import numpy as np
from scipy import interpolate

plt.gcf().subplots_adjust(bottom=0.15)

f = file(sys.argv[1]+'reward.csv').read().strip().split('\n')
colors = ['b','r','g']
cnt=0
for line in f:
	if cnt == 0: #skip source task learning
		cnt+=1
		continue
	label = line.split(',')[0]
	y = line.split(',')[1:]
	x = [i for i in range(len(y))]
	# plt.plot(x, y, label = label, marker = 'o', linestyle='')
	plt.plot(x, y, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
	cnt+=1

plt.legend(loc=4)
plt.ylabel('Average Accumulated Reward')
plt.xlabel('Number of Episodes')
#plt.show()
plt.savefig(sys.argv[1]+'plots_allPts.pdf')
