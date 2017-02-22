package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import core.game.Observation;
import core.game.StateObservation;
import tools.ElapsedCpuTimer;

/**
 * Object-Based Transfer agent
 * Learns a mapping between object classes in a source task and object classes in a target task to speed up learning
 */
public class OBTAgent extends OFQAgent {
	private static Model priorLearnedModel; //prior learned model from the source task
	private static ArrayList<Integer> currMapping; //current mapping between object classes in the source and target tasks
	private static ArrayList<int[]> numOfMappings; //keeps track of how many times each mapping is chosen as the best mapping over many runs
	
	//mapping similarity (it specifies for each object class in the new task, how similar each object class in the source task is; it also includes a similarity measure to an empty value function)
	private static ArrayList<double[]> weightedSim; //weighted similarity measure between mappings (combines multiple similarity measures, such as performance, transition, and reward)
	private static ArrayList<double[]> performanceSim; //similarity measure based on performance (try the mapped value function and use obtained reward as performance measure)
	private static ArrayList<double[]> objTransitionSim; //similarity measure based on transition function similarity
	private static ArrayList<double[]> objRewardSim; //similarity measure based on reward function similarity
	
	private static double[] weights; //weights for each similarity measure (indices below - performance, transition, reward)
	private static final int PERFORMANCE=0, TRANSITION=1, REWARD=2;
	private static final int numWeights = 3;

	public OBTAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	/**
	 * Runs the Object-Based Transfer algorithm with the given parameters
	 */
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, Model priorLearnedModel) {
		if(Constants.numEpisodesMapping > Constants.numTargetEpisodes){ //sanity check - cannot have more episodes in the mapping phase than total episodes in the full run
			System.out.println("Error: number of total episodes is less than the sum of episodes of each phase.");
			System.exit(0);
		}
		model = new Model();
		OBTAgent.gameName = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		
		if(Constants.readModelFromFile){
			try{
				OBTAgent.priorLearnedModel = new Model();
				OBTAgent.priorLearnedModel.readFile(Main.readModelFile);
			} catch(Exception e){
				e.printStackTrace();
			}
		} else if(priorLearnedModel != null){
			OBTAgent.priorLearnedModel = priorLearnedModel;
		}

		currMapping = new ArrayList<Integer>();
		numOfMappings = new ArrayList<int[]>();	
		weightedSim = new ArrayList<double[]>();
		performanceSim = new ArrayList<double[]>();
		objTransitionSim = new ArrayList<double[]>();
		objRewardSim = new ArrayList<double[]>();
		
		System.out.println("CURR MAPPING "+currMapping);
		
		weights = new double[numWeights];
		weights[PERFORMANCE] = 1;
//		for(int i=0; i<weights.length; i++)
//			weights[i] = 1/(double)weights.length;

		int k=0;
		//learn mappings between objects without making any changes to the value functions
		mappingPhase(conditionNum, k, Constants.numEpisodesMapping, game, level1, visuals, controller);
		k+=Constants.numEpisodesMapping;
		//copy value functions from previously learned task for new objects based on current mapping
		copyMappedValueFunctions();
		while(k < numEpisodes){
//			weightedSim = newWeightedSim();
//			currMapping = getGreedyMapping(weightedSim);
			//update Q-values in newly copied value functions
			qValuesPhase(conditionNum, k, 1, game, level1, visuals, controller);
			k+=1;
		}
		ArrayList<Integer> bestMapping = getMaxMapping(performanceSim);
		//update number of times this mapping has been chosen at the end of a run
		for(int i=0; i<bestMapping.size(); i++)
			numOfMappings.get(i)[bestMapping.get(i)]++;
		System.out.println("-------");
		System.out.println("Best Mapping based on mappingQ:");
		printItypeMapping(bestMapping);
		System.out.println("Current Mapping:");
		printItypeMapping(currMapping);
		return model;
	}
	
	/**
	 * Update mappings between object classes of the source and target task
	 * Q-values of the previously learned object classes in the source task do not change
	 */
	public void mappingPhase(int conditionNum, int iterationNum, int numEpisodes, String game, String level1, boolean visuals, String controller){
		updateQValues = false;
		for(int k=iterationNum; k<(iterationNum+numEpisodes); k++){
			weightedSim = calculateWeightedSim();
			currMapping = getMapping(weightedSim);
			double episodeReward = runOneEpisode(conditionNum, k, game, level1, visuals, controller);
			for(int i=0; i<currMapping.size(); i++){
				double q = performanceSim.get(i)[currMapping.get(i)]; //update mappingQ based on reward received
		        double qValue = (1 - Constants.mapping_alpha) * q + Constants.mapping_alpha * episodeReward;
		        performanceSim.get(i)[currMapping.get(i)] = qValue;
			}
			Constants.mapping_epsilon -= Constants.mapping_epsilon_delta;
			if(Constants.mapping_epsilon < Constants.mapping_epsilon_end)
				Constants.mapping_epsilon = Constants.mapping_epsilon_end;
		}
	}
	
	/**
	 * Based on the current mapping, copy the most similar value functions to use as a prior for the Q-values phase
	 */
	public void copyMappedValueFunctions(){
		for(int i=0; i<currMapping.size(); i++){
			int objClassItype = getItypeFromObjClassId(i, model.getItype_to_objClassId());
			if(currMapping.get(i) >= priorLearnedModel.qValueFunctions.size())
				model.qValueFunctions.set(i, new ValueFunction(null, objClassItype, -1)); //no previous object class
			else{
				int previousObjClassItype = getItypeFromObjClassId(currMapping.get(i), priorLearnedModel.getItype_to_objClassId());
				model.qValueFunctions.set(i, new ValueFunction(priorLearnedModel.qValueFunctions.get(currMapping.get(i)).getOptimalQValues(), objClassItype, previousObjClassItype));
			}
		}
	}
	
	/**
	 * Update Q-values of new value functions
	 */
	public void qValuesPhase(int conditionNum, int iterationNum, int numEpisodes, String game, String level1, boolean visuals, String controller){
		updateQValues = true;
		for(int k=iterationNum; k<(iterationNum+numEpisodes); k++)
			runOneEpisode(conditionNum, k, game, level1, visuals, controller);
	}
	
	/**
	 * If a new object class is seen, add it to the model
	 */
	public void processObs(Observation obs, Map<Observation, Object> map){
    	super.processObs(obs, map);
		if(model.getItype_to_objClassId().get(obs.itype) >= currMapping.size()){
			if(Constants.fixedMapping != null){
				if(Constants.fixedMapping.containsKey(obs.itype))
					currMapping.add(priorLearnedModel.getItype_to_objClassId().get(Constants.fixedMapping.get(obs.itype)));
				else
					currMapping.add(priorLearnedModel.qValueFunctions.size());
			} else {
				currMapping.add(rand.nextInt(priorLearnedModel.qValueFunctions.size()+1));
			}
			weightedSim.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			performanceSim.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			objTransitionSim.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			objRewardSim.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			numOfMappings.add(new int[priorLearnedModel.qValueFunctions.size()+1]);
		}
    }
	
	/**
	 * Gets the value function for the given object 
	 * It can be either a previously learned value function (during the mapping phase) or the new value function for that object class (during the Q-values phase)
	 */
	public ValueFunction getValueFunction(Object obj){
		if(updateQValues) //in qvalues phase and value functions are already copied into model.qValueFunctions
			return model.qValueFunctions.get(obj.getObjClassId());
		else{ //in mapping phase
//			System.out.println(currMapping);
//			System.out.println(priorLearnedModel);
//			System.out.println(priorLearnedModel.qValueFunctions);
			if(currMapping.get(obj.getObjClassId()) >= priorLearnedModel.qValueFunctions.size())
				return new ValueFunction(null, obj.getItype(), -1);
			else
				return priorLearnedModel.qValueFunctions.get(currMapping.get(obj.getObjClassId()));
		}
	}
	
	/**
	 * Uses an epsilon-greedy approach to choose a mapping from some similarity matrix
	 * With probability epsilon, a random prior object class (or a new class) is chosen for each new object class
	 * With probability 1-epsilon, the previous object class (or a new class) that has the highest Q-value is chosen as the mapping  
	 */
	public ArrayList<Integer> getMapping(ArrayList<double[]> similarityMatrix){
		if(Constants.fixedMapping != null){ //use fixed mapping
			ArrayList<Integer> mapping = new ArrayList<Integer>();
			for(int i=0; i<currMapping.size(); i++)
				mapping.add(-1);
			for(int new_itype : model.getItype_to_objClassId().keySet()){
				if(Constants.fixedMapping.containsKey(new_itype)) //choose the mapped object class from the given fixed mapping, if it exists
					mapping.set(model.getItype_to_objClassId().get(new_itype), priorLearnedModel.getItype_to_objClassId().get(Constants.fixedMapping.get(new_itype)));
				else //otherwise map to a new object class (does not map to any previously seen object class)
					mapping.set(model.getItype_to_objClassId().get(new_itype), priorLearnedModel.qValueFunctions.size());
			}
			return mapping;
		} else {
			//epsilon-greedy approach to choosing an mapping
			if(rand.nextDouble() < Constants.mapping_epsilon){
				//choose a random mapping
				ArrayList<Integer> mapping = new ArrayList<Integer>();
				for(int i=0; i<currMapping.size(); i++)
					mapping.add(rand.nextInt(similarityMatrix.get(i).length));
				return mapping;
			} else { //otherwise, chooses the best mapping/the one with the highest Q-value
				return getGreedyMapping(similarityMatrix);
			}
		}
	}
	
	/**
	 * Selects a greedy mapping based on the given similarity matrix
	 * (e.g., chooses the prior object class with the highest similarity measure to each new object class)
	 */
	public ArrayList<Integer> getGreedyMapping(ArrayList<double[]> similarityMatrix){
		ArrayList<Integer> mapping = new ArrayList<Integer>();
		for(int i=0; i<currMapping.size(); i++){ //for each new object class
			ArrayList<Integer> possibleMappings = new ArrayList<Integer>();
			double maxQ = Integer.MIN_VALUE;
			for(int j=0; j<similarityMatrix.get(i).length; j++){
				double q = similarityMatrix.get(i)[j];
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
			mapping.add(possibleMappings.get(rand.nextInt(possibleMappings.size())));
		}
		return mapping;
	}
	
	/**
	 * Calculates a weighted similarity measure based on multiple factors, such as performance, transition similarity, and reward similarity
	 */
	public ArrayList<double[]> calculateWeightedSim(){
		ArrayList<double[]> sim = new ArrayList<double[]>();
		for(int i=0; i<weightedSim.size(); i++){
			sim.add(new double[weightedSim.get(i).length]);
			for(int j=0; j<weightedSim.get(i).length; j++){
				for(int k=0; k<weights.length; k++){
					sim.get(i)[j] += weights[k]*getSimMatrix(k).get(i)[j];
				}
			}
		}
		return sim;
	}
	
	/**
	 * Returns the similarity matrix associated with each measure name
	 */
	public ArrayList<double[]> getSimMatrix(int index){
		switch(index){
			case PERFORMANCE:
				return performanceSim;
			case TRANSITION:
				return objTransitionSim;
			case REWARD:
				return objRewardSim;
		}
		return null;
	}
	
	/**
	 * Get the mapping that has maximum value given some similarity matrix
	 */
	public ArrayList<Integer> getMaxMapping(ArrayList<double[]> similarityMatrix){
		ArrayList<Integer> maxMapping = new ArrayList<Integer>();
		for(int i=0; i<similarityMatrix.size(); i++){
			double maxValue = Integer.MIN_VALUE;
			int maxIndex = -1;
			for(int j=0; j<similarityMatrix.get(i).length; j++){
				if(similarityMatrix.get(i)[j]>maxValue){
					maxValue = similarityMatrix.get(i)[j];
					maxIndex = j;
				}
			}
			maxMapping.add(maxIndex);
		}
		return maxMapping;
	}
	
	/**
	 * Print mapping from target task to source task using itype ids (not internal object class ids which can vary across runs)
	 */
	public void printItypeMapping(ArrayList<Integer> mapping){
		System.out.println("MAPPING");
		for(int new_itype : model.getItype_to_objClassId().keySet()){
			int newObjClassId = model.getItype_to_objClassId().get(new_itype);
			int previousObjClassId = mapping.get(newObjClassId);
			int previousItype = getItypeFromObjClassId(previousObjClassId, priorLearnedModel.getItype_to_objClassId());
			System.out.println(new_itype+" --> "+previousItype);
		}
	}
	
	/**
	 * Gets the itype id for an object class given its internal object class id
	 */
	public int getItypeFromObjClassId(int objClassId, HashMap<Integer, Integer> itype_to_objClassId){
		for(Entry<Integer, Integer> entry : itype_to_objClassId.entrySet()){
			if(entry.getValue() == objClassId)
				return entry.getKey();
		}
		return -1;
	}
	
	public void clearEachRun(){
		super.clearEachRun();
		priorLearnedModel = null;
		currMapping = null;
		weightedSim = null;
		performanceSim = null;
		objTransitionSim = null;
		objRewardSim = null;
		weights = null;
	}
	
	public void printList(ArrayList<double[]> list){
		for(int i=0; i<list.size(); i++){
			for(int j=0; j<list.get(i).length; j++)
				System.out.print(list.get(i)[j]+" ");
			System.out.println();
		}
	}
	
	public void printMatrix(double[][] matrix){
		for(int i=0; i<matrix.length; i++){
			for(int j=0; j<matrix[i].length; j++)
				System.out.print(matrix[i][j]+" ");
			System.out.println();
		}
	}
}
