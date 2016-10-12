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
    protected static Random rand;
    protected static double epsilon = 0.1;
    protected static double alpha = 0.1;
    protected static double gamma = 0.9;
    protected static int numRows;
    protected static int numCols;
    protected static int blockSize;
	protected static StateObservation lastStateObs;
	protected static Vector2d lastAvatarPos;
	protected static double lastScore = 0;
	protected static boolean updateQValues = true;
	protected static String game = "";
	
	protected static Map<Observation, Object> objectMap = new HashMap<Observation, Object>();
	protected static Map<Vector2d, Object> gridObjectMap = new HashMap<Vector2d, Object>();
	protected static Map<Observation, Object> objectNextStateMap = new HashMap<Observation, Object>();
	protected static Map<Vector2d, Object> gridObjectNextStateMap = new HashMap<Vector2d, Object>();
	protected static HashMap<Integer, Integer> itype_to_objClassId = new HashMap<Integer, Integer>();

    //Constructor. It must return in 1 second maximum.
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
    
    public abstract LearnedModel run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, LearnedModel priorLearnedModel);
    
    public abstract Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions);
    
    public abstract void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions);
	
    public void processStateObs(StateObservation stateObs, Map<Observation, Object> map, Map<Vector2d, Object> gridMap){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
		for (int c = 0; c < observationGrid.length; c++) {
			ArrayList<Observation>[] alv = observationGrid[c];
			for (int r = 0; r < alv.length; r++) {
				ArrayList<Observation> temp = alv[r];
				ArrayList<Observation> al = new ArrayList<Observation>();
				for (Observation obs : temp) {
					//TODO: remove hardcoding for specific games (currently this is used to focus only on relevant objects)
					if(game.equals("ramyaFreeway")) {
						if(obs.itype == 7 || obs.itype == 10 || obs.category == Types.TYPE_AVATAR)
							al.add(obs);
					} else if(game.equals("ramyaNormandy")) {
						if(obs.itype == 7 || obs.itype == 3 || obs.category == Types.TYPE_AVATAR)
							al.add(obs);	
					} else {
						al.add(obs);
					}
				}
				for (Observation obs : al){
					processObs(obs, map);
				}
			}
		}
		for(Object o : map.values())
			gridMap.put(getGridCellFromPixels(new Vector2d(o.getFeature(0), o.getFeature(1))), o);
    }
    
    public void processObs(Observation obs, Map<Observation, Object> map){
    	if(!itype_to_objClassId.containsKey(obs.itype))
			itype_to_objClassId.put(obs.itype, itype_to_objClassId.size());
		Object o = new Object(itype_to_objClassId.get(obs.itype), obs.itype, new int[]{(int)obs.position.x, (int)obs.position.y});
		map.put(obs, o);
//		for(int index : itype_to_objClassId.keySet())
//		System.out.println("itype "+index+" --> "+itype_to_objClassId.get(index));
    }
    
    public void clearEachRun(){
    	lastStateObs = null;
    	lastAvatarPos = null;
    	lastScore = 0;
    	itype_to_objClassId.clear();
    }
    
    //Act function. Called every game step, it must return an action in 40 ms maximum.
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
        
//		  System.out.println("qValueFunctions size "+((OFQAgent)this).qValueFunctions.size());
//        printStateObs(lastStateObs, gridObjectMap);
//        System.out.println(action);
//        printStateObs(stateObs, gridObjectNextStateMap);
//        System.out.println(currScore+" "+((currScore-lastScore)-0.1));
        
        updateEachStep(lastStateObs, action, stateObs, (currScore-lastScore)-0.1, actions);
        lastScore = currScore;
        //Return the action.
        return action;
    }
    
    public double getOneQValueUpdate(double q, double reward, double maxQ){
//    	System.out.println(q+" "+reward+" "+maxQ);
    	return (1 - alpha) * q + alpha * (reward + gamma * maxQ);
    }
    
    public static Vector2d getGridCellFromPixels(Vector2d position){
	 	int x = position.x >=0 ? ((int)position.x)/Agent.blockSize : 0;
    	int y = position.y >=0 ? ((int)position.y)/Agent.blockSize : 0;
    	x = x >= Agent.numCols ? Agent.numCols-1 : x;
    	y = y >= Agent.numRows ? Agent.numRows-1 : y;
		return new Vector2d(x, y);
	}
    
    public void printStateObs(StateObservation stateObs, Map<Vector2d, Object> gridMap){
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
    	for (int r = 0; r < observationGrid[0].length; r++) {
			for (int c = 0; c < observationGrid.length; c++) {
				if(gridMap.containsKey(new Vector2d(c,r))){
					Object o = gridMap.get(new Vector2d(c,r));
//					System.out.print(o.getObjClassId());
					System.out.print(o.getItype());
				} else {
					System.out.print("-");
				}
			}
			System.out.println();
		}
    }
}