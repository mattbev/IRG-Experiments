'''plot reward curve'''

import sys, argparse
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import math
import numpy as np
from scipy import interpolate

plt.gcf().subplots_adjust(bottom=0.15)

num_figs = 2
subplot_nums = [111] #[211,212]
colors = ['b','r','g']
titles = ['All Reward','Avg Reward']

cnt=0
f = file(sys.argv[1]+'/'+sys.argv[2]).read().strip().split('\n')
for line in f:
    label = line.split(',')[0]
    y = line.split(',')[1:]
    x = [i for i in range(len(y))]
#    x_int = np.linspace(x[0], x[-1], 100)
#    tck = interpolate.splrep(x, y, k = 3, s = 1)
#    y_int = interpolate.splev(x_int, tck, der = 0)
    if 'SOURCE' in label: #source task learning
        plt.figure(0)
    else: #target task learning
        plt.figure(1)
    plt.subplot(subplot_nums[0])
    plt.plot(x, y, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
#    plt.subplot(subplot_nums[1])
#    plt.plot(x_int, y_int, label = label, linestyle = '-', linewidth = 0.75, color=colors[cnt])
    cnt+=1

for fig in range(0,num_figs):
    for num in range(len(subplot_nums)):
        plt.figure(fig)
        plt.subplot(subplot_nums[num])
        plt.subplots_adjust(hspace=1)
        plt.title(titles[num])
        plt.legend(loc=4, fontsize = 'x-small')
        plt.ylabel('Average Accumulated\n'+titles[num])
        plt.xlabel('Number of Episodes')

plt.figure(0)
plt.savefig(sys.argv[1]+'/source_task.pdf')
plt.figure(1)
plt.savefig(sys.argv[1]+'/target_task.pdf')
