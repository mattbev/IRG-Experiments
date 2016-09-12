package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import core.player.*;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class Agent extends AbstractPlayer {
    protected Random rand;
    private static double epsilon = 0.1;
    private static double alpha = 0.1;
    private static double gamma = 0.9;
	private static int numObjClasses = 13;
	public static int numRows;
	public static int numCols;
	public static int blockSize;
	private Map<Observation, Object> objectMap = new HashMap<Observation, Object>();
	private Map<Observation, Object> objectNextStateMap = new HashMap<Observation, Object>();
	private static ValueFunction[] qValueFunctions;
	private StateObservation lastStateObs;
	private Vector2d lastAvatarPos;
	private double lastScore = 0;

    //Constructor. It must return in 1 second maximum.
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        rand = new Random();
        init(so);
    }
    
    public static int getNumNonZero(){
		int num = 0;
		double[][][] optimalQValues = qValueFunctions[12].optimalQValues;
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
    
    public void init(StateObservation so){
    	ArrayList<Observation>[][] observationGrid = so.getObservationGrid();
		numCols = observationGrid.length;
		numRows = observationGrid[0].length;
		if(qValueFunctions == null){
	        System.out.println("init");
			qValueFunctions = new ValueFunction[numObjClasses];
	    	for(int i=0; i<qValueFunctions.length; i++)
				qValueFunctions[i] = new ValueFunction(null, null);
		}
    	blockSize = so.getBlockSize();
    }

    //Act function. Called every game step, it must return an action in 40 ms maximum.
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) { 
    	processObs(stateObs, objectMap);
    	lastStateObs = stateObs.copy();
    	lastAvatarPos = stateObs.getAvatarPosition();
    	
        //Get the available actions in this game.
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        
        Types.ACTIONS action = egreedy(stateObs, actions);
        
        stateObs.advance(action);     
        processObs(stateObs, objectNextStateMap);

        double currScore = stateObs.getGameScore();  
        updateQValues(lastStateObs, action, stateObs, (currScore-lastScore)-1, actions);
        lastScore = currScore;
                
        //Return the action.
        return action;
    }
    
    public void processObs(StateObservation stateObs, Map<Observation, Object> map){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
		for (int c = 0; c < observationGrid.length; c++) {
			ArrayList<Observation>[] alv = observationGrid[c];
			for (int r = 0; r < alv.length; r++) {
				ArrayList<Observation> al = alv[r];
				for (Observation obs : al) {
					Object o = new Object(obs, obs.itype, new int[]{(int)obs.position.x, (int)obs.position.y});
					map.put(obs, o);
				}
			}
		}
    }
    
    public Vector2d getGridCellFromPixels(Vector2d position){
		return new Vector2d(position.x/blockSize, position.y/blockSize);
	}
    
    public Types.ACTIONS egreedy(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	if(rand.nextDouble() < epsilon){ //choose a random action
    		int index = rand.nextInt(actions.size());
            Types.ACTIONS action = actions.get(index);
            return action;
    	} else { //choose greedy action based on value function
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
    }
    
    /**
	 * Updates the current Q-values for this particular state, action, and next state
	 */
    public void updateQValues(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions){		
    	for(Observation obs : objectMap.keySet()) {
    		Object currObj = objectMap.get(obs);
    		Object nextObj = objectNextStateMap.get(obs);
    		ValueFunction qValues = qValueFunctions[currObj.objectClassId];
			double q = qValues.getOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action);
			double maxQ = 0;
			if(nextObj != null)
				maxQ = optimalMaxQ(qValues, nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), actions);	
	        double qValue = (1 - alpha) * q + alpha * (reward + gamma * maxQ);
	        qValues.setOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action, qValue);
    	}
    }
    
    /**
	 * Computes the maximum Q-value over the optimal value function for this state
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