package ramyaram;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Constants {
	public enum RunType {PLAY, RUN}	
	public static int GAME_PLAY_NUM = 1;
	//structures to store information from runs
	public static double[][] reward;
	public static double[][] gameTick;
	public static double[][] numWins;
	
	//settings for current run
	public static ArrayList<String> conditions = new ArrayList<String>();
    public static RunType runType;
	public static int gameIdx = -1; //the index of the game specified
	public static int levelIdx = -1; //the level of the game specified
	public static HashMap<Integer, Integer> fixedMapping; //fixed mapping if given prior to running the task
	public static File writeModelFile = null;
	public static File readModelFile = null;
	public static boolean visuals = false;

	//parameters for standard Q-learning
	public static double epsilon = 0.1;
	public static double alpha = 0.1;
	public static double gamma = 0.95;
	
	//parameters for object-based transfer
	public static double mapping_alpha = 0.1; //the learning rate for the mapping phase
	public static double mapping_epsilon = 0.1; //the amount an agent explores (as opposed to exploit) mappings of different object classes
	public static double mapping_epsilon_end = 0.001; //the ending mapping exploration rate (after decreasing to this value, the parameter stays constant)
	public static double mapping_epsilon_delta = 0.01; //exploration over mappings decreases by this delta value over time	
	
	//parameters to denote number of episodes
	public static int numAveraging = 0;
	public static int numTotalEpisodes = 0;
	public static int numEpisodesLearn = 1;
	public static int numEpisodesEval = 0;
	public static int numEpisodesMapping = 0;
	
	//files for saving results
	public static File avgRewardFile;
	public static File allRewardFile;
	public static File avgGameTickFile;
	public static File allGameTickFile;
	public static File avgNumWinsFile;
	public static File allNumWinsFile;
	public static File runInfoFile;
	public static File humanDataFile;
	public static File humanWinsFile;
	public static File humanScoresFile;
	public static File humanTicksFile;
}
