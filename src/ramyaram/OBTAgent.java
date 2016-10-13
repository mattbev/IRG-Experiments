package ramyaram;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import core.game.Observation;
import core.game.StateObservation;
import tools.ElapsedCpuTimer;

public class OBTAgent extends OFQAgent {
	private static ArrayList<Integer> currMapping;
	private static ArrayList<double[]> mappingQ; //Q-values over mappings (it specifies for each objClass in the new task, which objClass in the source task it aligns to (or a new class if nothing aligns))
	private static ArrayList<int[]> numOfMappings;
	private static LearnedModel priorLearnedModel;

	private static int numEpisodesMapping = 0;
	private static double mapping_alpha = 0.1; //the learning rate for the mapping phase
	private static double mapping_epsilon = 0.1; //the amount an agent explores (as opposed to exploit) mappings of different object classes
	private static double mapping_epsilon_end = 0.001; //the ending mapping exploration rate (after decreasing to this value, the parameter stays constant)
	private static double mapping_epsilon_delta = 0.01; //exploration over mappings decreases by this delta value over time

	public OBTAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	public LearnedModel run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, LearnedModel priorLearnedModel) {
		if(numEpisodesMapping > Main.numEpisodes){
			System.out.println("Error: number of total episodes is less than the sum of episodes of each phase.");
			System.exit(0);
		}
//		System.out.println("PRIOR LEARNED MODEL "+(priorLearnedModel==null?null:priorLearnedModel.getCondition()));
		OBTAgent.game = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		OBTAgent.priorLearnedModel = priorLearnedModel;
//		System.out.println(prior_itype_id_mapping);
		ArrayList<ValueFunction> priorValueFunctions = priorLearnedModel.getLearnedValueFunctions();
		if(priorValueFunctions != null) { //if a previously learned source task exists
			qValueFunctions = new ArrayList<ValueFunction>();//new ValueFunction[priorValueFunctions.length+1];
			for(int i=0; i<priorValueFunctions.size(); i++)
				qValueFunctions.add(new ValueFunction(priorValueFunctions.get(i).getOptimalQValues()));		
		}
//		System.out.println(itype_to_objClassId.size()+" "+prior_itype_id_mapping.size());
//		for(int i : itype_to_objClassId.keySet())
//			System.out.println(i+" "+itype_to_objClassId.get(i));
//		for(int i : prior_itype_id_mapping.keySet())
//			System.out.println(i+" "+prior_itype_id_mapping.get(i));
//		for(int i=0; i<qValueFunctions.size(); i++)
//			System.out.println(i+" "+qValueFunctions.get(i).getNumNonZero());
//		for(int i : priorLearnedModel.getLearnedIdMapping().keySet())
//			System.out.println(i+" "+priorLearnedModel.getLearnedIdMapping().get(i));
		//create a new empty value function that is learned if no previous ones align well
		qValueFunctions.add(new ValueFunction(null));
		currMapping = new ArrayList<Integer>();
		mappingQ = new ArrayList<double[]>();
		numOfMappings = new ArrayList<int[]>();
		
		int k=0;
		//keep value functions constant and learn mappings between objects
		mappingPhase(conditionNum, k, numEpisodesMapping, game, level1, visuals, controller, seed);
		k+=numEpisodesMapping;
		printItypeMapping(currMapping);
		while(k < numEpisodes){
			currMapping = getGreedyMapping(mappingQ);
//			printItypeMapping(currMapping);
//			System.out.println("CURR MAPPING "+currMapping.size());
			//keep mappings constant and update previously learned value functions
			qValuesPhase(conditionNum, k, 1, game, level1, visuals, controller, seed);
			k+=1;
		}
		ArrayList<Integer> bestMapping = new ArrayList<Integer>();
		for(int i=0; i<mappingQ.size(); i++){
			double maxValue = Integer.MIN_VALUE;
			int maxIndex = -1;
			for(int j=0; j<priorValueFunctions.size(); j++){
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
		return new LearnedModel(qValueFunctions, itype_to_objClassId, Condition.values()[conditionNum], OBTAgent.game);
	}
	
	public void printItypeMapping(ArrayList<Integer> mapping){
		System.out.println("MAPPING");
//		for(int mapping_value : mapping)
//			System.out.print(mapping_value+" ");
//		System.out.println();
		for(int new_itype : itype_to_objClassId.keySet()){
			int newObjClassId = itype_to_objClassId.get(new_itype);
			int oldObjClassId = mapping.get(newObjClassId);
			int oldIType = -1;
			for(Entry<Integer, Integer> entry : priorLearnedModel.getLearnedIdMapping().entrySet()){
				if(entry.getValue() == oldObjClassId)
					oldIType = entry.getKey();
			}
//			System.out.println(new_itype+" "+newObjClassId+" "+oldObjClassId+" "+oldIType);
			System.out.println(new_itype+" --> "+oldIType);
		}
	}
	
	public void addValueFunction(Observation obs){
		
	}
	
	public void processObs(Observation obs, Map<Observation, Object> map){
    	super.processObs(obs, map);
//    	System.out.println(itype_to_objClassId+" "+currMapping);
		if(itype_to_objClassId.get(obs.itype) >= currMapping.size()){
			if(Main.fixedMapping){
				int oldIType = getFixedMappingIType(obs.itype);
				if(oldIType >= 0)
					currMapping.add(priorLearnedModel.getLearnedIdMapping().get(oldIType));//rand.nextInt(qValueFunctions.size()));
				else
					currMapping.add(qValueFunctions.size()-1);
			} else {
				currMapping.add(rand.nextInt(qValueFunctions.size()));
			}
			mappingQ.add(new double[qValueFunctions.size()]);
			numOfMappings.add(new int[qValueFunctions.size()]);
//			System.out.println(currMapping.size()+" "+mappingQ.size()+" "+numOfMappings.size());
		}
    }
	
	public void runEpisode(int conditionNum, int episodeNum, String game, String level1, boolean visuals, String controller, int seed){
		super.runOneEpisode(conditionNum, episodeNum, game, level1, visuals, controller, seed);
        
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
	public void qValuesPhase(int conditionNum, int iterationNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed){
		updateQValues = true;
//		System.out.println(prior_itype_id_mapping);

//		for(int i=0; i<currMapping.size(); i++)
//			System.out.print(currMapping.get(i)+" ");
//		System.out.println();
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
//			System.out.println(prior_itype_id_mapping);
			ArrayList<Integer> mapping = getMapping(mappingQ);
			printItypeMapping(mapping);
//			System.out.println("mapping size "+mappingQ.size()+" "+mapping.size());
			double episodeReward = runOneEpisode(conditionNum, k, game, level1, visuals, controller, seed);
//			System.out.println(prior_itype_id_mapping);

//			System.out.println("mapping size "+mappingQ.size()+" "+mapping.size());
			for(int i=0; i<mapping.size(); i++){
				double q = mappingQ.get(i)[mapping.get(i)]; //update mappingQ based on reward received
		        double qValue = (1 - mapping_alpha) * q + mapping_alpha * episodeReward;
		        mappingQ.get(i)[mapping.get(i)] = qValue;
			}
		}
	}
	
	public ValueFunction getValueFunction(Object obj){
//		System.out.println("get value function for itype "+obj.getItype()+" id "+obj.getObjClassId()+" --> "+currMapping.get(obj.getObjClassId()));
//		for(int i=0; i<currMapping.size(); i++)
//			System.out.print(currMapping.get(i)+" ");
//		System.out.println();
//		System.out.println("new task");
//		printITypeMapping(itype_to_objClassId, obj.getObjClassId());
//		System.out.println("prior task");
//		System.out.println(prior_itype_id_mapping);
//		printITypeMapping(prior_itype_id_mapping, currMapping.get(obj.getObjClassId()));
//		System.out.println();
//		System.out.println(obj.getObjClassId());
//		System.out.println(currMapping.get(obj.getObjClassId()));
//		System.out.println(qValueFunctions.size());
		return qValueFunctions.get(currMapping.get(obj.getObjClassId()));
	}
	
	public int getFixedMappingIType(int newIType){
//		System.out.println(game+" "+priorLearnedModel.getGame());
//		System.exit(0);
		//identity mapping for transfer to the same game -- used for sanity check
		if(game.equalsIgnoreCase(priorLearnedModel.getGame())){
			return newIType;
		} else if(game.equalsIgnoreCase("ramyaNormandy") && priorLearnedModel.getGame().equalsIgnoreCase("ramyaFreeway")){
			if(priorLearnedModel.getLearnedIdMapping().containsKey(newIType))
				return newIType;
			else
				return -1;
		} else if((game.equalsIgnoreCase("ramyaNormandy2") || game.equalsIgnoreCase("ramyaNormandy3")) && priorLearnedModel.getGame().equalsIgnoreCase("ramyaFreeway")){
			switch(newIType){
				case 6: return 7; 
				default: return -1;
			}
		} else if(game.equalsIgnoreCase("missilecommand") && priorLearnedModel.getGame().equalsIgnoreCase("aliens")){
			switch(newIType){
				case 1: return 1; 
//				case 7: return 6; 
//				case 0: return 3; 
				case 4: return 5; 
				default: return -1;
			}
		} else if(game.equalsIgnoreCase("boulderdash") && priorLearnedModel.getGame().equalsIgnoreCase("digdug")){
			switch(newIType){
				case 1: return 1;
				case 6: return 5;
				case 10: return 11;
//				case 4: return 0;
				default: return -1;
			}
		} else if(game.equalsIgnoreCase("aliens") && priorLearnedModel.getGame().equalsIgnoreCase("missilecommand")){
			switch(newIType){
				case 3: return 0; 
				case 5: return 4; 
				case 6: return 7; 
				case 9: return 7;
				case 1: return 1;
				default: return -1;
			}
		}
		return -1;
	}
	
	/**
	 * Uses an epsilon-greedy approach to choose an mapping from the mapping value function
	 * With probability epsilon, a random prior object class (or a new class) is chosen for each new object class
	 * With probability 1-epsilon, the previous object class (or new class) that has the highest Q-value is chosen as the objClass that aligns best with each new object class
	 */
	public ArrayList<Integer> getMapping(ArrayList<double[]> similarityMatrix){
		if(Main.fixedMapping){
			return getGreedyMapping(similarityMatrix);
		} else {
			//epsilon-greedy approach to choosing an mapping
			if(rand.nextDouble() < mapping_epsilon){
				// choose a random mapping
				ArrayList<Integer> mapping = new ArrayList<Integer>();
				for(int i=0; i<currMapping.size(); i++){
					mapping.add(rand.nextInt(mappingQ.get(i).length));
				}
				return mapping;
			} else { // otherwise, chooses the best mapping/the one with the highest Q-value
				return getGreedyMapping(similarityMatrix);
			}
		}
	}
	
	public ArrayList<Integer> getGreedyMapping(ArrayList<double[]> similarityMatrix){
		ArrayList<Integer> mapping = new ArrayList<Integer>();
		if(Main.fixedMapping){
			//fixed mapping for debugging
			for(int i=0; i<currMapping.size(); i++)
				mapping.add(-1);
			for(int new_itype : itype_to_objClassId.keySet()){
				int oldIType = getFixedMappingIType(new_itype);
				if(oldIType >= 0)
					mapping.set(itype_to_objClassId.get(new_itype), priorLearnedModel.getLearnedIdMapping().get(oldIType));
				else
					mapping.set(itype_to_objClassId.get(new_itype), qValueFunctions.size()-1);
			}
		} else {
			for(int i=0; i<currMapping.size(); i++){ // for each new object class
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
		}
//		System.out.println("prior itype to id");
//		for(int i : priorLearnedModel.getLearnedIdMapping().keySet())
//			System.out.println(i+" --> "+priorLearnedModel.getLearnedIdMapping().get(i));
//		System.out.println("new itype to id");
//		for(int i : itype_to_objClassId.keySet())
//			System.out.println(i+" --> "+itype_to_objClassId.get(i));
//		System.out.println("mapping between classes, new --> old");
//		for(int i=0; i<mapping.size(); i++)
//			System.out.println(i+" --> "+mapping.get(i));
//		printItypeMapping(mapping);
		return mapping;
	}
	
	public void clearEachRun(){
		super.clearEachRun();
		currMapping = null;
		mappingQ = null;
		numOfMappings = null;
	}
}
