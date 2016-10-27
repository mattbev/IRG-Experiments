package ramyaram;

import java.util.ArrayList;
import java.util.Map;

import core.ArcadeMachine;
import core.game.Game;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Direction;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;

/**
 * Created by diego on 06/02/14.
 */
public class HumanAgent extends Agent {	
	
    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public HumanAgent(StateObservation so, ElapsedCpuTimer elapsedTimer) {
    	super(so, elapsedTimer);
    }  

    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        Direction move = Utils.processMovementActionKeys(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX);
        boolean useOn = Utils.processUseKey(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX);

        //In the keycontroller, move has preference.
        Types.ACTIONS action = Types.ACTIONS.fromVector(move);

        if(action == Types.ACTIONS.ACTION_NIL && useOn)
            action = Types.ACTIONS.ACTION_USE;
        
        processStateObs(stateObs, objectMap, gridObjectMap);
    	lastStateObs = stateObs.copy();
    	lastAvatarPos = stateObs.getAvatarPosition();

        stateObs.advance(action);    
        processStateObs(stateObs, objectNextStateMap, gridObjectNextStateMap);
        double currScore = stateObs.getGameScore();
        
        if(action != Types.ACTIONS.ACTION_NIL){
        	Main.writeToFile(Main.humanDataFile, "TICK: "+stateObs.getGameTick()+"\n");
	        Main.writeToFile(Main.humanDataFile, stateObsStr(lastStateObs, gridObjectMap));
	        Main.writeToFile(Main.humanDataFile, action.name()+", "+(currScore-lastScore)+"\n");
	        Main.writeToFile(Main.humanDataFile, stateObsStr(stateObs, gridObjectNextStateMap)+"\n");
        }
        if(stateObs.isGameOver())
        	Main.writeToFile(Main.humanDataFile, "WINNER: "+stateObs.getGameWinner()+", SCORE: "+stateObs.getGameScore()+"\n**********************\n\n");
        
        lastScore = currScore;
        objectMap.clear();
        objectNextStateMap.clear();
        gridObjectMap.clear();
        gridObjectNextStateMap.clear();

        return action;
    }
    
    public void processStateObs(StateObservation stateObs, Map<Observation, Object> map, Map<Vector2d, Object> gridMap){
    	super.processStateObs(stateObs, map, gridMap);
    	Vector2d avatarPos = getGridCellFromPixels(stateObs.getAvatarPosition());
    	Object agent = new Object(-1, -1, new int[]{(int)avatarPos.x, (int)avatarPos.y});
		gridMap.put(avatarPos, agent);
    }

    public void result(StateObservation stateObservation, ElapsedCpuTimer elapsedCpuTimer) {
        //System.out.println("Thanks for playing! " + stateObservation.isAvatarAlive());
    }
    
    public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, Model priorLearnedModel){
    	model = new Model(game);
    	HumanAgent.game = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
    	ArcadeMachine.runOneGame(game, level1, visuals, controller, null, seed, 0);
    	return null;
    }
   
    public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
    	return null;
    }
    
    public void updateEachStep(StateObservation stateObs, Types.ACTIONS action, StateObservation nextStateObs, double reward, ArrayList<Types.ACTIONS> actions){}
  
}
