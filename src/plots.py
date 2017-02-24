'''plot reward curve'''

import sys, argparse
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import math
import numpy as np
from scipy import interpolate

colors = ['b','r','g']
cnt=0
f = file(sys.argv[1]+'/'+sys.argv[2]).read().strip().split('\n')
for line in f:
    label = line.split(',')[0]
    y = line.split(',')[1:]
    y = [e for e in y if e] #use non-null elements
    x = [i for i in range(len(y))]
#    x_int = np.linspace(x[0], x[-1], 100)
#    tck = interpolate.splrep(x, y, k = 3, s = 1)
#    y_int = interpolate.splev(x_int, tck, der = 0)
    plt.figure(0)
    plt.plot(x, y, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
#    plt.subplot(subplot_nums[1])
#    plt.plot(x_int, y_int, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
    cnt+=1

plt.figure(0)
plt.title('Learning Curve')
plt.legend(loc=4, fontsize = 'x-small')
plt.ylabel('Average Accumulated\nReward')
plt.xlabel('Number of Episodes')

plt.figure(0)
plt.savefig(sys.argv[1]+'/plot.pdf')
