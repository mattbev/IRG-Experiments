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
		if((getXDist(agent, obj)+Agent.numCols-1) < 0 || (getYDist(agent, obj)+Agent.numRows-1) < 0){
			System.out.println("less than 0");
			System.out.println("agent "+agent+" obj "+obj);
			System.out.println("agent "+getGridCellFromPixels(agent)+" obj "+getGridCellFromPixels(obj));
			System.out.println("XDist "+getXDist(agent, obj));
			System.out.println("YDist "+getYDist(agent, obj));
			System.out.println("first index "+(getXDist(agent, obj)+Agent.numCols-1));
			System.out.println("second index "+(getYDist(agent, obj)+Agent.numRows-1));
			System.out.println("action "+action+" "+action.ordinal());
		}
		if((getXDist(agent, obj)+Agent.numCols-1) >= optimalQValues.length || (getYDist(agent, obj)+Agent.numRows-1) >= optimalQValues[0].length){
			System.out.println("greater than size");
			System.out.println("agent "+agent+" obj "+obj);
			System.out.println("agent "+getGridCellFromPixels(agent)+" obj "+getGridCellFromPixels(obj));
			System.out.println("XDist "+getXDist(agent, obj));
			System.out.println("YDist "+getYDist(agent, obj));
			System.out.println("first index "+(getXDist(agent, obj)+Agent.numCols-1));
			System.out.println("second index "+(getYDist(agent, obj)+Agent.numRows-1));
			System.out.println("action "+action+" "+action.ordinal());
		}
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