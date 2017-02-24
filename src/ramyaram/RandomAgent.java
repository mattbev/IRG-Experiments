package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;

import core.game.Observation;
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
	public Model run(int conditionNum, int numEpisodes, String game, String level1, boolean visuals, String controller, Model priorLearnedModel) {
		model = new Model();
		gameName = game.substring(game.lastIndexOf('/')+1, game.lastIndexOf('.'));
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

    public void updateEachStep(StateObservation state, HashMap<Observation, Object> stateObjMap, Types.ACTIONS action, StateObservation state2, HashMap<Observation, Object> state2ObjMap, double reward, ArrayList<Types.ACTIONS> actions){}
}