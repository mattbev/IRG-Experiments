package ramyaram;
/**
 * Specifies all the conditions that are supported by this code
 */
public enum Condition {
	Q_LEARNING, //this condition runs standard Q-learning (learns one tabular value function over all states and actions)
	OF_Q, //this condition runs Object-Focused Q-learning (learns a set of N tabular value functions, one for each object class in the new task)
}
