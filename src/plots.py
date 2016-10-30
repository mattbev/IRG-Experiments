'''plot reward curve'''

import sys, argparse
import matplotlib.pyplot as plt
import math
import numpy as np
from scipy import interpolate

plt.gcf().subplots_adjust(bottom=0.15)

f = file(sys.argv[1]+'/'+sys.argv[2]).read().strip().split('\n')
colors = ['b','r','g']
cnt=0
for line in f:
	if cnt == 0: #skip source task learning
		cnt+=1
		continue
	label = line.split(',')[0]
	y = line.split(',')[1:]
	x = [i for i in range(len(y))]
	x_int = np.linspace(x[0], x[-1], 100)
	tck = interpolate.splrep(x, y, k = 3, s = 1)
	y_int = interpolate.splev(x_int, tck, der = 0)
	plt.subplot(211)
	plt.plot(x, y, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
	plt.subplot(212)
	plt.plot(x_int, y_int, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
	cnt+=1

plt.subplot(211)
plt.title('Reward curve with all points')
plt.legend(loc=4)
plt.ylabel('Average Accumulated Reward')
plt.xlabel('Number of Episodes')

plt.subplot(212)
plt.title('Smoothed reward curve')
plt.legend(loc=4)
plt.ylabel('Average Accumulated Reward')
plt.xlabel('Number of Episodes')

plt.savefig(sys.argv[1]+'/plots.pdf')
