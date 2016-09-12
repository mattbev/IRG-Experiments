package ramyaram;

import ontology.Types;
import tools.Vector2d;

public class ValueFunction {
	public double[][][] optimalQValues;
	public double[][][] randomQValues;
	
	public ValueFunction(double[][][] optimalQValues, double[][][] randomQValues){
		this.optimalQValues = new double[Agent.numCols*2+1][Agent.numRows*2+1][Types.ACTIONS.values().length];
		this.randomQValues = new double[Agent.numCols*2+1][Agent.numRows*2+1][Types.ACTIONS.values().length];
		if(optimalQValues != null && randomQValues != null){
			for(int i=0; i<this.optimalQValues.length; i++){
				for(int j=0; j<this.optimalQValues[i].length; j++){
					for(int k=0; k<this.optimalQValues[i][j].length; k++){
						this.optimalQValues[i][j][k] = optimalQValues[i][j][k];
						this.randomQValues[i][j][k] = randomQValues[i][j][k]; 
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

	public Vector2d getGridCellFromPixels(Vector2d position){
		return new Vector2d(position.x/Agent.blockSize, position.y/Agent.blockSize);
	}
	
	public int getXDist(Vector2d agent, Vector2d obj){
		return ((int) agent.x)/Agent.blockSize - ((int) obj.x)/Agent.blockSize;
	}
	
	public int getYDist(Vector2d agent, Vector2d obj){
		return ((int) agent.y)/Agent.blockSize - ((int) obj.y)/Agent.blockSize;
	}
	
	public double[][][] getOptimalQValues() {
		return optimalQValues;
	}

	public double[][][] getRandomQValues() {
		return randomQValues;
	}

	public void setOptimalQValues(double[][][] optimalQValues) {
		this.optimalQValues = optimalQValues;
	}

	public void setRandomQValues(double[][][] randomQValues) {
		this.randomQValues = randomQValues;
	}
}