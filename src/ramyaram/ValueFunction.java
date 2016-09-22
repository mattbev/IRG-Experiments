package ramyaram;

import ontology.Types;

public class ValueFunction {
	public double[][] optimalQValues;
	public ValueFunction(double[][] optimalQValues){
		this.optimalQValues = new double[QLearningAgent.maxStates][Types.ACTIONS.values().length];
		if(optimalQValues != null){
			for(int i=0; i<this.optimalQValues.length; i++){
				for(int j=0; j<this.optimalQValues[i].length; j++){
					this.optimalQValues[i][j] = optimalQValues[i][j];
				}
			}
		}
	}
	public double getOptimalQValue(int stateNum, Types.ACTIONS action){
		return optimalQValues[stateNum][action.ordinal()];
	}
	
	public void setOptimalQValue(int stateNum, Types.ACTIONS action, double value){
		optimalQValues[stateNum][action.ordinal()] = value;
	}
}
