package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import core.game.Observation;
import core.game.StateObservation;
import core.player.*;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public abstract class Agent extends AbstractPlayer {
	public static Agent INSTANCE;
    protected Random rand;
    protected static double epsilon = 0.1;
    protected static double alpha = 0.1;
    protected static double gamma = 0.9;
    protected static int numRows;
    protected static int numCols;
    protected static int blockSize;
	protected StateObservation lastStateObs;
	protected Vector2d lastAvatarPos;
	protected double lastScore = 0;
	protected static int EPISODE_NUM = 0;
	protected boolean updateQValues = true;
	
	protected Map<Observation, Object> objectMap = new HashMap<Observation, Object>();
	protected Map<Vector2d, Object> gridObjectMap = new HashMap<Vector2d, Object>();
	protected Map<Observation, Object> objectNextStateMap = new HashMap<Observation, Object>();
	protected Map<Vector2d, Object> gridObjectNextStateMap = new HashMap<Vector2d, Object>();

    //Constructor. It must return in 1 second maximum.
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        rand = new Random();
        init(so);
        INSTANCE = this;
    }
    
    public void init(StateObservation so){
    	ArrayList<Observation>[][] observationGrid = so.getObservationGrid();
		numCols = observationGrid.length;
		numRows = observationGrid[0].length;
    	blockSize = so.getBlockSize();
    }
    
    public abstract ValueFunction[] run(int conditionNum, int numEpisodes, String game, String level1, String controller, int seed, ValueFunction[] priorValueFunctions);
    
    public abstract Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions);
    
    public abstract void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions);
	
    public void clearEachRun(){
    	EPISODE_NUM = 0;
    	lastStateObs = null;
    	lastAvatarPos = null;
    	lastScore = 0;
    }
    
    //Act function. Called every game step, it must return an action in 40 ms maximum.
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
    	processObs(stateObs, objectMap, gridObjectMap);
    	lastStateObs = stateObs.copy();
    	lastAvatarPos = stateObs.getAvatarPosition();
    	
        //Get the available actions in this game and choose one
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        Types.ACTIONS action = chooseAction(stateObs, actions);

        stateObs.advance(action);    
        processObs(stateObs, objectNextStateMap, gridObjectNextStateMap);
        double currScore = stateObs.getGameScore(); 
        
//        printStateObs(lastStateObs, gridObjectMap);
//        System.out.println(action);
//        printStateObs(stateObs, gridObjectNextStateMap);
//        System.out.println(currScore+" "+((currScore-lastScore)-0.1));
        
        updateEachStep(lastStateObs, action, stateObs, (currScore-lastScore)-0.1, actions);
        lastScore = currScore;
        EPISODE_NUM++;
        //Return the action.
        return action;
    }
    
    public double getOneQValueUpdate(double q, double reward, double maxQ){
//    	System.out.println(q+" "+reward+" "+maxQ);
    	return (1 - alpha) * q + alpha * (reward + gamma * maxQ);
    }
     
    public Vector2d getGridCellFromPixels(Vector2d position){
    	int x = position.x >=0 ? ((int)position.x)/blockSize : 0;
    	int y = position.y >=0 ? ((int)position.y)/blockSize : 0;
		return new Vector2d(x, y);
	}
    
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
				for (Observation obs : al) {
					Object o = new Object(obs, obs.itype, obs.category, new int[]{(int)obs.position.x, (int)obs.position.y});
					map.put(obs, o);
				}
			}
		}
		for(Object o : map.values())
			gridMap.put(getGridCellFromPixels(new Vector2d(o.getFeature(0), o.getFeature(1))), o);
    }
    
    public void printStateObs(StateObservation stateObs, Map<Vector2d, Object> gridMap){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
    	for (int r = 0; r < observationGrid[0].length; r++) {
			for (int c = 0; c < observationGrid.length; c++) {
				if(gridMap.containsKey(new Vector2d(c,r))){
					Object o = gridMap.get(new Vector2d(c,r));
					System.out.print(o.objectClassId);
				} else {
					System.out.print("-");
				}
			}
			System.out.println();
		}
    }
	
	public static int getNumNonZero(ValueFunction valueFunction){
		int num = 0;
		double[][][] optimalQValues = valueFunction.optimalQValues;
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