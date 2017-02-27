package ramyaram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
	protected static HashMap<Observation, Object> objectLastStateMap = new HashMap<Observation, Object>();
	protected static HashMap<Observation, Object> objectMap = new HashMap<Observation, Object>();
	protected static Model model;

	public OFQAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	/**
	 * Runs the Object-Focused Q-learning algorithm with the given parameters
	 */
	@Override
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller) {
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
	@Override
	public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	if(rand.nextDouble() < Constants.epsilon) //choose a random action
            return actions.get(rand.nextInt(actions.size()));
    	else //choose greedy action based on value function
    		return getGreedyAction(stateObs, actions);
	}
	
	/**
	 * Update Q-value functions if updateQValues variable is set to true (in Object-Focused Q-learning, this is always set to true)
	 */
	@Override
    public void updateEachStep(StateObservation state, Types.ACTIONS action, StateObservation state2, double reward, ArrayList<Types.ACTIONS> actions){		
    	HashMap<Observation, Object> stateObjMap = objectLastStateMap;
    	HashMap<Observation, Object> state2ObjMap = objectMap;
    	if(updateQValues)
        	updateQValues(state, stateObjMap, action, state2, state2ObjMap, reward, actions);
        objectLastStateMap = new HashMap<Observation, Object>(objectMap); 
        objectMap.clear();
    }
    
    /**
     * Converts the given state observation into a map to keep track of objects in the current state
     */
    @Override
    public void processStateObs(StateObservation stateObs){
    	HashMap<Observation, Object> map = objectMap;
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
		for (int i = 0; i < observationGrid.length; i++) {
			for (int j = 0; j < observationGrid[i].length; j++) {
				ArrayList<Observation> obsList = new ArrayList<Observation>();
				for (Observation obs : observationGrid[i][j]) {
					if(getImportantObjects(gameName) != null){
						if(getImportantObjects(gameName).contains(obs.itype))
							obsList.add(obs);
					} else {
						if(!Arrays.asList().contains(obs.itype))
							obsList.add(obs);
					}
				}
				for(Observation obs : obsList)
					processObs(obs, map);
			}
		}
    }
    
    @Override
    public void clearEachRun(){
    	super.clearEachRun();
    	if(model != null)
    		model.clear();
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
    public void updateQValues(StateObservation state, HashMap<Observation, Object> stateObjMap, Types.ACTIONS action, StateObservation state2, HashMap<Observation, Object> state2ObjMap, double reward, ArrayList<Types.ACTIONS> actions){		
    	Vector2d avatarPos = getAvatarGridPos(state);
    	Vector2d avatarPos2 = getAvatarGridPos(state2);
    	for(Observation obs : stateObjMap.keySet()) {
    		if(obs.category != Types.TYPE_AVATAR){
	    		Object o_state = stateObjMap.get(obs);
	    		Object o_state2 = state2ObjMap.get(obs);
	    		ValueFunction qValues = getValueFunction(o_state);
				double q = qValues.getOptimalQValue(avatarPos, o_state.getGridPos(), action);
				double maxQ = 0;
				if(o_state2 != null)
					maxQ = optimalMaxQ(qValues, avatarPos2, o_state2.getGridPos(), actions);	
		        double qValue = getOneQValueUpdate(q, reward, maxQ);
		        qValues.setOptimalQValue(avatarPos, o_state.getGridPos(), action, qValue);
    		}
    	}
    }
    
    /**
     * Process a single observation
     * Adds an object class to the model when it's first seen in this task
     */
    public void processObs(Observation obs, HashMap<Observation, Object> map){
    	if(!model.getItype_to_objClassId().containsKey(obs.itype))
    		model.getItype_to_objClassId().put(obs.itype, model.getItype_to_objClassId().size());
    	Vector2d gridPos = getGridCellFromPixels(obs.position);
		Object o = new Object(model.getItype_to_objClassId().get(obs.itype), obs.itype, gridPos);
		map.put(obs, o);
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
}