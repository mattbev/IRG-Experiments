package ramyaram;

import java.util.ArrayList;

import core.ArcadeMachine;
import core.game.Game;
import core.game.StateObservation;
import ontology.Types;
import tools.Direction;
import tools.ElapsedCpuTimer;
import tools.Utils;

/**
 * Records data as a participant plays a game
 */
public class HumanAgent extends Agent {	
	public static int lastWin = -1;
	public static double lastGameScore = -1;
    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public HumanAgent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
    	super(so, elapsedTimer);
    }  
    
    @Override
    public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller){
    	HumanAgent.gameName = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
    	ArcadeMachine.runOneGame(game, level1, visuals, controller, null, 1, 0);
    	return null;
    }

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        Direction move = Utils.processMovementActionKeys(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX);
        boolean useOn = Utils.processUseKey(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX);

        //in the keycontroller, move has preference.
        Types.ACTIONS action = Types.ACTIONS.fromVector(move);

        if(action == Types.ACTIONS.ACTION_NIL && useOn)
            action = Types.ACTIONS.ACTION_USE;
        
        double currScore = stateObs.getGameScore();
        if(action != Types.ACTIONS.ACTION_NIL){ //only save "important" actions and record the tick at which the action happened (skips recording nil actions)
        	Main.writeToFile(Main.humanDataFile, "TICK: "+stateObs.getGameTick()+"\n");
	        Main.writeToFile(Main.humanDataFile, stateObsStr(lastStateObs));
	        Main.writeToFile(Main.humanDataFile, action.name()+", "+Agent.roundDouble(currScore-lastScore)+"\n");
        }
        
    	lastStateObs = stateObs.copy();
    	lastAction = action;
        lastScore = currScore;

        return action;
    }
    
    /**
     * Save end-of-game stats
     */
    public void result(StateObservation stateObs, ElapsedCpuTimer elapsedCpuTimer) {
    	Main.writeToFile(Main.humanDataFile, "WINNER: "+stateObs.getGameWinner()+", SCORE: "+stateObs.getGameScore()+"\n**********************\n\n");
    	Main.writeToFile(Main.humanWinsFile, stateObs.getGameWinner()+",");
    	Main.writeToFile(Main.humanScoresFile, stateObs.getGameScore()+",");
    	Main.writeToFile(Main.humanTicksFile, stateObs.getGameTick()+",");
    	lastWin = stateObs.getGameWinner() == Types.WINNER.PLAYER_WINS ? 1 : 0;
    	lastGameScore = Agent.roundDouble(stateObs.getGameScore());
		
		lastScore = 0;
		lastAction = null;
		lastStateObs = null;
	}
   
    @Override
    public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	return null; //no agent controller here as the person is playing the game
    }
    
    @Override
    public void updateEachStep(StateObservation state, Types.ACTIONS action, StateObservation state2, double reward, ArrayList<Types.ACTIONS> actions){}

	@Override
	public void processStateObs(StateObservation stateObs) {}
}

