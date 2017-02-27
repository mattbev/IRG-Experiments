package ramyaram;

import java.util.ArrayList;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;

/**
 * Random agent
 * Takes random actions every step, used as a baseline
 */
public class RandomAgent extends Agent {
	public RandomAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){
		super(so, elapsedTimer);
	}
	
	/**
	 * Runs a random agent
	 */
	@Override
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller) {
		gameName = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		for(int i=0; i<numEpisodes; i++)
			runOneEpisode(conditionNum, i, game, level1, visuals, controller);
		return null;
	}
	
	/**
	 * Epsilon-greedy approach to choosing an action
	 */
	@Override
	public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
		return actions.get(rand.nextInt(actions.size()));
	}

	@Override
    public void updateEachStep(StateObservation state, Types.ACTIONS action, StateObservation state2, double reward, ArrayList<Types.ACTIONS> actions){}

	@Override
	public void processStateObs(StateObservation stateObs) {}
}