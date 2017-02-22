package ramyaram;

import java.util.ArrayList;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;
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
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, Model priorLearnedModel) {
		model = new Model();
		RandomAgent.gameName = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		//run Random agent
		for(int i=0; i<numEpisodes; i++)
			runOneEpisode(conditionNum, i, game, level1, visuals, controller);
		return model;
	}
	
	/**
	 * Epsilon-greedy approach to choosing an action
	 */
	public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
		return actions.get(rand.nextInt(actions.size()));
	}

	public void updateEachStep(StateObservation stateObs, ACTIONS action, StateObservation nextStateObs, double reward,
			ArrayList<ACTIONS> actions) {		
	}
}