package ramyaram;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Object-Based Transfer agent
 * Learns a mapping between objects in the target and source tasks to speed up learning
 */
public class OBTAgent extends OFQAgent {
	private static Model priorLearnedModel;
	private static ArrayList<Integer> currMapping;
	private static ArrayList<int[]> numOfMappings;	
	private static ArrayList<double[]> weightedSim;
	private static ArrayList<double[]> mappingQ; //Q-values over mappings (it specifies for each objClass in the new task, which objClass in the source task it aligns to (or a new class if nothing aligns))
	private static ArrayList<double[]> objTransitionSim;
	private static ArrayList<double[]> objRewardSim;
	
	private static double[] weights; //weights for each similarity metric (metric indices below - performance, transition, reward)
	private static final int PERFORMANCE=0, TRANSITION=1, REWARD=2;
	private static final int numWeights = 3;

	public OBTAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, Model priorLearnedModel) {
		model = new Model(game);
		if(Main.numEpisodesMapping > Main.numEpisodes){
			System.out.println("Error: number of total episodes is less than the sum of episodes of each phase.");
			System.exit(0);
		}
		OBTAgent.game = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		OBTAgent.priorLearnedModel = priorLearnedModel;
		
		currMapping = new ArrayList<Integer>();
		numOfMappings = new ArrayList<int[]>();	
		weightedSim = new ArrayList<double[]>();
		mappingQ = new ArrayList<double[]>();
		objTransitionSim = new ArrayList<double[]>();
		objRewardSim = new ArrayList<double[]>();
		
		weights = new double[numWeights];
		weights[PERFORMANCE] = 1;
//		for(int i=0; i<weights.length; i++)
//			weights[i] = 1/(double)weights.length;
		
		int k=0;
		//Learn mappings between objects without making any changes to the value functions
		mappingPhase(conditionNum, k, Main.numEpisodesMapping, game, level1, visuals, controller, seed);
		k+=Main.numEpisodesMapping;
//		printItypeMapping(currMapping);
//		System.out.println(currMapping);
//		for(int i=0; i<model.qValueFunctions.size(); i++)
//			System.out.println(model.qValueFunctions.get(i)+" "+model.qValueFunctions.get(i).getNumNonZero());
		//copy value functions from previously learned task for new objects based on current mapping
		copyMappedValueFunctions();
//		for(int i=0; i<model.qValueFunctions.size(); i++)
//			System.out.println(model.qValueFunctions.get(i)+" "+model.qValueFunctions.get(i).getNumNonZero());
		while(k < numEpisodes){
//			weightedSim = newWeightedSim();
//			currMapping = getGreedyMapping(weightedSim);
			//Update value functions based on the learned mapping
			qValuesPhase(conditionNum, k, 1, game, level1, visuals, controller, seed);
			k+=1;
		}
		ArrayList<Integer> bestMapping = new ArrayList<Integer>();
		for(int i=0; i<mappingQ.size(); i++){
			double maxValue = Integer.MIN_VALUE;
			int maxIndex = -1;
			for(int j=0; j<mappingQ.get(i).length; j++){
				System.out.print(mappingQ.get(i)[j]+" ");
				if(mappingQ.get(i)[j]>maxValue){
					maxValue = mappingQ.get(i)[j];
					maxIndex = j;
				}
			}
			System.out.println();
			bestMapping.add(maxIndex);
			numOfMappings.get(i)[bestMapping.get(i)]++;
		}
		System.out.println("-------");
		System.out.println("Best Mapping based on mappingQ:");
		printItypeMapping(bestMapping);
		System.out.println("Current Mapping:");
		printItypeMapping(currMapping);
		return model;
	}
	
	public void copyMappedValueFunctions(){
//		System.out.println(currMapping.size());
		for(int i=0; i<currMapping.size(); i++){
			if(currMapping.get(i) >= priorLearnedModel.qValueFunctions.size())
				model.qValueFunctions.set(i, new ValueFunction(null));
			else
				model.qValueFunctions.set(i, new ValueFunction(priorLearnedModel.qValueFunctions.get(currMapping.get(i)).getOptimalQValues()));
		}
//		System.out.println(model.qValueFunctions.size());
	}
	
	public ArrayList<double[]> newWeightedSim(){
//		System.out.println("mappingQ");
//		printMatrix(mappingQ);
//		System.out.println("objTransitionSim");
//		printMatrix(objTransitionSim);
//		System.out.println("objRewardSim");
//		printMatrix(objRewardSim);
		ArrayList<double[]> sim = new ArrayList<double[]>();
		for(int i=0; i<weightedSim.size(); i++){
			sim.add(new double[weightedSim.get(i).length]);
			for(int j=0; j<weightedSim.get(i).length; j++){
				for(int k=0; k<weights.length; k++){
					sim.get(i)[j] += weights[k]*getSimMatrix(k).get(i)[j];
				}
			}
		}
//		System.out.println("newWeightedSim");
//		printMatrix(sim);
		return sim;
	}
	
	public ArrayList<double[]> getSimMatrix(int index){
		switch(index){
			case PERFORMANCE:
				return mappingQ;
			case TRANSITION:
				return objTransitionSim;
			case REWARD:
				return objRewardSim;
		}
		return null;
	}
	
	public void updateModelSimilarity(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward){
		//update model similarity of object classes
		double [][] tempTransSim = new double[weightedSim.size()][weightedSim.get(0).length];
		double [][] tempRewardSim = new double[weightedSim.size()][weightedSim.get(0).length];
		for(Observation obs : objectMap.keySet()){
			Object obj = objectMap.get(obs);
			Object nextObj = objectNextStateMap.get(obs);
			if(nextObj != null){
				for(int j=0; j<priorLearnedModel.qValueFunctions.size(); j++){
//					System.out.println(nextStateObs+" "+nextObj);
//					System.out.println("priorModel: ");
//					priorLearnedModel.numNonZeroTransReward();
//					System.out.println("newModel: ");
//					model.numNonZeroTransReward();
					int newTransitionCounts = model.getTransitionCounts(obj.getObjClassId(), stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action,
		    				nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), reward);
					int previousTransitionCounts = priorLearnedModel.getTransitionCounts(j, stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action,
		    				nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), reward);
//					System.out.println("transition "+newTransitionCounts+" "+previousTransitionCounts);
					if(newTransitionCounts>0 && previousTransitionCounts>0){
						tempTransSim[obj.getObjClassId()][j]++;
					}
					int newRewardCounts = model.getRewardCounts(obj.getObjClassId(), stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action,
		    				nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), reward);
					int previousRewardCounts = priorLearnedModel.getRewardCounts(j, stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action,
		    				nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), reward);
//					System.out.println("reward "+newRewardCounts+" "+previousRewardCounts);
					if(newRewardCounts>0 && previousRewardCounts>0){
						tempRewardSim[obj.getObjClassId()][j]++;
					}
				}
			}
		}
		normalize(tempTransSim);
		normalize(tempRewardSim);
		for(int i=0; i<tempTransSim.length; i++){
			for(int j=0; j<tempTransSim[i].length; j++){
				objTransitionSim.get(i)[j] = (objTransitionSim.get(i)[j]+tempTransSim[i][j])/2;
				objRewardSim.get(i)[j] = (objRewardSim.get(i)[j]+tempRewardSim[i][j])/2;
			}
		}
	}
	
    public void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions) {
    	super.updateEachStep(stateObs, action, nextStateObs, reward, actions);
    	for(Observation obs : objectMap.keySet()){
			Object obj = objectMap.get(obs);
			Object nextObj = objectNextStateMap.get(obs);
			if(nextObj != null){
				model.updateModelEstimate(obj.getObjClassId(), stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action,
    				nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), reward);
				updateModelSimilarity(stateObs, action, nextStateObs, reward);
			}
    	}
    }
	
	public double[][] normalize(double[][] matrix){
		for(int i=0; i<matrix.length; i++){
			double sum = 0;
			for(int j=0; j<matrix[i].length; j++)
				sum += matrix[i][j];
			for(int j=0; j<matrix[i].length; j++){
				if(sum > 0)
					matrix[i][j] /= sum;
			}
		}
		return matrix;
	}
	
	public void printItypeMapping(ArrayList<Integer> mapping){
		System.out.println("MAPPING");
		for(int new_itype : model.getItype_to_objClassId().keySet()){
			int newObjClassId = model.getItype_to_objClassId().get(new_itype);
			int oldObjClassId = mapping.get(newObjClassId);
			int oldIType = -1;
			for(Entry<Integer, Integer> entry : priorLearnedModel.getItype_to_objClassId().entrySet()){
				if(entry.getValue() == oldObjClassId)
					oldIType = entry.getKey();
			}
			System.out.println(new_itype+" --> "+oldIType);
		}
	}
	
	public void processObs(Observation obs, Map<Observation, Object> map){
    	super.processObs(obs, map);
		if(model.getItype_to_objClassId().get(obs.itype) >= currMapping.size()){
			if(Main.fixedMapping != null){
				int oldIType = Main.fixedMapping.containsKey(obs.itype)? Main.fixedMapping.get(obs.itype) : -1;
				if(oldIType >= 0)
					currMapping.add(priorLearnedModel.getItype_to_objClassId().get(oldIType));//rand.nextInt(model.qValueFunctions.size()));
				else
					currMapping.add(priorLearnedModel.qValueFunctions.size());
			} else {
				currMapping.add(rand.nextInt(priorLearnedModel.qValueFunctions.size()+1));
			}
			weightedSim.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			mappingQ.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			objTransitionSim.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			objRewardSim.add(new double[priorLearnedModel.qValueFunctions.size()+1]);
			numOfMappings.add(new int[priorLearnedModel.qValueFunctions.size()+1]);
		}
    }
	
	public void runEpisode(int conditionNum, int episodeNum, String game, String level1, boolean visuals, String controller, int seed){
//		printItypeMapping(currMapping);
		super.runOneEpisode(conditionNum, episodeNum, game, level1, visuals, controller, seed);
        
        Main.mapping_epsilon -= Main.mapping_epsilon_delta;
		if(Main.mapping_epsilon < Main.mapping_epsilon_end)
			Main.mapping_epsilon = Main.mapping_epsilon_end;
	}
	
	/**
	 * Update QValues of previously learned value functions
	 * Mappings between object classes of the source and target task are kept constant
	 * Actions are chosen based on the previously learned value functions (based on mapping)
	 * and the value function used for action selection is updated
	 */
	public void qValuesPhase(int conditionNum, int iterationNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed){
		updateQValues = true;
		for(int k=iterationNum; k<(iterationNum+numEpisodes); k++)
			runOneEpisode(conditionNum, k, game, level1, visuals, controller, seed);
	}
	
	/**
	 * Update mappings between object classes of the source and target task
	 * QValues of the previously learned object classes in the source task are kept constant
	 */
	public void mappingPhase(int conditionNum, int iterationNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed){
		updateQValues = false;
		for(int k=iterationNum; k<(iterationNum+numEpisodes); k++){
			weightedSim = newWeightedSim();
			currMapping = getMapping(weightedSim);
//			printItypeMapping(currMapping);
			double episodeReward = runOneEpisode(conditionNum, k, game, level1, visuals, controller, seed);
			for(int i=0; i<currMapping.size(); i++){
//				System.out.println(currMapping.size()+" "+mappingQ.size()+" "+i);
//				System.out.println(mappingQ.get(i));
//				System.out.println(mappingQ.get(i).length);
//				System.out.println(currMapping.get(i));
				double q = mappingQ.get(i)[currMapping.get(i)]; //update mappingQ based on reward received
		        double qValue = (1 - Main.mapping_alpha) * q + Main.mapping_alpha * episodeReward;
		        mappingQ.get(i)[currMapping.get(i)] = qValue;
			}
//			printMatrix(mappingQ);
		}
	}
	
	/**
	 * Gets the value function of the object mapped to object obj
	 */
	public ValueFunction getValueFunction(Object obj){
		if(updateQValues)
			return model.qValueFunctions.get(obj.getObjClassId());
		else{
//			System.out.println(currMapping);
//			System.out.println(priorLearnedModel);
//			System.out.println(priorLearnedModel.qValueFunctions);
//			System.out.println(currMapping);
//			printItypeMapping(currMapping);
			if(currMapping.get(obj.getObjClassId()) >= priorLearnedModel.qValueFunctions.size())
				return new ValueFunction(null);
			else
				return priorLearnedModel.qValueFunctions.get(currMapping.get(obj.getObjClassId()));
		}
	}
	
	/**
	 * Uses an epsilon-greedy approach to choose an mapping from the mapping value function
	 * With probability epsilon, a random prior object class (or a new class) is chosen for each new object class
	 * With probability 1-epsilon, the previous object class (or new class) that has the highest Q-value is chosen as the objClass that aligns best with each new object class
	 */
	public ArrayList<Integer> getMapping(ArrayList<double[]> similarityMatrix){
		if(Main.fixedMapping != null){
			return getGreedyMapping(similarityMatrix);
		} else {
			//Epsilon-greedy approach to choosing an mapping
			if(rand.nextDouble() < Main.mapping_epsilon){
				//Choose a random mapping
				ArrayList<Integer> mapping = new ArrayList<Integer>();
				for(int i=0; i<currMapping.size(); i++){
					mapping.add(rand.nextInt(mappingQ.get(i).length));
				}
				return mapping;
			} else { //Otherwise, chooses the best mapping/the one with the highest Q-value
				return getGreedyMapping(similarityMatrix);
			}
		}
	}
	
	public ArrayList<Integer> getGreedyMapping(ArrayList<double[]> similarityMatrix){
		ArrayList<Integer> mapping = new ArrayList<Integer>();
		if(Main.fixedMapping != null){
			//Fixed mapping for debugging
			for(int i=0; i<currMapping.size(); i++)
				mapping.add(-1);
			for(int new_itype : model.getItype_to_objClassId().keySet()){
				int oldIType = Main.fixedMapping.containsKey(new_itype)? Main.fixedMapping.get(new_itype) : -1;
				if(oldIType >= 0)
					mapping.set(model.getItype_to_objClassId().get(new_itype), priorLearnedModel.getItype_to_objClassId().get(oldIType));
				else
					mapping.set(model.getItype_to_objClassId().get(new_itype), priorLearnedModel.qValueFunctions.size());
			}
		} else {
			for(int i=0; i<currMapping.size(); i++){ //For each new object class
				ArrayList<Integer> possibleMappings = new ArrayList<Integer>();
				double maxQ = Integer.MIN_VALUE;
				for(int j=0; j<similarityMatrix.get(i).length; j++){
					double q = similarityMatrix.get(i)[j];
					if(Math.abs(q - maxQ) < 0.0001){ //Basically equal
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
		}
		return mapping;
	}
	
	public void clearEachRun(){
		super.clearEachRun();
		currMapping = null;
		weightedSim = null;
		mappingQ = null;
		objTransitionSim = null;
		objRewardSim = null;
		numOfMappings = null;
	}
	
	public void printMatrix(ArrayList<double[]> matrix){
		for(int i=0; i<matrix.size(); i++){
			for(int j=0; j<matrix.get(i).length; j++){
				System.out.print(matrix.get(i)[j]+" ");
			}
			System.out.println();
		}
	}
}
