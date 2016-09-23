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

	 public Vector2d getGridCellFromPixels(Vector2d position){
	 	int x = position.x >=0 ? ((int)position.x)/Agent.blockSize : 0;
    	int y = position.y >=0 ? ((int)position.y)/Agent.blockSize : 0;
    	x = x >= Agent.numCols ? Agent.numCols-1 : x;
    	y = y >= Agent.numRows ? Agent.numRows-1 : y;
		return new Vector2d(x, y);
	}
	
	public int getXDist(Vector2d agent, Vector2d obj){
		return (int) (getGridCellFromPixels(agent).x - getGridCellFromPixels(obj).x);
	}
	
	public int getYDist(Vector2d agent, Vector2d obj){
		return (int) (getGridCellFromPixels(agent).y - getGridCellFromPixels(obj).y);
	}

	public double[][][] getOptimalQValues() {
		return optimalQValues;
	}
}