package ramyaram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    protected static Random rand;
    protected static Scanner scan;
    protected static int numX;
    protected static int numY;
    protected static int blockSize;
	protected static StateObservation lastStateObs;
	protected static Types.ACTIONS lastAction;
	public static double lastScore;
	protected static boolean updateQValues;
	protected static String gameName;
	
    /**
     * Constructor. It must return in 1 second maximum.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        rand = new Random();
        scan = new Scanner(System.in);
        INSTANCE = this;
        if(so != null){
        	ArrayList<Observation>[][] observationGrid = so.getObservationGrid();
    		numX = observationGrid.length;
    		numY = observationGrid[0].length;
        	blockSize = so.getBlockSize();	
        }
    }
    
    /**
     * Runs multiple episodes of the specified game and level using the given controller
     */
    public abstract Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller);
    
    /**
     * Selects the next action to take given the current state and all possible actions
     */
    public abstract Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions);
    
    /**
     * Updates the agent's learning model after each experience
     */
    public abstract void updateEachStep(StateObservation state, Types.ACTIONS action, StateObservation state2, double reward, ArrayList<Types.ACTIONS> actions);
    
    /**
     * Any processing of the state, if needed
     */
    public abstract void processStateObs(StateObservation stateObs);
    
	/**
	 * Runs one episode of the task (start state to goal state)
	 * Records stats from the game
	 */
	public double runOneEpisode(int conditionNum, int episodeNum, String game, String level1, boolean visuals, String controller){
		System.out.println("Episode "+episodeNum);
		int seed = rand.nextInt();
        double[] result = ArcadeMachine.runOneGame(game, level1, visuals, controller, null, seed, 0);
        while(result[0] == Types.WINNER.PLAYER_DISQ.key()) //don't count any episodes in which the controller was disqualified for time
        	result = ArcadeMachine.runOneGame(game, level1, visuals, controller, null, seed, 0);
        
        if(episodeNum % Constants.numEpisodesLearn == 0){
    		double[] eval = evaluate(Constants.numEpisodesEval, conditionNum, game, level1, visuals, controller, seed);
	    	double evalWins = eval[0];
	    	double evalReward = eval[1];
	    	double evalTicks = eval[2];
	    	//record game winner
	    	Constants.numWins[conditionNum][(episodeNum/Constants.numEpisodesLearn)] += evalWins;
	    	writeToFile(Constants.allNumWinsFile, evalWins+", ");
	    	//record reward
	    	Constants.reward[conditionNum][(episodeNum/Constants.numEpisodesLearn)] += evalReward; //score of the game
	    	writeToFile(Constants.allRewardFile, evalReward+", ");
	    	//record end game tick
	    	Constants.gameTick[conditionNum][(episodeNum/Constants.numEpisodesLearn)] += evalTicks; //game tick at the end of the game
	    	writeToFile(Constants.allGameTickFile, evalTicks+", ");
	    	return evalReward;
	    }
	    return result[1];
	}
    
   /**
	* Runs N episodes for evaluation of the current model
	* Returns [average number of wins, average reward in an episode, average number of ticks per episode]
	*/
	public double[] evaluate(int numEpisodes, int conditionNum, String game, String level1, boolean visuals, String controller, int seed){
		double[] eval = new double[]{0,0,0};
		Agent.updateQValues = false;
		for(int i=0; i<numEpisodes; i++){
			double[] result = ArcadeMachine.runOneGame(game, level1, visuals, controller, null, seed, 0);
			for(int j=0; j<eval.length; j++)
				eval[j] += result[j];
		}
		for(int j=0; j<eval.length; j++)
			eval[j] = eval[j] / numEpisodes;
		Agent.updateQValues = true;
		return eval;
	}
    
    /**
     * Act function. Called every game step, it must return an action in 40 ms maximum.
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
    	processStateObs(stateObs);
    	
        //get the available actions in this game and choose one
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        Types.ACTIONS action = chooseAction(stateObs, actions);
        double currScore = stateObs.getGameScore(); 
        
//      if(lastStateObs != null)
//        	System.out.print("lastStateObs\n"+stateObsStr(lastStateObs));
//		System.out.println("lastAction: "+lastAction);
//		System.out.print("stateObs\n"+stateObsStr(stateObs));
//		System.out.println("CurrentScore: "+currScore+", LastScore: "+lastScore+", ScoreChange: "+roundDouble(currScore-lastScore)+"\n"); 
      
		updateEachStep(lastStateObs, lastAction, stateObs, roundDouble(currScore-lastScore), actions);
        
		lastStateObs = stateObs.copy();
        lastAction = action;
        lastScore = currScore;

        return action;
    }
	
	public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
		double currScore = stateObservation.getGameScore();
		updateEachStep(lastStateObs, lastAction, stateObservation, roundDouble(currScore-lastScore), stateObservation.getAvailableActions());

//		System.out.println("In result");
//		if(lastStateObs != null)
//        	System.out.print("lastStateObs\n"+stateObsStr(lastStateObs));
//		System.out.println("lastAction: "+lastAction);
//		System.out.print("stateObs\n"+stateObsStr(stateObservation));
//		System.out.println("CurrentScore: "+currScore+", LastScore: "+lastScore+", ScoreChange: "+roundDouble(currScore-lastScore)+"\n");
		
		lastScore = 0;
		lastAction = null;
		lastStateObs = null;
	}
	
	/**
     * Calculates the updated Q-value with the given quantities
     */
    public double getOneQValueUpdate(double q, double reward, double maxQ){
    	return (1 - Constants.alpha) * q + Constants.alpha * (reward + Constants.gamma * maxQ);
    }
	
	/**
     * Clear any fields at the end of each run (which runs N episodes)
     */
    public void clearEachRun(){
    	lastStateObs = null;
    	lastAction = null;
    	lastScore = 0;
    }
	
	/**
     * Temporarily, hand-specify "important" objects in each game so that the agent can focus its learning on those objects
     * (Don't include background, wall, floor, etc)
     */
    //TODO: remove hard-coding
    public static List<Integer> getImportantObjects(String game){
    	switch(game){
	    	case "aliens": return Arrays.asList(3,9,6,5);
	    	case "missilecommand": return Arrays.asList(3,4,7);
	    	case "M": return Arrays.asList(3,6,8);
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
	    	case "avoidgeorge": case "A": return Arrays.asList(4,6,7,8);
	    	case "witnessprotection": return Arrays.asList(3,4,9,10,13,15,16,18,19);
	    	case "jaws": return Arrays.asList(4,5,6,8,10,11,12,13);
	    	case "waves": return Arrays.asList(3,5,6,7,9,10,11,12);
	    	case "W": return Arrays.asList(3,5,6,8,9,10);
	    	case "whackamole": return Arrays.asList(3,4,6,8,9);
	    	case "eggomania": return Arrays.asList(3,8,11);
	    	case "E": return Arrays.asList(3,7,8,11,12);
	    	case "K": return Arrays.asList(3,4,6,8,9);
	    	case "simpleGame": return Arrays.asList(4,5);
    	}
    	return null;
    }
	
	/**
     * Converts position in pixels to position in grid coordinates
     */
    public static Vector2d getGridCellFromPixels(Vector2d position){
	 	int x = position.x >= 0 ? ((int)position.x)/Agent.blockSize : 0;
    	int y = position.y >= 0 ? ((int)position.y)/Agent.blockSize : 0;
    	x = x >= Agent.numX ? Agent.numX-1 : x;
    	y = y >= Agent.numY ? Agent.numY-1 : y;
		return new Vector2d(x, y);
	}
    
    /**
     * Gets avatar grid position from the given state observation
     */
    public static Vector2d getAvatarGridPos(StateObservation stateObs){
    	return getGridCellFromPixels(stateObs.getAvatarPosition());
    }
	
	public static double roundDouble(double num){
		return Math.round(num * 100.0) / 100.0;
	}
	
	/**
	 * Print the given state observation using object itypes 
	 * Only prints one object at each grid coordinate for easy visualization but there might be multiple objects at each
	 */
	public String stateObsStr(StateObservation stateObs){
	    if(getImportantObjects(gameName) == null)
	        return "";
    	String str = " ";
    	ArrayList<Observation>[][] observationGrid = stateObs.getObservationGrid();
    	for (int c = 0; c < observationGrid.length; c++)
    		str+=c;
    	str+="\n";
    	for (int r = 0; r < observationGrid[0].length; r++) {
    		str+=r;
			for (int c = 0; c < observationGrid.length; c++) {
				boolean isObject = false;
				for(Observation obs : observationGrid[c][r]){
					if(getImportantObjects(gameName).contains(obs.itype) || obs.category == Types.TYPE_AVATAR){
						str += obs.itype;
						isObject = true;
						break;
					}
				}
				if(!isObject)
					str += "-";
			}
			str += "\n";
		}
    	return str;
    }
	
	public static void writeToFile(File file, String str){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
	     	writer.write(str);
	     	writer.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}