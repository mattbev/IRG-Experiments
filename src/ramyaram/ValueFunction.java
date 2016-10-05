package ramyaram;

import ontology.Types;
import tools.Vector2d;

public class ValueFunction {
	public double[][][] optimalQValues;
	
	public ValueFunction(double[][][] optimalQValues){
		this.optimalQValues = new double[Agent.numCols*2+1][Agent.numRows*2+1][Types.ACTIONS.values().length];
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
		return optimalQValues[getXDist(agent, obj)+Agent.numCols-1][getYDist(agent, obj)+Agent.numRows-1][action.ordinal()];
	}
	
	public void setOptimalQValue(Vector2d agent, Vector2d obj, Types.ACTIONS action, double value){
		optimalQValues[getXDist(agent, obj)+Agent.numCols-1][getYDist(agent, obj)+Agent.numRows-1][action.ordinal()] = value;
	}
	
	public int getXDist(Vector2d agent, Vector2d obj){
		return (int) (Agent.getGridCellFromPixels(agent).x - Agent.getGridCellFromPixels(obj).x);
	}
	
	public int getYDist(Vector2d agent, Vector2d obj){
		return (int) (Agent.getGridCellFromPixels(agent).y - Agent.getGridCellFromPixels(obj).y);
	}

	public double[][][] getOptimalQValues() {
		return optimalQValues;
	}
	
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