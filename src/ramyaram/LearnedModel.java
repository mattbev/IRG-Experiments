package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Model learned after a number of episodes
 * Includes learned value functions for each object class in the given game
 * And the mapping between itype sprite ids in the game to internal object class ids
 */
public class LearnedModel {
	private ArrayList<ValueFunction> learnedValueFunctions;
	private HashMap<Integer, Integer> learnedIdMapping;
	private Condition condition;
	private String game;
	
	public LearnedModel(ArrayList<ValueFunction> learnedValueFunctions, HashMap<Integer, Integer> priorIdMapping, Condition condition, String game) {
		super();
		this.learnedValueFunctions = new ArrayList<ValueFunction>(learnedValueFunctions);
		this.learnedIdMapping = new HashMap<Integer, Integer>();
		for(int key : priorIdMapping.keySet())
			this.learnedIdMapping.put(key, priorIdMapping.get(key));
		this.condition = condition;
		this.game = game;
	}
	public ArrayList<ValueFunction> getLearnedValueFunctions() {
		return learnedValueFunctions;
	}
	public void setLearnedValueFunctions(ArrayList<ValueFunction> learnedValueFunctions) {
		this.learnedValueFunctions = learnedValueFunctions;
	}
	public HashMap<Integer, Integer> getLearnedIdMapping() {
		return learnedIdMapping;
	}
	public void setLearnedIdMapping(HashMap<Integer, Integer> learnedIdMapping) {
		this.learnedIdMapping = learnedIdMapping;
	}
	public Condition getCondition() {
		return condition;
	}
	public String getGame() {
		return game;
	}
}
