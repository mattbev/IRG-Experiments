package ramyaram;

import java.util.ArrayList;
import java.util.List;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class QLearningAgent extends Agent {
	protected static int maxStates = 100000;
	protected static ValueFunction qValueFunction;
	
	public QLearningAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
		if(qValueFunction == null){
	        System.out.println("init");
	        qValueFunction = new ValueFunction(null);
		}
	}
	
    public void updateQValues(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions){		
    	double q = qValueFunction.getOptimalQValue(getStateId(stateObs), action);
    	double maxQ = optimalMaxQ(qValueFunction, getStateId(nextStateObs), actions);	
    	double newValue = getOneQValueUpdate(q, reward, maxQ);
    	qValueFunction.setOptimalQValue(getStateId(stateObs), action, newValue);
    }
	
    public Types.ACTIONS greedy(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	double maxValue = Integer.MIN_VALUE;
		List<Types.ACTIONS> possibleActions = new ArrayList<Types.ACTIONS>();
		for(Types.ACTIONS action : actions){
			System.out.println(getStateId(stateObs));
			double value = qValueFunction.getOptimalQValue(getStateId(stateObs), action);
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
		return possibleActions.get(rand.nextInt(possibleActions.size()));
//    	int index = rand.nextInt(actions.size());
//        Types.ACTIONS action = actions.get(index);
//        return action;
    }
    
    /**
	 * Computes the maximum Q-value over the optimal value function for this state
	 */
	public double optimalMaxQ(ValueFunction qValues, int stateNum, ArrayList<Types.ACTIONS> actions) {
		double maxValue = Integer.MIN_VALUE;
		for(Types.ACTIONS action : actions){
			double value = qValues.getOptimalQValue(stateNum, action);
			if(value > maxValue)
				maxValue = value;
		}
		return maxValue;
	}
    
    public int getStateId(StateObservation stateObs){
    	int id = 0;
    	int numObjs = objectMap.values().size();
    	int numGridLocs = numCols * numRows;
    	System.out.println(numGridLocs+" "+numObjs+" "+Math.pow(numGridLocs, numObjs));
    	int count = 0;
    	for(Observation obs : objectMap.keySet()){
    		int power = numObjs-1 - count;
			id += Math.pow(numGridLocs, power)*getGridLocId(obs.position);
			count++;
		}
		return id;
    }
    
    public int getGridLocId(Vector2d position){
    	Vector2d cellPosition = getGridCellFromPixels(position);
    	return ((int)cellPosition.x)*numRows + ((int)cellPosition.y);
    }

	public void clearEachRun() {
		qValueFunction = null;
	}
}
