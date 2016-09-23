package ramyaram;

import java.util.ArrayList;
import java.util.List;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

public class OBTAgent extends OFQAgent {
	private static int[] currMapping;
	private static double[][] mappingQ; //Q-values over mappings (it specifies for each objClass in the new task, which objClass in the source task it aligns to (or a new class if nothing aligns))
	private static int numEpisodesMapping = 1;
	private static int numEpisodesQValues = 9;
	private static double mapping_alpha = 0.1; //the learning rate for the mapping phase
	private static double mapping_epsilon = 0.1; //the amount an agent explores (as opposed to exploit) mappings of different object classes
	private static double mapping_epsilon_end = 0.001; //the ending mapping exploration rate (after decreasing to this value, the parameter stays constant)
	private static double mapping_epsilon_delta = 0.01; //exploration over mappings decreases by this delta value over time
	private static int[][] numOfMappings;

	public OBTAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
		currMapping = new int[numObjClasses];
		for(int i=0; i<currMapping.length; i++)
			currMapping[i] = i;
	}
	
	public ValueFunction[] run(int conditionNum, int numEpisodes, String game, String level1, String controller, int seed, ValueFunction[] priorValueFunctions) {
		if(priorValueFunctions != null) { //if a previously learned source task exists
			qValueFunctions = new ValueFunction[priorValueFunctions.length+1];
			for(int i=0; i<qValueFunctions.length; i++){
				if(i < priorValueFunctions.length)
					qValueFunctions[i] = new ValueFunction(priorValueFunctions[i].getOptimalQValues());
				else //create a new empty value function that is learned if no previous ones align well
					qValueFunctions[i] = new ValueFunction(null);
			}			
		}
		System.out.println("in obt run");
		mappingQ = new double[numObjClasses][qValueFunctions.length];
		numOfMappings = new int[numObjClasses][qValueFunctions.length];
		
		int k=0;
		//keep value functions constant and learn mappings between objects
		mappingPhase(conditionNum, k, numEpisodesMapping, game, level1, controller, seed);
		k+=numEpisodesMapping;
		currMapping = getGreedyMapping(mappingQ);
		while(k < numEpisodes){
			//keep mappings constant and update previously learned value functions
			qValuesPhase(conditionNum, k, numEpisodesQValues, game, level1, controller, seed);
			k+=numEpisodesQValues;
		}
		int[] bestMapping = new int[mappingQ.length];
		for(int i=0; i<mappingQ.length; i++){
			double maxValue = Integer.MIN_VALUE;
			int maxIndex = -1;
			for(int j=0; j<mappingQ[i].length; j++){
				System.out.print(mappingQ[i][j]+" ");
				if(mappingQ[i][j]>maxValue){
					maxValue = mappingQ[i][j];
					maxIndex = j;
				}
			}
			System.out.println();
			bestMapping[i] = maxIndex;
			numOfMappings[i][bestMapping[i]]++;
		}
		System.out.println("-------");

		for(int i=0; i<bestMapping.length; i++)
			System.out.print(bestMapping[i]+" ");
		System.out.println();
		return qValueFunctions;
	}
	
	public void runEpisode(int conditionNum, int episodeNum, String game, String level1, String controller, int seed){
		super.runOneEpisode(conditionNum, episodeNum, game, level1, controller, seed);
        
        mapping_epsilon -= mapping_epsilon_delta;
		if(mapping_epsilon < mapping_epsilon_end)
			mapping_epsilon = mapping_epsilon_end;
	}
	
	/**
	 * Update QValues of previously learned value functions
	 * Mappings between object classes of the source and target task are kept constant
	 * Actions are chosen based on the previously learned value functions (based on mapping)
	 * and the value function used for action selection is updated
	 */
	public void qValuesPhase(int conditionNum, int iterationNum, int numEpisodes, String game, String level1, String controller, int seed){
		updateQValues = true;
		for(int k=iterationNum; k<(iterationNum+numEpisodes); k++)
			runOneEpisode(conditionNum, k, game, level1, controller, seed);
	}
	
	/**
	 * Update mappings between object classes of the source and target task
	 * QValues of the previously learned object classes in the source task are kept constant
	 */
	public void mappingPhase(int conditionNum, int iterationNum, int numEpisodes, String game, String level1, String controller, int seed){
		updateQValues = false;
		for(int k=iterationNum; k<(iterationNum+numEpisodes); k++){
			int[] mapping = getMapping(mappingQ);
			double episodeReward = runOneEpisode(conditionNum, k, game, level1, controller, seed);
			for(int i=0; i<mapping.length; i++){
				double q = mappingQ[i][mapping[i]]; //update mappingQ based on reward received
		        double qValue = (1 - mapping_alpha) * q + mapping_alpha * episodeReward;
		        mappingQ[i][mapping[i]] = qValue;
			}
		}
	}
	
	public ValueFunction getValueFunction(Object obj){
		return qValueFunctions[currMapping[obj.objectClassId]];
	}
	
	/**
	 * Uses an epsilon-greedy approach to choose an mapping from the mapping value function
	 * With probability epsilon, a random prior object class (or a new class) is chosen for each new object class
	 * With probability 1-epsilon, the previous object class (or new class) that has the highest Q-value is chosen as the objClass that aligns best with each new object class
	 */
	public int[] getMapping(double[][] similarityMatrix){
		//epsilon-greedy approach to choosing an mapping
		if(rand.nextDouble() < mapping_epsilon){
			// choose a random mapping
			int[] mapping = new int[numObjClasses];
			for(int i=0; i<mapping.length; i++)
				mapping[i] = rand.nextInt(qValueFunctions.length);
			return mapping;
		} else { // otherwise, chooses the best mapping/the one with the highest Q-value
			return getGreedyMapping(similarityMatrix);
		}
	}
	
	public int[] getGreedyMapping(double[][] similarityMatrix){
		int[] mapping = new int[numObjClasses];
		for(int i=0; i<similarityMatrix.length; i++){ // for each new object class
			List<Integer> possibleMappings = new ArrayList<Integer>();
			double maxQ = Integer.MIN_VALUE;
			for(int j=0; j<similarityMatrix[i].length; j++){
				double q = similarityMatrix[i][j];
				if(Math.abs(q - maxQ) < 0.0001){ //basically equal
					possibleMappings.add(j);
					maxQ = Math.max(q, maxQ);
				}
				else if(q > maxQ){
					maxQ = q;
					possibleMappings.clear();
					possibleMappings.add(j);
				}
			}
			mapping[i] = possibleMappings.get(rand.nextInt(possibleMappings.size()));
		}
		return mapping;
	}
}
