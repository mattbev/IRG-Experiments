package ramyaram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import core.ArcadeMachine;
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
	public static boolean endedGame;
    protected static Random rand;
    protected static Scanner scan;
    protected static int numRows;
    protected static int numCols;
    protected static int blockSize;
	protected static StateObservation lastStateObs;
	protected static double lastScore;
	protected static boolean updateQValues;
	protected static String gameName;
	
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
        scan = new Scanner(System.in);
        INSTANCE = this;
        if(so != null){
        	ArrayList<Observation>[][] observationGrid = so.getObservationGrid();
    		numCols = observationGrid.length;
    		numRows = observationGrid[0].length;
        	blockSize = so.getBlockSize();	
        }
    }
    
    /**
     * Runs multiple episodes of the specified game and level using the given controller
     */
    public abstract Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, Model priorLearnedModel);
    
    /**
     * Selects the next action to take given the current state and all possible actions
     */
    public abstract Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions);
    
    /**
     * Updates the agent's learning model after each experience
     */
    public abstract void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions);
	
	/**
	 * Runs one episode of the task (start state to goal state)
	 * Records stats from the game
	 */
	public double runOneEpisode(int conditionNum, int episodeNum, String game, String level1, boolean visuals, String controller, int seed){
		System.out.println("Episode "+episodeNum);
		lastScore = 0;
		lastStateObs = null;
		endedGame = false;
        double[] result = ArcadeMachine.runOneGame(game, level1, visuals, controller, null, seed, 0);
        while(result[0] == Types.WINNER.PLAYER_DISQ.key()) //don't count any episodes in which the controller was disqualified for time
//        	System.out.println("DQ!");
        	result = ArcadeMachine.runOneGame(game, level1, visuals, controller, null, seed, 0);
        if(episodeNum % Main.interval == 0){
        	//record reward
        	Main.reward[conditionNum][(episodeNum/Main.interval)] += result[1]; //score of the game
        	Main.writeToFile(Main.allRewardFile, result[1]+", ");
        	//record game winner
        	int winIndex = (result[0] == Types.WINNER.PLAYER_WINS.key()) ? 1 : 0;
    		Main.numWins[conditionNum][(episodeNum/Main.interval)] += winIndex;
        	Main.writeToFile(Main.allNumWinsFile, winIndex+", ");
        	//record end game tick
        	Main.gameTick[conditionNum][(episodeNum/Main.interval)] += result[2]; //game tick at the end of the game
        	Main.writeToFile(Main.allGameTickFile, result[2]+", ");      	
        }
        return result[1];
	}
    
    /**
     * Converts the given state observation into a map to keep track of objects in the current state
     */
    public void processStateObs(StateObservation stateObs, Map<Observation, Object> map, Map<Vector2d, Object> gridMap){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
		for (int i = 0; i < observationGrid.length; i++) {
			for (int j = 0; j < observationGrid[i].length; j++) {
				ArrayList<Observation> obsList = new ArrayList<Observation>();
				for (Observation obs : observationGrid[i][j]) {
//					if(obs.category == Types.TYPE_AVATAR) //uncomment if you want to see the avatar in the console, along with the important objects
//						obsList.add(obs);
					if(getImportantObjects(gameName) != null){
						if(getImportantObjects(gameName).contains(obs.itype))
							obsList.add(obs);
					} else {
						if(!Arrays.asList().contains(obs.itype))
							obsList.add(obs);
					}
				}
				for(Observation obs : obsList)
					processObs(obs, map);
			}
		}
		for(Object o : map.values())
			gridMap.put(o.getGridPos(), o);
    }
    
    /**
     * Temporarily, hand-specify "important" objects in each game so that the agent can focus its learning on those objects
     * (Don't include background, wall, floor, etc)
     */
    //TODO: remove hard-coding
    public List<Integer> getImportantObjects(String game){
    	switch(game){
	    	case "aliens": return Arrays.asList(3,9,6,5);
	    	case "missilecommand": return Arrays.asList(3,4,7);
	    	case "sheriff": case "H": return Arrays.asList(3,5,12,13,14,15,16);
	    	case "sheriffTopBottom": return Arrays.asList(3,5,14,15,16);
	    	case "solarfox": case "S": return Arrays.asList(8,9,6,11,12);
	    	case "solarfoxShoot": return Arrays.asList(4,7,9,10,12,13);
	    	case "solarfoxShootGem": return Arrays.asList(4,7,9,10,12,13);
	    	case "butterflies": return Arrays.asList(3,5);
	    	case "firestorms": case "F": return Arrays.asList(3,4,5,6);
	    	case "firecaster": return Arrays.asList(4,5,6,8);
	    	case "crossfire": return Arrays.asList(3,4,6);
	    	case "plaqueattack": case "P": return Arrays.asList(4,7,11,14,16);
	    	case "defender": case "D": return Arrays.asList(3,5,6,7,12);
	    	case "avoidgeorge": return Arrays.asList(6,7,8);
	    	case "witnessprotection": return Arrays.asList(3,4,9,10,13,15,16,18,19);
	    	case "jaws": return Arrays.asList(4,5,6,8,10,11,12,13);
	    	case "waves": return Arrays.asList(3,5,6,7,9,10,11,12);
	    	case "W": return Arrays.asList(3,5,6,8,9,10);
    	}
    	return null;
    }
    
    /**
     * Process a single observation
     */
    public void processObs(Observation obs, Map<Observation, Object> map){
    	if(!model.getItype_to_objClassId().containsKey(obs.itype))
    		model.getItype_to_objClassId().put(obs.itype, model.getItype_to_objClassId().size());
    	Vector2d gridPos = getGridCellFromPixels(obs.position);
		Object o = new Object(model.getItype_to_objClassId().get(obs.itype), obs.itype, gridPos);
		map.put(obs, o);
    }
    
    /**
     * Clear any fields at the end of each run (which runs N episodes)
     */
    public void clearEachRun(){
    	lastStateObs = null;
    	lastScore = 0;
    	endedGame = false;
    	if(model != null)
    		model.clear();
    }
    
    /**
     * Act function. Called every game step, it must return an action in 40 ms maximum.
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
    	processStateObs(stateObs, objectMap, gridObjectMap);
    	lastStateObs = stateObs.copy();
    	
        //get the available actions in this game and choose one
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        Types.ACTIONS action = chooseAction(stateObs, actions);

        stateObs.advance(action);    
        processStateObs(stateObs, objectNextStateMap, gridObjectNextStateMap);
        double currScore = stateObs.getGameScore(); 
        
//		System.out.println("QValueFunctions size "+model.qValueFunctions.size());
//		System.out.print(stateObsStr(lastStateObs, gridObjectMap));
////		for(Observation obs : objectMap.keySet())
////        	System.out.println(objectMap.get(obs).getGridPos()+" "+objectMap.get(obs).getItype());
//		System.out.println(action);
//		System.out.print(stateObsStr(stateObs, gridObjectNextStateMap));
////		for(Observation obs : objectNextStateMap.keySet())
////        	System.out.println(objectNextStateMap.get(obs).getGridPos()+" "+objectNextStateMap.get(obs).getItype());
//		System.out.println("Current Game Score: "+currScore+", Score Change: "+(currScore-lastScore)+"\n");
        
        updateEachStep(lastStateObs, action, stateObs, (currScore-lastScore), actions);
        lastScore = currScore;
        
        objectMap.clear();
        objectNextStateMap.clear();
        gridObjectMap.clear();
        gridObjectNextStateMap.clear();

        return action;
    }
    
    /**
     * Calculates the updated Q-value with the given quantities
     */
    public double getOneQValueUpdate(double q, double reward, double maxQ){
    	return (1 - Main.alpha) * q + Main.alpha * (reward + Main.gamma * maxQ);
    }
    
	/**
	 * Print the given state observation using object itypes 
	 * Only prints one object at each grid coordinate for easy visualization but there might be multiple objects at each
	 */
	public String stateObsStr(StateObservation stateObs, Map<Vector2d, Object> gridMap){
    	String str = " ";
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
    	for (int c = 0; c < observationGrid.length; c++)
    		str+=c;
    	str+="\n";
    	for (int r = 0; r < observationGrid[0].length; r++) {
    		str+=r;
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
	
	/**
     * Converts position in pixels to position in grid coordinates
     */
    public static Vector2d getGridCellFromPixels(Vector2d position){
	 	int x = position.x >= 0 ? ((int)position.x)/Agent.blockSize : 0;
    	int y = position.y >= 0 ? ((int)position.y)/Agent.blockSize : 0;
    	x = x >= Agent.numCols ? Agent.numCols-1 : x;
    	y = y >= Agent.numRows ? Agent.numRows-1 : y;
		return new Vector2d(x, y);
	}
    
    /**
     * Gets avatar grid position from the given state observation
     */
    public static Vector2d getAvatarGridPos(StateObservation stateObs){
    	return getGridCellFromPixels(stateObs.getAvatarPosition());
    }
    
    /**
     * Gets X distance between agent and object
     */
    public static int getXDist(Vector2d agent, Vector2d obj){
		return (int)agent.x - (int)obj.x;
	}
	
    /**
     * Gets Y distance between agent and object
     */
	public static int getYDist(Vector2d agent, Vector2d obj){
		return (int)agent.y - (int)obj.y;
	}
}