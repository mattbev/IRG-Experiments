package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;

public class LearnedModel {
	private ArrayList<ValueFunction> learnedValueFunctions;
	private HashMap<Integer, Integer> learnedIdMapping;
	private Condition condition;
	
	public LearnedModel(ArrayList<ValueFunction> learnedValueFunctions, HashMap<Integer, Integer> priorIdMapping, Condition condition) {
		super();
		this.learnedValueFunctions = new ArrayList<ValueFunction>(learnedValueFunctions);
		this.learnedIdMapping = new HashMap<Integer, Integer>();
		for(int key : priorIdMapping.keySet())
			this.learnedIdMapping.put(key, priorIdMapping.get(key));
		this.condition = condition;
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
}
