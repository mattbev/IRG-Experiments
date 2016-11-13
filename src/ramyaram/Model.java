package ramyaram;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

import ontology.Types;
import tools.Vector2d;

/**
 * Model learned after a number of episodes
 * Includes learned value functions for each object class in the given game, a mapping between itype sprite ids in the game to internal object class ids,
 * an estimate of the transition function for each object class, and an estimate of the reward function for each object class
 */
public class Model {
	public ArrayList<ValueFunction> qValueFunctions;
	private HashMap<Integer, Integer> itype_to_objClassId;
	private ArrayList<int[][][][][]> transitionEstimates;
	private ArrayList<int[][][][]> rewardEstimates;
	
	public Model(){
		this(new ArrayList<ValueFunction>(), new HashMap<Integer, Integer>(), new ArrayList<int[][][][][]>(), new ArrayList<int[][][][]>());
	}
	
	public Model(ArrayList<ValueFunction> qValueFunctions, HashMap<Integer, Integer> itype_to_objClassId, 
			ArrayList<int[][][][][]> transitionEstimates, ArrayList<int[][][][]> rewardEstimates){
		this.qValueFunctions = qValueFunctions;
		this.itype_to_objClassId = itype_to_objClassId;
		this.transitionEstimates = transitionEstimates;
		this.rewardEstimates = rewardEstimates;
	}
	
	/**
	 * Add a newly seen object class into the model (new value function, new transition estimate, new reward estimate)
	 */
	public void addObjClassToModel(int objClassItype){
		qValueFunctions.add(new ValueFunction(null, objClassItype, -1)); //no previous object class
		transitionEstimates.add(new int[Agent.numCols*2][Agent.numRows*2][Types.ACTIONS.values().length][Agent.numCols*2][Agent.numRows*2]);
		rewardEstimates.add(new int[Agent.numCols*2][Agent.numRows*2][Types.ACTIONS.values().length][3]);
	}

	/**
	 * Update transition and reward function estimate for the given object class index
	 */
	public void updateModelEstimate(int objClassIndex, Vector2d agent, Vector2d obj, Types.ACTIONS action, Vector2d agentNext, Vector2d objNext, double reward){
		transitionEstimates.get(objClassIndex)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[Agent.getXDist(agentNext, objNext)+Agent.numCols-1][Agent.getYDist(agentNext, objNext)+Agent.numRows-1]++;
		rewardEstimates.get(objClassIndex)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[getRewardIndex(reward)]++;
	}
	
	/**
	 * Get the number of times a particular transition tuple <s,a,s'> has been seen for the given object class index
	 */
	public int getTransitionCounts(int objClassIndex, Vector2d agent, Vector2d obj, Types.ACTIONS action, Vector2d agentNext, Vector2d objNext, double reward){
		return transitionEstimates.get(objClassIndex)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[Agent.getXDist(agentNext, objNext)+Agent.numCols-1][Agent.getYDist(agentNext, objNext)+Agent.numRows-1];
	}
	
	/**
	 * Get the number of times a particular reward tuple <s,a,r> has been seen for the given object class index
	 * Reward is discretized into three buckets: negative, close to 0, and positive
	 */
	public int getRewardCounts(int objClassIndex, Vector2d agent, Vector2d obj, Types.ACTIONS action, Vector2d agentNext, Vector2d objNext, double reward){
		return rewardEstimates.get(objClassIndex)[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()]
				[getRewardIndex(reward)];
	}
	
	/**
	 * Discretize reward into negative, close to 0, positive
	 */
	public int getRewardIndex(double reward){
		if(reward < -0.5) //if the reward is more negative than the tiny reward given at each time step
			return 0;
		else if(reward >= -0.5 && reward <= 0.5) //if the reward is zero or a tiny reward given at each time step
			return 1;
		else //if the reward is highly positive
			return 2;
	}
	
	public void writeToFile(File dir){
		try{
			String dirPath = dir.getPath();
			File infoFile = new File(dirPath+"/modelInfo.txt");
			Main.writeToFile(infoFile, "numRows="+Agent.numRows+"\n");
			Main.writeToFile(infoFile, "numCols="+Agent.numCols+"\n");
			for(ValueFunction q : qValueFunctions){
				File qFile = new File(dirPath+"/"+q.objClassItype+".csv");
				BufferedWriter writer = new BufferedWriter(new FileWriter(qFile));
				for(int i=0; i<q.optimalQValues.length; i++){
					for(int j=0; j<q.optimalQValues[i].length; j++){
						for(int k=0; k<q.optimalQValues[i][j].length; k++){
							writer.write(q.optimalQValues[i][j][k]+",");
						}
					}
				}
				writer.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void readFile(File dir){
		try{
			dir = new File(dir.getPath()+"/learnedQ");
			System.out.println(dir.getAbsolutePath());
			for(File file : dir.listFiles()){ //for each object value function
				if(file.getPath().contains("modelInfo"))
					continue;
				int itype = Integer.parseInt(file.getPath().substring(file.getPath().lastIndexOf('/')+1, file.getPath().lastIndexOf('.')));
				itype_to_objClassId.put(itype, itype_to_objClassId.size());
				
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String[] tokens = reader.readLine().split(",");
				int count = 0;
				ValueFunction q = new ValueFunction(null, itype, -1, DataAnalysis.getVariableValueFromFile(dir.getPath()+"/modelInfo.txt", "numRows"), 
						DataAnalysis.getVariableValueFromFile(dir.getPath()+"/modelInfo.txt", "numCols"));
				for(int i=0; i<q.optimalQValues.length; i++){
					for(int j=0; j<q.optimalQValues[i].length; j++){
						for(int k=0; k<q.optimalQValues[i][j].length; k++){
							q.optimalQValues[i][j][k] = Double.parseDouble(tokens[count]);
							count++;
						}
					}
				}
				qValueFunctions.add(q);
				reader.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		for(ValueFunction q : qValueFunctions){
			System.out.println(q.getNumNonZero());
		}
	}
	
	public HashMap<Integer, Integer> getItype_to_objClassId(){
		return itype_to_objClassId;
	}
	
	public Model clone(){
		return new Model(new ArrayList<ValueFunction>(qValueFunctions), new HashMap<Integer, Integer>(itype_to_objClassId), 
				new ArrayList<int[][][][][]>(transitionEstimates), new ArrayList<int[][][][]>(rewardEstimates));
	}
	
	public void clear(){
		qValueFunctions = null;
		itype_to_objClassId = null;
		transitionEstimates = null;
		rewardEstimates = null;
	}
	
	/**
	 * Print the number of non-zero values in the transition estimate and in the reward estimate
	 */
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
		System.out.println("NumNonZeros -- transition: "+transNum+" reward: "+rewardNum);
	}
}
