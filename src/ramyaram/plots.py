'''plot reward curve'''

import sys, argparse
import matplotlib.pyplot as plt
import math

plt.gcf().subplots_adjust(bottom=0.15)

f = file(sys.argv[1]).read().strip().split('\n')
colors = ['r','b','g']
cnt=0
for line in f:
    vals = line.split(',')[0:]
    x = [i for i in range(len(vals))]
    plt.plot(x, vals, color=colors[cnt])
    cnt+=1

plt.legend(loc=4)
plt.ylabel('Average Accumulated Reward')
plt.xlabel('Number of Episodes')
#plt.show()
plt.savefig(sys.argv[2])
