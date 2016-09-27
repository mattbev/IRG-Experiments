package ramyaram;

import java.util.ArrayList;
import java.util.Map;

import core.game.Observation;
import core.game.StateObservation;
import tools.ElapsedCpuTimer;

public class OBTAgent extends OFQAgent {
	private static ArrayList<Integer> currMapping;
	private static ArrayList<double[]> mappingQ; //Q-values over mappings (it specifies for each objClass in the new task, which objClass in the source task it aligns to (or a new class if nothing aligns))
	private static ArrayList<int[]> numOfMappings;

	private static int numEpisodesMapping = 0;
	private static int numEpisodesQValues = 100;
	private static double mapping_alpha = 0.1; //the learning rate for the mapping phase
	private static double mapping_epsilon = 0.1; //the amount an agent explores (as opposed to exploit) mappings of different object classes
	private static double mapping_epsilon_end = 0.001; //the ending mapping exploration rate (after decreasing to this value, the parameter stays constant)
	private static double mapping_epsilon_delta = 0.01; //exploration over mappings decreases by this delta value over time

	public OBTAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	public ArrayList<ValueFunction> run(int conditionNum, int numEpisodes, String game, String level1, String controller, int seed, ArrayList<ValueFunction> priorValueFunctions) {
		if((numEpisodesMapping+numEpisodesQValues)>Main.numEpisodes){
			System.out.println("Error: number of total episodes is less than the sum of episodes of each phase.");
			System.exit(0);
		}
		if(priorValueFunctions != null) { //if a previously learned source task exists
//			System.out.println("prioValueFucntions size "+priorValueFunctions.size());
			qValueFunctions = new ArrayList<ValueFunction>();//new ValueFunction[priorValueFunctions.length+1];
//			System.out.println("qvaluefunctions size "+qValueFunctions.size());
			for(int i=0; i<priorValueFunctions.size(); i++)
				qValueFunctions.add(new ValueFunction(priorValueFunctions.get(i).getOptimalQValues()));		
		}
		//create a new empty value function that is learned if no previous ones align well
		qValueFunctions.add(new ValueFunction(null));
//		System.out.println("qvalue functions SIZe "+qValueFunctions.size());
//		System.out.println("run OBT "+qValueFunctions.length);
		currMapping = new ArrayList<Integer>();
//		for(int i=0; i<currMapping.length; i++)
//			currMapping[i] = i;
		mappingQ = new ArrayList<double[]>();
		numOfMappings = new ArrayList<int[]>();
		
		int k=0;
		//keep value functions constant and learn mappings between objects
		mappingPhase(conditionNum, k, numEpisodesMapping, game, level1, controller, seed);
		k+=numEpisodesMapping;
		currMapping = getGreedyMapping(mappingQ);
		for(int i=0; i<currMapping.size(); i++)
			System.out.print(currMapping.get(i)+" ");
		System.out.println();
//			currMapping[i] = i;
		while(k < numEpisodes){
			//keep mappings constant and update previously learned value functions
			qValuesPhase(conditionNum, k, numEpisodesQValues, game, level1, controller, seed);
			k+=numEpisodesQValues;
		}
		int[] bestMapping = new int[getNumObjClasses()];
		for(int i=0; i<bestMapping.length; i++){
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
			bestMapping[i] = maxIndex;
			numOfMappings.get(i)[bestMapping[i]]++;
		}
		System.out.println("-------");

		for(int i=0; i<bestMapping.length; i++)
			System.out.print(bestMapping[i]+" ");
		System.out.println();
		return qValueFunctions;
	}
	
	public void addValueFunction(Observation obs){
		
	}
	
	public void processObs(Observation obs, Map<Observation, Object> map){
    	super.processObs(obs, map);
		if(itype_to_objClassId.get(obs.itype) >= currMapping.size()){
			currMapping.add(rand.nextInt(qValueFunctions.size()));
			mappingQ.add(new double[qValueFunctions.size()]);
			numOfMappings.add(new int[qValueFunctions.size()]);
			System.out.println(currMapping.size()+" "+mappingQ.size()+" "+numOfMappings.size());
		}
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
		for(int i=0; i<currMapping.size(); i++)
			System.out.print(currMapping.get(i)+" ");
		System.out.println();
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
			ArrayList<Integer> mapping = getMapping(mappingQ);
//			System.out.println("mapping size "+mappingQ.size()+" "+mapping.size());
			double episodeReward = runOneEpisode(conditionNum, k, game, level1, controller, seed);
//			System.out.println("mapping size "+mappingQ.size()+" "+mapping.size());
			for(int i=0; i<mapping.size(); i++){
				double q = mappingQ.get(i)[mapping.get(i)]; //update mappingQ based on reward received
		        double qValue = (1 - mapping_alpha) * q + mapping_alpha * episodeReward;
		        mappingQ.get(i)[mapping.get(i)] = qValue;
			}
		}
	}
	
	public ValueFunction getValueFunction(Object obj){
//		System.out.println("get value function for obj class "+obj.objectClassId+" --> "+currMapping[obj.objectClassId]);
//		System.out.println(obj.getObjClassId());
//		System.out.println(currMapping.get(obj.getObjClassId()));
//		System.out.println(qValueFunctions.size());
		return qValueFunctions.get(currMapping.get(obj.getObjClassId()));
	}
	
	/**
	 * Uses an epsilon-greedy approach to choose an mapping from the mapping value function
	 * With probability epsilon, a random prior object class (or a new class) is chosen for each new object class
	 * With probability 1-epsilon, the previous object class (or new class) that has the highest Q-value is chosen as the objClass that aligns best with each new object class
	 */
	public ArrayList<Integer> getMapping(ArrayList<double[]> similarityMatrix){
		ArrayList<Integer> mapping = new ArrayList<Integer>();
		for(int i=0; i<currMapping.size(); i++){ 
			mapping.add(i);
		}
		return mapping;
		//epsilon-greedy approach to choosing an mapping
//		if(rand.nextDouble() < mapping_epsilon){
//			// choose a random mapping
//			ArrayList<Integer> mapping = new ArrayList<Integer>();
//			for(int i=0; i<currMapping.size(); i++){
//				mapping.add(rand.nextInt(getNumObjClasses()));
////				System.out.print(mapping.get(i)+" ");
//			}
////			System.out.println("that was random "+mapping.size());
//			return mapping;
//		} else { // otherwise, chooses the best mapping/the one with the highest Q-value
//			return getGreedyMapping(similarityMatrix);
//		}
	}
	
	public ArrayList<Integer> getGreedyMapping(ArrayList<double[]> similarityMatrix){
		ArrayList<Integer> mapping = new ArrayList<Integer>();
		for(int i=0; i<currMapping.size(); i++){ 
			mapping.add(i);
		}
		return mapping;
//		System.out.println(mappingQ.size());
//		for(int i=0; i<currMapping.size(); i++){ // for each new object class
//			ArrayList<Integer> possibleMappings = new ArrayList<Integer>();
//			double maxQ = Integer.MIN_VALUE;
//			for(int j=0; j<similarityMatrix.get(i).length; j++){
//				double q = similarityMatrix.get(i)[j];
//				if(Math.abs(q - maxQ) < 0.0001){ //basically equal
//					possibleMappings.add(j);
//					maxQ = Math.max(q, maxQ);
//				}
//				else if(q > maxQ){
//					maxQ = q;
//					possibleMappings.clear();
//					possibleMappings.add(j);
//				}
//			}
//			System.out.println("mapping length "+mapping.size());
//			mapping.add(possibleMappings.get(rand.nextInt(possibleMappings.size())));
//			System.out.print(mapping.get(i)+" ");
//		}
//		System.out.println("that was greedy "+mapping.size());
//		return mapping;
	}
	
	public void clearEachRun(){
		super.clearEachRun();
//		System.out.println("clearing run");
		currMapping = null;
		mappingQ = null;
		numOfMappings = null;
	}
}
