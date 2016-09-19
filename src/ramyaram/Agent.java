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
	private static ValueFunction[] qValueFunctions;

	private Map<Observation, Object> objectMap = new HashMap<Observation, Object>();
	private Map<Vector2d, Object> gridObjectMap = new HashMap<Vector2d, Object>();
	private Map<Observation, Object> objectNextStateMap = new HashMap<Observation, Object>();
	private Map<Vector2d, Object> gridObjectNextStateMap = new HashMap<Vector2d, Object>();
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
    
    public static void clear(){
    	qValueFunctions = null;
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
//    	System.out.println(stateObs.getObservationGrid().length+" "+stateObs.getObservationGrid()[0].length);
    	processObs(stateObs, objectMap, gridObjectMap);
//    	System.out.println(gridObjectMap.size());
//    	System.out.println(new Vector2d(0,2).equals(new Vector2d(0,2)));
//    	Vector2d v = new Vector2d(0,2);
//    	gridObjectMap.put(v,null);
//    	for(Vector2d key : gridObjectMap.keySet())
//    		System.out.println(key);
//    	System.out.println(gridObjectMap.containsKey(new Vector2d(0,2)));
//    	printStateObs(stateObs, gridObjectMap);
//    	System.out.println(gridObjectMap.size());
    	lastStateObs = stateObs.copy();
    	lastAvatarPos = stateObs.getAvatarPosition();
    	
        //Get the available actions in this game and choose one
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        Types.ACTIONS action = egreedy(stateObs, actions);

        stateObs.advance(action);    
        processObs(stateObs, objectNextStateMap, gridObjectNextStateMap);
        double currScore = stateObs.getGameScore(); 
        
//    	printStateObs(lastStateObs, gridObjectMap);
//        System.out.println(action);
//        printStateObs(stateObs, gridObjectNextStateMap);
//        System.out.println(currScore+" "+((currScore-lastScore)-0.1));
        
        updateQValues(lastStateObs, action, stateObs, (currScore-lastScore)-0.1, actions);
        lastScore = currScore;
        objectMap.clear();
        objectNextStateMap.clear();
        gridObjectMap.clear();
        gridObjectNextStateMap.clear();
        //Return the action.
        return action;
    }
    
    public void printStateObs(StateObservation stateObs, Map<Vector2d, Object> gridMap){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
    	for (int r = 0; r < observationGrid[0].length; r++) {
//			ArrayList<Observation>[] alv = observationGrid[c];
			for (int c = 0; c < observationGrid.length; c++) {
				if(gridMap.containsKey(new Vector2d(c,r))){
					Object o = gridMap.get(new Vector2d(c,r));
//					System.out.print((char) ((int)'-'+3+o.objectClassId));
					System.out.print(o.objectClassId);
				} else {
					System.out.print("-");
				}
			}
			System.out.println();
		}
//		for (int c = 0; c < observationGrid.length; c++) {
//			ArrayList<Observation>[] alv = observationGrid[c];
//			for (int r = 0; r < alv.length; r++) {
//				if(gridMap.containsKey(new Vector2d(c,r))){
//					Object o = gridMap.get(new Vector2d(c,r));
////					System.out.print((char) ((int)'-'+3+o.objectClassId));
//					System.out.print(o.objectClassId);
//				} else {
//					System.out.print("-");
//				}
//			}
//			System.out.println();
//		}
    }
//    	for (Object obj: objectMap.values()) {
//	    	if(al.size() == 0)
//				System.out.print("-");
//			else if(al.size() == 1)
//				System.out.print((char) ((int)'-'+3+al.get(0).itype));
//			else{
//	//			System.out.println("*");
//				for (Observation obs : al)
//					System.out.print((char) ((int)'-'+3+obs.itype));
//			}
//		}
//    }
    
    public void processObs(StateObservation stateObs, Map<Observation, Object> map, Map<Vector2d, Object> gridMap){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
		for (int c = 0; c < observationGrid.length; c++) {
			ArrayList<Observation>[] alv = observationGrid[c];
			for (int r = 0; r < alv.length; r++) {
				ArrayList<Observation> temp = alv[r];
				ArrayList<Observation> al = new ArrayList<Observation>();
				for (Observation obs : temp) {
					if(obs.itype != 2 && obs.itype != 11)
						al.add(obs);
				}
//				if(al.size() == 0)
//					System.out.print("-");
//				else if(al.size() == 1)
//					System.out.print((char) ((int)'-'+3+al.get(0).itype));
//				else
//					System.out.print("*");
				for (Observation obs : al) {
//					System.out.println(obs.itype+" "+obs.category);
//					if(!objectMap.containsKey(obs)){
//					System.out.println(obs.position.x+" "+obs.position.y);
//					System.out.println((int)obs.position.x+" "+(int)obs.position.y);
//					if(obs.position.x != (int) obs.position.x || obs.position.y != (int) obs.position.y){
//						System.out.println(obs.position.x+" "+obs.position.y);
//						System.out.println((int)obs.position.x+" "+(int)obs.position.y);
//					}
						Object o = new Object(obs, obs.itype, obs.category, new int[]{(int)obs.position.x, (int)obs.position.y});
						map.put(obs, o);
//						if(al.size() == 0)
//							System.out.print("-");
//						else if(al.size() == 1)
//							System.out.print((char) ((int)'-'+3+al.get(0).itype));
//						else{
//							System.out.println("*");
//					}
				}
			}
//			System.out.println();
		}
		
		for(Object o : map.values()){
			gridMap.put(getGridCellFromPixels(new Vector2d(o.getFeature(0), o.getFeature(1))), o);
//			System.out.println("put "+getGridCellFromPixels(new Vector2d(o.getFeature(0), o.getFeature(1))));
		}
    }
    
    public Vector2d getGridCellFromPixels(Vector2d position){
//    	if(position.x % 32 != 0 || position.y % 32 != 0){
//	    	System.out.println(position.x+" "+position.y);
//	    	System.out.println(position.x/blockSize+" "+position.y/blockSize);
//	    	System.out.println(((int)position.x)/blockSize+" "+((int)position.y)/blockSize);
//    	}
		return new Vector2d(((int)position.x)/blockSize, ((int)position.y)/blockSize);
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
//    		System.out.println("obj class id "+currObj.objectClassId);
    		ValueFunction qValues = qValueFunctions[currObj.objectClassId];
			double q = qValues.getOptimalQValue(lastAvatarPos, new Vector2d(currObj.getFeature(0), currObj.getFeature(1)), action);
			double maxQ = 0;
			if(nextObj != null)
				maxQ = optimalMaxQ(qValues, nextStateObs.getAvatarPosition(), new Vector2d(nextObj.getFeature(0), nextObj.getFeature(1)), actions);	
	        double qValue = (1 - alpha) * q + alpha * (reward + gamma * maxQ);
//	        System.out.println(obs.itype+", "+getGridCellFromPixels(obs.position)+" --> "+qValues);
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