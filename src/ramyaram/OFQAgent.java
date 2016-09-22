package ramyaram;

import java.util.ArrayList;
import java.util.List;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class OFQAgent extends Agent {
    protected static int numObjClasses = 20;
	protected static ObjectBasedValueFunction[] qValueFunctions;

	public OFQAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
		if(qValueFunctions == null){
	        System.out.println("init");
			qValueFunctions = new ObjectBasedValueFunction[numObjClasses];
	    	for(int i=0; i<qValueFunctions.length; i++)
				qValueFunctions[i] = new ObjectBasedValueFunction(null);
		}
	}
	
	/**
	 * Updates the current Q-values for this particular state, action, and next state
	 */
    public void updateQValues(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions){		
    	for(Observation obs : objectMap.keySet()) {
    		if(obs.category != Types.TYPE_AVATAR){
	    		Object currObj = objectMap.get(obs);
	    		Object nextObj = objectNextStateMap.get(obs);
	    		ObjectBasedValueFunction qValues = qValueFunctions[currObj.objectClassId];
				double q = qValues.getOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action);
				double maxQ = 0;
				if(nextObj != null)
					maxQ = optimalMaxQ(qValues, nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), actions);	
		        double qValue = getOneQValueUpdate(q, reward, maxQ);
		        qValues.setOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action, qValue);
    		}
    	}
    }
    
    public Types.ACTIONS greedy(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	double maxValue = Integer.MIN_VALUE;
		List<Types.ACTIONS> possibleActions = new ArrayList<Types.ACTIONS>();
		for(Observation obs : objectMap.keySet()){
			Object obj = objectMap.get(obs);
			int objClassId = obj.objectClassId;
			for(Types.ACTIONS action : actions){//safeActions){
				double value = qValueFunctions[objClassId].getOptimalQValue(stateObs.getAvatarPosition(), new Vector2d(obj.getFeature(0), obj.getFeature(1)), action);
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
	 * Computes the maximum Q-value over the optimal value function for this state
	 */
	public double optimalMaxQ(ObjectBasedValueFunction qValues, Vector2d agent, Vector2d obj, ArrayList<Types.ACTIONS> actions) {
		double maxValue = Integer.MIN_VALUE;
		for(Types.ACTIONS action : actions){
			double value = qValues.getOptimalQValue(agent, obj, action);
			if(value > maxValue)
				maxValue = value;
		}
		return maxValue;
	}
    
    public void clearEachStep(){
    	objectMap.clear();
        objectNextStateMap.clear();
        gridObjectMap.clear();
        gridObjectNextStateMap.clear();
    }
    
    public void clearEachRun(){
    	qValueFunctions = null;
    }
}
