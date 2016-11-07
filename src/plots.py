'''plot reward curve'''

import sys, argparse
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import math
import numpy as np
from scipy import interpolate

plt.gcf().subplots_adjust(bottom=0.15)

subplot_nums = [311,312,313]
colors = ['b','r','g']
titles = ['Reward','Final Game Tick','Number of Wins']

for num in range(len(subplot_nums)):
    cnt=0
    f = file(sys.argv[1]+'/'+sys.argv[num+2]).read().strip().split('\n')
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
        plt.subplot(subplot_nums[num])
        plt.plot(x, y, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
#       plt.subplot(212)
#       plt.plot(x_int, y_int, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
        cnt+=1

for num in range(len(subplot_nums)):
    plt.subplot(subplot_nums[num])
    plt.subplots_adjust(hspace=1)
    plt.title(titles[num])
    plt.legend(loc=4, fontsize = 'x-small')
    plt.ylabel('Average Accumulated\n'+titles[num])
    plt.xlabel('Number of Episodes')

plt.savefig(sys.argv[1]+'/plots.pdf')
