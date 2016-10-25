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

/**
 * Object-Focused Q-learning agent
 * Learns a value function for each object class in the task rather than one big value function for the entire task
 */
public class OFQAgent extends Agent {
	public OFQAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, Model priorLearnedModel) {
		model = new Model(game);
		OFQAgent.game = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		updateQValues = true;
//		runOneEpisode(conditionNum, 0, game, level1, true, controller, seed);
		for(int i=0; i<numEpisodes; i++)
        	runOneEpisode(conditionNum, i, game, level1, visuals, controller, seed);	
//		runOneEpisode(conditionNum, numEpisodes, game, level1, true, controller, seed);
		return model;
	}
	
	public double runOneEpisode(int conditionNum, int episodeNum, String game, String level1, boolean visuals, String controller, int seed){
		System.out.println("Episode "+episodeNum);
        double[] result = ArcadeMachine.runOneGame(game, level1, visuals, controller, null, seed, 0);
        if(episodeNum % Main.interval == 0){
        	Main.reward[conditionNum][(episodeNum/Main.interval)] += result[1]; //score of the game
        	if(result[0] == Types.WINNER.PLAYER_WINS.key())
        		Main.wins[(episodeNum/Main.interval)] = true;
        	Main.writeToFile(Main.allRewardFile, result[1]+", ");
        }
        return result[1];
	}
	
	/**
	 * Epislon-greedy approach to choosing an action
	 */
	public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	if(rand.nextDouble() < Main.epsilon){ //choose a random action
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
			for(Types.ACTIONS action : actions){
				double value = getValueFunction(obj).getOptimalQValue(stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action);
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
	    		ValueFunction qValues = getValueFunction(currObj); //qValueFunctions[currObj.objectClassId];
				double q = qValues.getOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action);
				double maxQ = 0;
				if(nextObj != null)
					maxQ = optimalMaxQ(qValues, nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), actions);	
		        double qValue = getOneQValueUpdate(q, reward, maxQ);
		        qValues.setOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action, qValue);
    		}
    	}
    }
    
    public void processObs(Observation obs, Map<Observation, Object> map){
    	super.processObs(obs, map);
    	if(model.getItype_to_objClassId().get(obs.itype) >= model.qValueFunctions.size())
			model.addObjToModel();
    }
    
    public ValueFunction getValueFunction(Object obj){
		return model.qValueFunctions.get(obj.getObjClassId());
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
    }
}