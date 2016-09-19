package ramyaram;

import core.game.Observation;

/**
 * Representation of an object in the task
 * Each object is part of an object class (objects of the same class are assumed to behave similarly)
 */
public class Object {
	public Observation obs;
	public int objectClassId;
	public int category;
	public int[] features; //each object is represented using a set of features (current features are: [x_agent-x_obj, y_agent-y_obj])
	
	public Object(Observation obs, int objectClassId, int category, int[] features) {
		this.obs = obs;
		this.objectClassId = objectClassId;
		this.category = category;
		this.features = features;
	}

	public int getFeature(int index) {
		return features[index];
	}
	
	public int setFeature(int index, int value) {
		return features[index] = value;
	}
	
	public int hashCode(){
		return 2;
	}
	
	public boolean equals(java.lang.Object obj_param){
		Object obj = (Object)obj_param;
		if(!obs.equals(obj.obs))
			return false;
		for(int i=0; i<features.length; i++){
			if(features[i] != obj.features[i])
				return false;
		}
		return true;
	}
	
	public Object clone(){
		int[] newStateValues = new int[features.length];
		for(int i=0; i<features.length; i++){
			newStateValues[i] = features[i];
		}
		return new Object(obs, objectClassId, category, newStateValues);
	}

	public String toString() {
		String str = "";
		for(int i=0; i<features.length; i++){
			str+=i+"="+features[i]+" ";
		}
		return str;
	}
}
