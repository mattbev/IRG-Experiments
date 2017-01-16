import sys, collections, re
import pdb
import matplotlib.pyplot as plt
import numpy as np
import scipy.stats.mstats
from scipy import stats

num_participants = 12
num_likert_ques = 6 # 6 likert-scale questions to analyze
advice_index = 2 # Index in transfer.tsv that has the advive condition information
game_index = 3 # Index in transfer.tsv with game information
likert1_indices = [21,22,23] # Indices for the first three likert questions
is_advice_index = 24 # Index indicating whether advice was given in this condition
likert2_indices = [25,26,27] # Indices for the last three likert questions (only analyze if advice was given)

likert_answers = {'A':[],'B':[],'C':[]}
for i in likert_answers:
	for j in range(num_likert_ques): 
		likert_answers[i].append([])

f = file("data/transfer.tsv").read().split("\n")
count = 0
for line in f:
	if count == 0: # Skip the first line -- just has labels
		count += 1
		continue
	tokens = line.split("\t")
	for i in range(len(likert1_indices)):
		likert_answers[tokens[advice_index]][i].append(int(tokens[likert1_indices[i]]))
	if tokens[is_advice_index] == 'Yes':
		for j in range(len(likert2_indices)):
			likert_answers[tokens[advice_index]][j+3].append(int(tokens[likert2_indices[j]]))

for advice_condition in likert_answers:
	print advice_condition
	for i in likert_answers[advice_condition]:
		print i
		if len(i) > 0: # Testing for normal distribution
			z,pval = scipy.stats.mstats.normaltest(i)
			print z, pval
			if pval < 0.055:
				print "Not normal distribution"
			else:
				print "Normal distribution"
	
# Run t-test with given data
def run_ttest(condition1, condition2, num_ques):
	for i in range(num_ques): 
		stat,pval = stats.ttest_ind(likert_answers[condition1][i], likert_answers[condition2][i])
		print stat, pval

# Comparing each advice condition pair
print "Good vs Bad Advice"
run_ttest('A', 'B', num_likert_ques)
print "Good vs No Advice"
run_ttest('B', 'C', 3)
print "Bad vs No Advice"
run_ttest('A', 'C', 3)