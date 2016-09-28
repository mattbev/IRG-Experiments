package ramyaram;
/**
 * Specifies all the conditions that are supported by this code
 */
public enum Condition {
	OF_Q_SOURCE, //this condition runs Object-Focused Q-learning (which learns a set of N tabular value functions, one for each object class) on the source task
	OF_Q_TARGET, //this condition runs Object-Focused Q-learning on the target task with no source task knowledge
	OBT_TARGET, //this condition runs Object-Based Transfer on the target task (which transfers previously learned object-based value functions from the source task)
}
