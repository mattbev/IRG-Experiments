import sys, collections, re
import pdb

print sys.argv
from scipy import stats as st

stats = collections.defaultdict(lambda:collections.defaultdict(lambda:{}))
gameOrder = {1: [1,2,3], 2: [3,1,2], 3:[2,3,1], 
			 4: [1,2,3], 5: [3,1,2], 6:[2,3,1],
			 7: [3,2,1], 8: [2,1,3], 9:[1,3,2],
			 10: [3,2,1], 11: [2,1,3], 12:[1,3,2]}
adviceOrder = {1: ['A','B','C'], 2: ['B','C','A'], 3:['C','A','B'], 
			 4: ['A','B','C'], 5: ['B','C','A'], 6:['C','A','B'],
			 7: ['C','B','A'], 8: ['A','C','B'], 9:['B','A','C'],
			 10: ['C','B','A'], 11: ['A','C','B'], 12:['B','A','C']}
gameIndex2Name = {1:'A0', 2:'E0',3:'W0'}

"""
Reading from files

SAMPLE usage:
stats[1].keys()
['H0', 'A0', 'K0', 'M0', 'W0', 'E0']

stats[1]['H0']
{'mean': 18.666666666666668, 'max': 40.0, 'min': 3.0, 'all_scores': [3.0, 23.0, 38.0, 40.0, 4.0, 4.0]}
"""

# 12 participants
for participant_num in range(1, 13):
  f = file("data/"+str(participant_num)+".txt").read().split("\n")
  game = ""
  scores = []
  for line in f:
    if "./run.sh play" in line:
      # Get game name
      game = line.strip().split()[-1]
    elif "Traceback" in line:
      # Store game stats 
      stats[participant_num][game]["all_scores"] = scores
      stats[participant_num][game]["first"] = scores[0]
      stats[participant_num][game]["max"] = max(scores)
      stats[participant_num][game]["min"] = min(scores)
      stats[participant_num][game]["mean"] = sum(scores)/len(scores)

      game = ""
      scores = []
    elif "Result" in line:
      score = line.strip().split()[-2].split(":")[-1][:-1]
      scores.append(float(score))

# Stored in following format: stats["player num"]["game name"]["all_scores/first/max/min/mean"]

"""Analyzing stats per game and advice condition"""

# Runs a t-test using the given data
def run_ttest(condition1_data, condition2_data):
	stat,pval = st.ttest_ind(condition1_data, condition2_data)
	print stat, pval

# Comparing reward within each game across advice conditions (only 4 data points per condition)
for analysis_type in ["first", "max", "min", "mean"]: # Looping through all possible analyses of the data: "first", "max", "min", "mean"
	print "Analysis for: ", analysis_type
	perGameStats = {1:{'A':[], 'B':[], 'C':[]}, 
					2:{'A':[], 'B':[], 'C':[]}, 
					3:{'A':[], 'B':[], 'C':[]}}

	for participantNum in adviceOrder:
		for i in range(len(adviceOrder[participantNum])):
			gameNum = gameOrder[participantNum][i]
			game = gameIndex2Name[gameNum]
			advice = adviceOrder[participantNum][i]
			data = stats[participantNum][game]
			perGameStats[gameNum][advice].append(data[analysis_type])

	for game in perGameStats:
		print game
		for advice in perGameStats[game]:
			print advice
			print perGameStats[game][advice]
			print sum(perGameStats[game][advice])/len(perGameStats[game][advice])

	# Within each game, comparing each advice condition pair (i.e., good vs. bad, good vs. no, and bad vs. no)
	for game in perGameStats:
		print game
		print "Good vs Bad Advice"
		run_ttest(perGameStats[game]['A'], perGameStats[game]['B'])
		print "Good vs No Advice"
		run_ttest(perGameStats[game]['B'], perGameStats[game]['C'])
		print "Bad vs No Advice"
		run_ttest(perGameStats[game]['A'], perGameStats[game]['C'])
