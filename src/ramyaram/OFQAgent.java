package ramyaram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
	
	/**
	 * Runs the Object-Focused Q-learning algorithm with the given parameters
	 */
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, Model priorLearnedModel) {
		model = new Model();
		gameName = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		updateQValues = true;
		//run OF-Q
		for(int i=0; i<numEpisodes; i++)
			runOneEpisode(conditionNum, i, game, level1, visuals, controller);
		//save learned model to a file
		model.writeToFile(Constants.writeModelFile);
		return model;
	}
	
	/**
	 * Epsilon-greedy approach to choosing an action
	 */
	public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	if(rand.nextDouble() < Constants.epsilon) //choose a random action
            return actions.get(rand.nextInt(actions.size()));
    	else //choose greedy action based on value function
    		return getGreedyAction(stateObs, actions);
	}
    
	/**
	 * Chooses the action with the maximum Q-value over all objects and all actions 
	 */
    public Types.ACTIONS getGreedyAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	double maxValue = Integer.MIN_VALUE;
		List<Types.ACTIONS> possibleActions = new ArrayList<Types.ACTIONS>();
		for(Observation obs : objectMap.keySet()){
			Object obj = objectMap.get(obs);
			for(Types.ACTIONS action : actions){
				double value = getValueFunction(obj).getOptimalQValue(getAvatarGridPos(stateObs), obj.getGridPos(), action);
//				System.out.println("obj_type "+obs.itype+" action "+action+" = value "+value);
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
//		for(int j=0; j<possibleActions.size(); j++)
//			System.out.print(possibleActions.get(j)+" ");
//		System.out.println();
		return possibleActions.get(rand.nextInt(possibleActions.size()));
    }
    
    /**
	 * Updates each object class's Q-value function for the given state, action, and next state
	 */
    public void updateQValues(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions){		
    	for(Observation obs : objectMap.keySet()) {
    		if(obs.category != Types.TYPE_AVATAR){
	    		Object currObj = objectLastStateMap.get(obs);
	    		Object nextObj = objectMap.get(obs);
	    		ValueFunction qValues = getValueFunction(currObj);
				double q = qValues.getOptimalQValue(getAvatarGridPos(stateObs), currObj.getGridPos(), action);
				double maxQ = 0;
				if(nextObj != null)
					maxQ = optimalMaxQ(qValues, getAvatarGridPos(nextStateObs), nextObj.getGridPos(), actions);	
		        double qValue = getOneQValueUpdate(q, reward, maxQ);
		        qValues.setOptimalQValue(getAvatarGridPos(stateObs), currObj.getGridPos(), action, qValue);
    		}
    	}
    }
    
    /**
     * Adds an object class to the model when it's first seen in this task
     */
    public void processObs(Observation obs, Map<Observation, Object> map){
    	super.processObs(obs, map);
    	if(model.getItype_to_objClassId().get(obs.itype) >= model.qValueFunctions.size())
			model.addObjClassToModel(obs.itype);
    }
    
    /**
     * Gets the appropriate object class value function for the given object
     */
    public ValueFunction getValueFunction(Object obj){
		return model.qValueFunctions.get(obj.getObjClassId());
	}
    
    /**
	 * Computes the maximum Q-value for this state
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
    
	/**
	 * Update Q-value functions if updateQValues variable is set to true (in Object-Focused Q-learning, this is always set to true)
	 */
    public void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions) {
        if(updateQValues)
        	updateQValues(stateObs, action, nextStateObs, reward, actions);
    }
}