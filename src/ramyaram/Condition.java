package ramyaram;
/**
 * Specifies all the conditions that are supported by this code
 */
public enum Condition {
	OF_Q, //this condition runs Object-Focused Q-learning (which learns a set of N tabular value functions, one for each object class in the new task)
	OBT, //this condition runs Object-Based Transfer (which transfers previously learned object-based value functions for a new task)
}
