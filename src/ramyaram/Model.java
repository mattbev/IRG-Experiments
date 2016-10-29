package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;

import ontology.Types;
import tools.Vector2d;

/**
 * Model learned after a number of episodes
 * Includes learned value functions for each object class in the given game
 * And the mapping between itype sprite ids in the game to internal object class ids
 */
public class Model {
	public ArrayList<ValueFunction> qValueFunctions;
	private HashMap<Integer, Integer> itype_to_objClassId;
	private ArrayList<int[][][][][]> transitionEstimates;
	private ArrayList<int[][][][]> rewardEstimates;
	private String game;
	
	public Model(String game) {
		this.game = game;
		this.qValueFunctions = new ArrayList<ValueFunction>();
		this.itype_to_objClassId = new HashMap<Integer, Integer>();
		this.transitionEstimates = new ArrayList<int[][][][][]>();
		this.rewardEstimates = new ArrayList<int[][][][]>();
	}
	
	public Model(String game, ArrayList<ValueFunction> qValueFunctions, HashMap<Integer, Integer> itype_to_objClassId, 
			ArrayList<int[][][][][]> transitionEstimates, ArrayList<int[][][][]> rewardEstimates){
		this.game = game;
		this.qValueFunctions = qValueFunctions;
		this.itype_to_objClassId = itype_to_objClassId;
		this.transitionEstimates = transitionEstimates;
		this.rewardEstimates = rewardEstimates;
	}

	public void updateModelEstimate(int index, Vector2d agent, Vector2d obj, Types.ACTIONS action, Vector2d agentNext, Vector2d objNext, double reward){
//		numNonZeroTransReward();
		transitionEstimates.get(index)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[Agent.getXDist(agentNext, objNext)+Agent.numCols-1][Agent.getYDist(agentNext, objNext)+Agent.numRows-1]++;
		rewardEstimates.get(index)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[getRewardIndex(reward)]++;
//		numNonZeroTransReward();
	}
	
	public int getTransitionCounts(int index, Vector2d agent, Vector2d obj, Types.ACTIONS action, Vector2d agentNext, Vector2d objNext, double reward){
		return transitionEstimates.get(index)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[Agent.getXDist(agentNext, objNext)+Agent.numCols-1][Agent.getYDist(agentNext, objNext)+Agent.numRows-1];
	}
	
	public int getRewardCounts(int index, Vector2d agent, Vector2d obj, Types.ACTIONS action, Vector2d agentNext, Vector2d objNext, double reward){
		return rewardEstimates.get(index)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[getRewardIndex(reward)];
	}
	
	public void numNonZeroTransReward(){
		int transNum = 0;
		int rewardNum = 0;
		for(int i=0; i<transitionEstimates.size(); i++){
			for(int j=0; j<transitionEstimates.get(i).length; j++){
				for(int k=0; k<transitionEstimates.get(i)[j].length; k++){
					for(int l=0; l<transitionEstimates.get(i)[j][k].length; l++){
						for(int m=0; m<rewardEstimates.get(i)[j][k][l].length; m++){
							if(rewardEstimates.get(i)[j][k][l][m] > 0 || rewardEstimates.get(i)[j][k][l][m] < 0)
								rewardNum++;
						}
						for(int m=0; m<transitionEstimates.get(i)[j][k][l].length; m++){
							for(int n=0; n<transitionEstimates.get(i)[j][k][l][m].length; n++){
								if(transitionEstimates.get(i)[j][k][l][m][n] > 0 || transitionEstimates.get(i)[j][k][l][m][n] < 0)
									transNum++;
							}
						}
					}
				}
			}
		}
		System.out.println("numNonZeros -- transition: "+transNum+" reward: "+rewardNum);
	}
	
	public int getRewardIndex(double reward){
		if(reward < -0.5) //if the reward is more negative than the tiny reward given at each time step
			return 0;
		else if(reward >= -0.5 && reward <= 0.5) //if the reward is zero or a tiny reward given at each time step
			return 1;
		else //if the reward is highly positive
			return 2;
	}
	
	public void addObjToModel(){
		qValueFunctions.add(new ValueFunction(null));
		transitionEstimates.add(new int[Agent.numCols*2][Agent.numRows*2][Types.ACTIONS.values().length][Agent.numCols*2][Agent.numRows*2]);
		rewardEstimates.add(new int[Agent.numCols*2][Agent.numRows*2][Types.ACTIONS.values().length][3]);
	}
	
	public HashMap<Integer, Integer> getItype_to_objClassId(){
		return itype_to_objClassId;
	}
	
	public String getGame() {
		return game;
	}
	
	public Model clone(){
		return new Model(game, new ArrayList<ValueFunction>(qValueFunctions), new HashMap<Integer, Integer>(itype_to_objClassId), 
				new ArrayList<int[][][][][]>(transitionEstimates), new ArrayList<int[][][][]>(rewardEstimates));
	}
	
	public void clear(){
		qValueFunctions = null;
		itype_to_objClassId = null;
		transitionEstimates = null;
		rewardEstimates = null;
		game = "";
	}
}
