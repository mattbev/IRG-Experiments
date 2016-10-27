package ramyaram;

import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * Abstract agent controller class with common fields and methods
 */
public abstract class Agent extends AbstractPlayer {
	public static Agent INSTANCE;
    protected static Random rand;
    protected static int numRows;
    protected static int numCols;
    protected static int blockSize;
	protected static StateObservation lastStateObs;
	protected static Vector2d lastAvatarPos;
	protected static double lastScore;
	protected static boolean updateQValues;
	protected static String game;
	
	protected static Map<Observation, Object> objectMap = new HashMap<Observation, Object>();
	protected static Map<Vector2d, Object> gridObjectMap = new HashMap<Vector2d, Object>();
	protected static Map<Observation, Object> objectNextStateMap = new HashMap<Observation, Object>();
	protected static Map<Vector2d, Object> gridObjectNextStateMap = new HashMap<Vector2d, Object>();
	protected static Model model;

    /**
     * Constructor. It must return in 1 second maximum.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        rand = new Random();
        INSTANCE = this;
        if(so != null)
        	init(so);
    }
    
    public void init(StateObservation so){
    	ArrayList<Observation>[][] observationGrid = so.getObservationGrid();
		numCols = observationGrid.length;
		numRows = observationGrid[0].length;
    	blockSize = so.getBlockSize();
    }
    
    /**
     * Runs multiple episodes of the given game and level using the given controller
     */
    public abstract Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, Model priorLearnedModel);
    
    /**
     * Action selection method
     */
    public abstract Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions);
    
    /**
     * Any updates to the agent's learning model after each experience
     */
    public abstract void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions);
	
    /**
     * Take the given stateObs and add objects to a map to keep track
     */
    public void processStateObs(StateObservation stateObs, Map<Observation, Object> map, Map<Vector2d, Object> gridMap){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
		for (int c = 0; c < observationGrid.length; c++) {
			ArrayList<Observation>[] alv = observationGrid[c];
			for (int r = 0; r < alv.length; r++) {
				ArrayList<Observation> temp = alv[r];
				ArrayList<Observation> al = new ArrayList<Observation>();
				for (Observation obs : temp) {
					if(getImportantObjects(game) != null){
						if(getImportantObjects(game).contains(obs.itype))
							al.add(obs);
					} 
				}
				for (Observation obs : al)
					processObs(obs, map);
			}
		}
		for(Object o : map.values())
			gridMap.put(getGridCellFromPixels(new Vector2d(o.getFeature(0), o.getFeature(1))), o);
    }
    
    /**
     * Temporarily, hand-specify "important" objects in each game so that the agent can focus its learning on those objects
     * (Don't include background, wall, floor, etc)
     */
    //TODO: Remove hard-coding
    public List<Integer> getImportantObjects(String game){
    	switch(game){
	    	case "aliens": return Arrays.asList(3,9,6,5);
	    	case "missilecommand": return Arrays.asList(3,4,7);
	    	case "sheriff": return Arrays.asList(3,5,12,13,14,15,16);
	    	case "sheriffTopBottom": return Arrays.asList(3,5,14,15,16);
	    	case "solarfox": return Arrays.asList(8,9,6,11,12);
	    	case "solarfoxShoot": return Arrays.asList(4,7,9,10,12,13);
	    	case "solarfoxShootGem": return Arrays.asList(4,7,9,10,12,13);
    	}
    	return null;
    }
    
    /**
     * Process a single observation
     */
    public void processObs(Observation obs, Map<Observation, Object> map){
    	if(!model.getItype_to_objClassId().containsKey(obs.itype))
    		model.getItype_to_objClassId().put(obs.itype, model.getItype_to_objClassId().size());
		Object o = new Object(model.getItype_to_objClassId().get(obs.itype), obs.itype, new int[]{(int)obs.position.x, (int)obs.position.y});
		map.put(obs, o);
    }
    
    /**
     * Clear any fields at the end of each run (which runs N episodes)
     */
    public void clearEachRun(){
    	lastStateObs = null;
    	lastAvatarPos = null;
    	lastScore = 0;
    	if(model != null)
    		model.clear();
    }
    
    /**
     * Act function. Called every game step, it must return an action in 40 ms maximum.
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
    	processStateObs(stateObs, objectMap, gridObjectMap);
    	lastStateObs = stateObs.copy();
    	lastAvatarPos = stateObs.getAvatarPosition();
    	
        //Get the available actions in this game and choose one
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        Types.ACTIONS action = chooseAction(stateObs, actions);

        stateObs.advance(action);    
        processStateObs(stateObs, objectNextStateMap, gridObjectNextStateMap);
        double currScore = stateObs.getGameScore(); 
        
//		System.out.println("QValueFunctions size "+model.qValueFunctions.size());
//		System.out.println(stateObsStr(lastStateObs, gridObjectMap));
//		System.out.println(action);
//		System.out.println(stateObsStr(stateObs, gridObjectNextStateMap));
//		System.out.println(currScore+" "+(currScore-lastScore));
        
        updateEachStep(lastStateObs, action, stateObs, (currScore-lastScore), actions);
        lastScore = currScore;
        
        objectMap.clear();
        objectNextStateMap.clear();
        gridObjectMap.clear();
        gridObjectNextStateMap.clear();

        return action;
    }
    
    /**
     * Calculates the updated qValue given the given quantities
     */
    public double getOneQValueUpdate(double q, double reward, double maxQ){
    	return (1 - Main.alpha) * q + Main.alpha * (reward + Main.gamma * maxQ);
    }
    
    /**
     * Convert position in pixels to grid coordinates
     */
    public static Vector2d getGridCellFromPixels(Vector2d position){
	 	int x = position.x >=0 ? ((int)position.x)/Agent.blockSize : 0;
    	int y = position.y >=0 ? ((int)position.y)/Agent.blockSize : 0;
    	x = x >= Agent.numCols ? Agent.numCols-1 : x;
    	y = y >= Agent.numRows ? Agent.numRows-1 : y;
		return new Vector2d(x, y);
	}
    
    public String stateObsStr(StateObservation stateObs, Map<Vector2d, Object> gridMap){
    	String str = "";
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
    	for (int r = 0; r < observationGrid[0].length; r++) {
			for (int c = 0; c < observationGrid.length; c++) {
				if(gridMap.containsKey(new Vector2d(c,r))){
					Object o = gridMap.get(new Vector2d(c,r));
					str += o.getItype();
				} else {
					str += "-";
				}
			}
			str += "\n";
		}
    	return str;
    }
    
    public static int getXDist(Vector2d agent, Vector2d obj){
		return (int) (getGridCellFromPixels(agent).x - getGridCellFromPixels(obj).x);
	}
	
	public static int getYDist(Vector2d agent, Vector2d obj){
		return (int) (getGridCellFromPixels(agent).y - getGridCellFromPixels(obj).y);
	}
}