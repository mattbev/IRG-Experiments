package ramyaram;

import ontology.Types;
import tools.Vector2d;

/**
 * Represents an object-based value function Q(s_a, s_o, a)
 */
public class ValueFunction {
	public double[][][] optimalQValues;
	public int objClassItype; //object class itype associated with this value function
	public int previousObjClassItype; //previous object class itype if copied from a previous object class
	
	public ValueFunction(double[][][] optimalQValues, int objClassItype, int previousObjClassItype){
		this(optimalQValues, objClassItype, previousObjClassItype, Agent.numRows, Agent.numCols);
	}
	
	public ValueFunction(double[][][] optimalQValues, int objClassItype, int previousObjClassItype, int numRows, int numCols){
		this.optimalQValues = new double[numCols*2+1][numRows*2+1][Types.ACTIONS.values().length];
		this.objClassItype = objClassItype;
		this.previousObjClassItype = previousObjClassItype;
		
		//if a value function is passed in, make a copy of it
		if(optimalQValues != null){
			for(int i=0; i<this.optimalQValues.length; i++){
				for(int j=0; j<this.optimalQValues[i].length; j++){
					for(int k=0; k<this.optimalQValues[i][j].length; k++){
						this.optimalQValues[i][j][k] = optimalQValues[i][j][k];
					}
				}
			}
		}
	}
	
	public double getOptimalQValue(Vector2d agent, Vector2d obj, Types.ACTIONS action){
		return optimalQValues[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()];
	}
	
	public void setOptimalQValue(Vector2d agent, Vector2d obj, Types.ACTIONS action, double value){
		optimalQValues[Agent.getXDist(agent, obj)+Agent.numCols-1][Agent.getYDist(agent, obj)+Agent.numRows-1][action.ordinal()] = value;
	}

	public double[][][] getOptimalQValues() {
		return optimalQValues;
	}
	
	/**
	 * Gets the number of non-zero elements in the value function
	 */
	public int getNumNonZero(){
		int num = 0;
		for(int i=0; i<optimalQValues.length; i++){
			for(int j=0; j<optimalQValues[i].length; j++){
				for(int k=0; k<optimalQValues[i][j].length; k++){
					if(optimalQValues[i][j][k] > 0 || optimalQValues[i][j][k] < 0){
						num++;
					}
				}
			}
		}
		return num;
	}
}