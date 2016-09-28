package ramyaram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import core.ArcadeMachine;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class OFQAgent extends Agent {
	protected static ArrayList<ValueFunction> qValueFunctions;

	public OFQAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	public LearnedModel run(int conditionNum, int numEpisodes, String game, String level1, String controller, int seed, LearnedModel priorLearnedModel) {
//		System.out.println("in ofq run");
		updateQValues = true;
		qValueFunctions = new ArrayList<ValueFunction>();
		for(int i=0; i<numEpisodes; i++)
        	runOneEpisode(conditionNum, i, game, level1, controller, seed);		
		return new LearnedModel(qValueFunctions, itype_to_objClassId);
	}
	
	public double runOneEpisode(int conditionNum, int episodeNum, String game, String level1, String controller, int seed){
		System.out.println("Episode "+episodeNum);
        double[] result = ArcadeMachine.runOneGame(game, level1, false, controller, null, seed, 0);
        if(episodeNum % Main.interval == 0){
//        	System.out.println("episodeNum "+episodeNum+" interval "+Main.interval);
//        	System.out.println(episodeNum+" "+Main.interval+" "+(episodeNum/Main.interval));
        	Main.reward[conditionNum][(episodeNum/Main.interval)] += result[1]; //score of the game
        	if(result[0] == Types.WINNER.PLAYER_WINS.key())
        		Main.wins[(episodeNum/Main.interval)] = true;
        	Main.writeToFile(Main.allDataFileName, result[1]+", ");
        }
        return result[1];
	}
	
	/**
	 * Epislon-greedy approach to choosing an action
	 */
	public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	if(rand.nextDouble() < epsilon){ //choose a random action
    		int index = rand.nextInt(actions.size());
            Types.ACTIONS action = actions.get(index);
            return action;
    	} else { //choose greedy action based on value function
    		return greedy(stateObs, actions);
    	}
	}
    
    public Types.ACTIONS greedy(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	double maxValue = Integer.MIN_VALUE;
		List<Types.ACTIONS> possibleActions = new ArrayList<Types.ACTIONS>();
		for(Observation obs : objectMap.keySet()){
			Object obj = objectMap.get(obs);
			for(Types.ACTIONS action : actions){//safeActions){
				double value = getValueFunction(obj).getOptimalQValue(stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action);
//				System.out.println(obj.objectClassId+" "+stateObs.getAvatarPosition()+" "+new Vector2d(obj.getFeature(0), obj.getFeature(1))+" "+action+" "+value);
				if(Math.abs(value - maxValue) < 0.001 && !possibleActions.contains(action)){ //basically equal
					possibleActions.add(action);
					maxValue = Math.max(value, maxValue);
				}
				else if(value > maxValue){
					maxValue = value;
					possibleActions.clear();
					if (!possibleActions.contains(action))
						possibleActions.add(action);
				}
    		}
		}
		return possibleActions.get(rand.nextInt(possibleActions.size()));
    }
    
    /**
	 * Updates the current Q-values for this particular state, action, and next state
	 */
    public void updateQValues(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions){		
    	for(Observation obs : objectMap.keySet()) {
    		if(obs.category != Types.TYPE_AVATAR){
	    		Object currObj = objectMap.get(obs);
	    		Object nextObj = objectNextStateMap.get(obs);
//	    		System.out.println("obj class id "+currObj.objectClassId);
	    		ValueFunction qValues = getValueFunction(currObj); //qValueFunctions[currObj.objectClassId];
				double q = qValues.getOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action);
				double maxQ = 0;
				if(nextObj != null)
					maxQ = optimalMaxQ(qValues, nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), actions);	
		        double qValue = getOneQValueUpdate(q, reward, maxQ);
//		        System.out.println(obs.itype+", "+getGridCellFromPixels(obs.position)+" --> "+qValues);
//		        System.out.println(getGridCellFromPixels(lastAvatarPos)+" "+getGridCellFromPixels(new Vector2d(currObj.getFeature(0), currObj.getFeature(1)))+" "+action+" "+qValue);
//		        System.out.println("before "+qValues.getOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action));
		        qValues.setOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action, qValue);
//		        System.out.println("after "+qValues.getOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action));
    		}
    	}
//    	System.out.println();
    }
    
    public void processObs(Observation obs, Map<Observation, Object> map){
    	super.processObs(obs, map);
    	addValueFunction(obs);
    }
    
    public void addValueFunction(Observation obs){
		if(itype_to_objClassId.get(obs.itype) >= qValueFunctions.size())
			qValueFunctions.add(new ValueFunction(null));
    }
    
    public ValueFunction getValueFunction(Object obj){
		return qValueFunctions.get(obj.getObjClassId());
	}
    
    /**
	 * Computes the maximum Q-value over the optimal value function for this state
	 */
	public double optimalMaxQ(ValueFunction qValues, Vector2d agent, Vector2d obj, ArrayList<Types.ACTIONS> actions) {
		double maxValue = Integer.MIN_VALUE;
		for(Types.ACTIONS action : actions){
			double value = qValues.getOptimalQValue(agent, obj, action);
			if(value > maxValue)
				maxValue = value;
		}
		return maxValue;
	}
    
    public void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions) {
        if(updateQValues)
        	updateQValues(stateObs, action, nextStateObs, reward, actions);
    	objectMap.clear();
        objectNextStateMap.clear();
        gridObjectMap.clear();
        gridObjectNextStateMap.clear();
    }
    
    public void clearEachRun(){
    	super.clearEachRun();
    	qValueFunctions = null;
    }
}