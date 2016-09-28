package ramyaram;

import java.util.ArrayList;
import java.util.HashMap;

public class LearnedModel {
	private ArrayList<ValueFunction> learnedValueFunctions;
	private HashMap<Integer, Integer> learnedIdMapping;
	public LearnedModel(ArrayList<ValueFunction> learnedValueFunctions, HashMap<Integer, Integer> learnedIdMapping) {
		super();
		this.learnedValueFunctions = new ArrayList<ValueFunction>(learnedValueFunctions);
		this.learnedIdMapping = new HashMap<Integer, Integer>(learnedIdMapping);
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
}
