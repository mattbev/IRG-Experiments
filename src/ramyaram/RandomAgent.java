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
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, int seed, Model priorLearnedModel) {
		model = new Model();
		RandomAgent.gameName = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
		//show agent play the game before learning
		for(int i=0; i<Main.visuals; i++) 
			runOneEpisode(conditionNum, 0, game, level1, true, controller, seed);
		//run Random agent
		for(int i=0; i<numEpisodes; i++)
			runOneEpisode(conditionNum, i, game, level1, visuals, controller, seed);
		//show agent play the game after learning
		for(int i=0; i<Main.visuals; i++)
			runOneEpisode(conditionNum, numEpisodes-1, game, level1, true, controller, seed);
		return model;
	}
	
	/**
	 * Epsilon-greedy approach to choosing an action
	 */
	public Types.ACTIONS chooseAction(StateObservation stateObs, ArrayList<Types.ACTIONS> actions){
		System.out.println("random action");
		return actions.get(rand.nextInt(actions.size()));
	}

	public void updateEachStep(StateObservation stateObs, ACTIONS action, StateObservation nextStateObs, double reward,
			ArrayList<ACTIONS> actions) {		
	}
}