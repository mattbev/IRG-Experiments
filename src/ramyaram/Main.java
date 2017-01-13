package ramyaram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Random;

import core.competition.CompetitionParameters;

/**
 * Main method that runs simulations of agents playing various games and analyzes the data
 */
public class Main {
	public enum RunType {PLAY, RUN}	
	public static HashMap<String, Integer> conditions = new HashMap<String,Integer>();
	//structures to store information from runs
	public static double[][] reward;
	public static double[][] gameTick;
	public static double[][] numWins;
	public static Model[] learnedModels;
	public static boolean writeModelToFile = false;
	public static boolean readModelFromFile = false;
	public static int visuals = 0;
	public static int GAME_PLAY_NUM = 1;
    //parameters to denote number of episodes
	public static int numAveraging = 0;
	public static int numSourceEpisodes = 0;
	public static int numTargetEpisodes = 0;
	public static int interval = 1;
	public static int numEpisodesMapping = -1;
	//parameters for standard Q-learning
	public static double epsilon = 0.1;
	public static double alpha = 0.1;
	public static double gamma = 0.95;	
	//parameters for object-based transfer
	public static double mapping_alpha = 0.1; //the learning rate for the mapping phase
	public static double mapping_epsilon = 0.1; //the amount an agent explores (as opposed to exploit) mappings of different object classes
	public static double mapping_epsilon_end = 0.001; //the ending mapping exploration rate (after decreasing to this value, the parameter stays constant)
	public static double mapping_epsilon_delta = 0.01; //exploration over mappings decreases by this delta value over time	
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
	public static File writeModelFile;
	public static File readModelFile;
	//settings for current run
    public static RunType runType;
	public static int[] sourceGame = new int[]{-1,-1}; //this array has 2 indices, the first specifies the game index and the second is the level index
	public static int[] targetGame = new int[]{-1,-1}; //this array has 2 indices, the first specifies the game index and the second is the level index
	public static HashMap<Integer, Integer> fixedMapping; //fixed mapping if given prior to running the task
	//path and names for all games
	public static String gamesPath = "../examples/gridphysics/";
	public static String games[] = new String[]
    		{"aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", 			  //0-4
            "blacksmoke", "boloadventures", "bomber", "boulderchase", "boulderdash",      //5-9
            "brainman", "butterflies", "cakybaky", "camelRace", "catapults",              //10-14
            "chainreaction", "chase", "chipschallenge", "clusters", "colourescape",       //15-19
            "chopper", "cookmepasta", "cops", "crossfire", "defem",                       //20-24
            "defender", "digdug", "dungeon", "eggomania", "enemycitadel",                 //25-29
            "escape", "factorymanager", "firecaster",  "fireman", "firestorms",           //30-34
            "freeway", "frogs", "gymkhana", "hungrybirds", "iceandfire",                  //35-39
            "infection", "intersection", "islands", "jaws", "labyrinth",                  //40-44
            "labyrinthdual", "lasers", "lasers2", "lemmings", "missilecommand",           //45-49
            "modality", "overload", "pacman", "painter", "plants",                        //50-54
            "plaqueattack", "portals", "racebet", "raceBet2", "realportals",              //55-59
            "realsokoban", "rivers", "roguelike", "run", "seaquest",                      //60-64
            "sheriff", "shipwreck", "sokoban", "solarfox" ,"superman",                    //65-69
            "surround", "survivezombies", "tercio", "thecitadel", "thesnowman",           //70-74
            "waitforbreakfast", "watergame", "waves", "whackamole", "witnessprotection",  //75-79
            "zelda", "zenpuzzle","solarfoxShoot","solarfoxShootGem", "sheriffTopBottom",
            "aliens1", "solarfoxShootGem1", "sheriff1", "S", "M", "P", "F", "W", "H", "D","E","K","A"};
	
	public static void main(String[] args) {
		//argument list
		//args[0] - directory name
		//args[1] - play or run
		//args[2] and on - can include any of these flags:
		//"-s": source task flag, usage: <game><index> (e.g., "-s W5" which means run source task: game W level 5)
		//"-t": target task flag, usage: <game><index> (e.g., "-t W5" which means run target task: game W level 5)
		//"-ns": number of episodes to run the source task, usage: <number of episodes to run the task for> (e.g., "-ns 5000" which means run source task for 5000 episodes)
		//"-nt": number of episodes to run the target task, usage: <number of episodes to run the task for> (e.g., "-nt 5000" which means run target task for 5000 episodes)
		//"-c": which conditions to run, usage: <condition1>,<condition2>... (e.g., "-c qs,qt,tt,rt,rs" which means you run all conditions: of-q on source (qs), of-q on target (qt), obt on target (tt), random on target (rt), random on source (rs))
		//"-m": mapping between two tasks, usage: {<target obj itype>:<source obj itype>,<target obj itype>:<source obj itype>} (e.g., "-m {3:4,5:16}" which means target obj class itype 3 is mapped to 4 from earlier task and similarly 5 is mapped to 16)
		//"-a": number of runs to average over flag, usage: <number of runs> (e.g., "-a 50" which means run 50 runs and average over them)
		//"-i": interval for recording, usage: <interval> (e.g., "-i 10" which means record reward every 10 episodes)
		//"-f": file with saved model to read from, usage: <interval> (e.g., "-f src/F5" which means read from directory src/F5)
		//"-v": watch the agent play the game a certain number of times, usage: <number of times you see agent play the game> (e.g., "-v 5" which means you see the agent play 5 times before continuing learning)

		//pass in directory name (if code is run using run.sh, this directory will already be created and passed in)
		File dir = new File(args[0]);
		if(!dir.exists()){ //when not running Java program from run.sh, no directory has been created yet
			dir.mkdir();
			//when running on Eclipse, the following two paths are different than when running with run.sh
			gamesPath = gamesPath.substring(gamesPath.indexOf('/')+1);
			CompetitionParameters.IMG_PATH = CompetitionParameters.IMG_PATH.substring(CompetitionParameters.IMG_PATH.indexOf('/')+1);
		}

		//read in arguments
		String[] tokens = args[1].split(" ");
		for(String token : tokens)
			System.out.print(token+",");
		System.out.println();
		switch(tokens[0]){
			case "play": runType = RunType.PLAY; break;
			case "run": runType = RunType.RUN; break;
		}
		for(int i=1; i<tokens.length; i++){
			String flag = tokens[i];
			String argument = tokens[i+1];
			switch(flag){
				case "-s": 
					sourceGame = getGameLvlIdx(argument, games);
					writeModelToFile = true; break;
				case "-t":
					targetGame = getGameLvlIdx(argument, games); break;
				case "-ns":
					numSourceEpisodes = Integer.parseInt(argument); break;
				case "-nt":
					numTargetEpisodes = Integer.parseInt(argument); break;
				case "-c":
					String[] conditions_args = argument.split(",");
					for(String c : conditions_args){
						switch(c){
							case "qs": conditions.put("OF_Q_SOURCE", conditions.size()); break;
							case "qt": conditions.put("OF_Q_TARGET", conditions.size()); break;
							case "tt": conditions.put("OBT_TARGET", conditions.size()); break;
							case "rt": conditions.put("RANDOM_TARGET", conditions.size()); break;
							case "rs": conditions.put("RANDOM_SOURCE", conditions.size()); break;
						}
					} break;
				case "-a":
					numAveraging = Integer.parseInt(argument); break;
				case "-i":
					interval = Integer.parseInt(argument); break;
				case "-m":
					fixedMapping = parseGivenMapping(argument); break;
				case "-f":
					readModelFromFile = true;
					readModelFile = new File(argument); break;
				case "-v":
					visuals = Integer.parseInt(argument); break;
			}
			i++;
		}

		if(fixedMapping != null){ //when a fixed mapping is given
			if(fixedMapping.isEmpty()) //if the given mapping is empty, run only the Q-values phase (equivalent to OF-Q)
				numEpisodesMapping = 0;
			else //otherwise, run only the mapping phase (no update of Q-values)
				numEpisodesMapping = numTargetEpisodes;
		} else {
			numEpisodesMapping = numTargetEpisodes;
		}
		
        if(runType == RunType.RUN){
	        avgRewardFile = new File(dir.getPath()+"/reward.csv");
	        allRewardFile = new File(dir.getPath()+"/allReward.csv");
	        avgGameTickFile = new File(dir.getPath()+"/gameTick.csv");
	        allGameTickFile = new File(dir.getPath()+"/allGameTick.csv");
	        avgNumWinsFile = new File(dir.getPath()+"/numWins.csv");
	        allNumWinsFile = new File(dir.getPath()+"/allNumWins.csv");
	        runInfoFile = new File(dir.getPath()+"/runInfo.txt");
	        writeInfoToFile(runInfoFile, getGameStrFromIndices(sourceGame), getGameStrFromIndices(targetGame));
	        writeModelFile = new File(dir.getPath()+"/learnedQ");
	        writeModelFile.mkdir();
        } else if(runType == RunType.PLAY){
        	humanDataFile = new File(dir.getPath()+"/humanData.txt");
        	humanWinsFile = new File(dir.getPath()+"/humanWins.csv");
        	humanScoresFile = new File(dir.getPath()+"/humanScores.csv");
        	humanTicksFile = new File(dir.getPath()+"/humanTicks.csv");
        }

		int maxDataPoints = Math.max(numSourceEpisodes,numTargetEpisodes)/interval;
		reward = new double[conditions.size()][maxDataPoints];
		numWins = new double[conditions.size()][maxDataPoints];
		gameTick = new double[conditions.size()][maxDataPoints];
		
		switch(runType){
			case RUN:
		        String conditionsStr = "";
		        int numDataPoints = 0;
		        for(String condition : conditions.keySet())
		        	numDataPoints += condition.contains("SOURCE") ? numSourceEpisodes : numTargetEpisodes; 
		        numDataPoints = numDataPoints/interval;
		        for(String condition : conditions.keySet()){
		        	conditionsStr+=condition;
		        	for(int i=0; i<numDataPoints; i++)
		        		conditionsStr+=", ";
		        	conditionsStr+=",";
		        }
		        conditionsStr+="\n";
		        writeToAllFiles(conditionsStr);
		        
		        //number of episodes for each condition (currently, first condition is using numSourceEpisodes and second two conditions are using numTargetEpisodes)
		        int[] numEpisodes = new int[conditions.size()];
				for(String condition : conditions.keySet())
					numEpisodes[conditions.get(condition)] = condition.contains("SOURCE") ? numSourceEpisodes : numTargetEpisodes;
		        for(int num=0; num<numAveraging; num++){
		        	learnedModels = new Model[conditions.size()];
		        	for(String condition : conditions.keySet()){
		        		int gameIdx = condition.contains("SOURCE") ? sourceGame[0] : targetGame[0];
		        		int levelIdx = condition.contains("SOURCE") ? sourceGame[1] : targetGame[1];
		        		String game = gamesPath + games[gameIdx] + ".txt";
		        		String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx +".txt";
		                System.out.println("PLAYING "+games[gameIdx]+" level "+levelIdx);
		        		String controller = initController(condition);
		        		if(Agent.INSTANCE != null){
		        			System.out.println("Running condition "+condition);
		        			Agent.INSTANCE.clearEachRun(); //clear learned data before each condition
				        	System.out.println("Averaging "+num);
				        	int c = conditions.get(condition);
				        	//run the condition for a full run and save learned model
				        	//currently, only the OBT_TARGET condition uses the source task model (learnedModels[0]) to learn in the target task, but it is passed to all conditions
				        	learnedModels[c] = Agent.INSTANCE.run(c, numEpisodes[c], game, level1, false, controller, learnedModels[0]).clone();
		        		}
		        		writeToAllFiles(",");
		        	}
		        	writeToAllFiles("\n");
		        } 
		        writeFinalResultsToFile(avgRewardFile, reward, numAveraging, numEpisodes);
		        writeFinalResultsToFile(avgNumWinsFile, numWins, numAveraging, numEpisodes);
		        writeFinalResultsToFile(avgGameTickFile, gameTick, numAveraging, numEpisodes);
		        System.exit(0);
		        
			case PLAY:
				System.out.println("Playing "+games[sourceGame[0]]);
		        if(runType == RunType.PLAY){
		        	String game = gamesPath + games[sourceGame[0]] + ".txt";
		        	String level1 = gamesPath + games[sourceGame[0]] + "_lvl" + sourceGame[1] +".txt";
		        	new HumanAgent(null,null);
		        	String controller = "ramyaram.HumanAgent";
		        	while(GAME_PLAY_NUM <= 10){ //human can keep playing the game until the max number of episodes
		        		writeToFile(humanDataFile, "PLAY #"+GAME_PLAY_NUM+"\n");
	        			Agent.INSTANCE.run(-1, -1, game, level1, true, controller, null);
	        			GAME_PLAY_NUM++;
		        	}
		       }
		}
	}
	
	/**
	 * Initialize controller for the given condition
	 */
	public static String initController(String condition){
		if(condition.contains("OF_Q")){
			new OFQAgent(null,null);
			return "ramyaram.OFQAgent";
		} else if(condition.contains("OBT")){
			new OBTAgent(null,null);
			return "ramyaram.OBTAgent";
		} else if(condition.contains("RANDOM")){
			new RandomAgent(null, null);
			return "ramyaram.RandomAgent";
		}
		return null;
	}
	
	/**
	 * Convert a mapping in the form of a string to a HashMap
	 */
	public static HashMap<Integer, Integer> parseGivenMapping(String mappingStr){
		mappingStr = mappingStr.substring(1, mappingStr.length()-1); //removes brackets
		HashMap<Integer, Integer> mapping = new HashMap<Integer, Integer>();
		while(mappingStr.length() > 0){
			int key = Integer.parseInt(mappingStr.substring(0, mappingStr.indexOf(':', 0)));
			int valueEndIndex = mappingStr.indexOf(',', 0);
			if(valueEndIndex == -1)
				valueEndIndex = mappingStr.length();
			int value = Integer.parseInt(""+mappingStr.substring(mappingStr.indexOf(':', 0)+1, valueEndIndex));
			mapping.put(key, value);
			if(mappingStr.contains(","))
				mappingStr = mappingStr.substring(valueEndIndex+1);
			else
				break;
		}
		return mapping;
	}
	
	public static String getGameStrFromIndices(int[] game){
		if(game[0] >= 0 && game[1] >= 0)
			return games[game[0]]+game[1];
		return "";
	}
	
	/**
	 * Given the set of all games and a string in the form of game+level (e.g., aliens0), get the game index and level index
	 */
	public static int[] getGameLvlIdx(String game_lvl, String[] allGames){
		int[] gameLvlIdx = new int[2];
		for(int i=0; i<gameLvlIdx.length; i++)
			gameLvlIdx[i] = -1;
		String gameStr = game_lvl.substring(0, game_lvl.length()-1);
		for(int i=0; i<allGames.length; i++){
			if(gameStr.equalsIgnoreCase(allGames[i]))
				gameLvlIdx[0] = i;
		}		
		gameLvlIdx[1] = Integer.parseInt(game_lvl.substring(game_lvl.length()-1));
		return gameLvlIdx;
	}
	
	public static void writeToFile(File file, String str){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
	     	writer.write(str);
	     	writer.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void writeInfoToFile(File runInfoFile, String sourceGameStr, String targetGameStr){
		writeToFile(runInfoFile, "runType="+runType+"\n");
		writeToFile(runInfoFile, "conditions="+conditions.entrySet()+"\n");
        writeToFile(runInfoFile, "sourceGame="+sourceGameStr+"\n");
        if(targetGame != null)
        	writeToFile(runInfoFile, "targetGame="+targetGameStr+"\n");
        if(fixedMapping != null)
        	writeToFile(runInfoFile, "fixedMapping="+fixedMapping.entrySet()+"\n");
        writeToFile(runInfoFile, "epsilon="+epsilon+"\n");
        writeToFile(runInfoFile, "alpha="+alpha+"\n");
        writeToFile(runInfoFile, "gamma="+gamma+"\n");
        writeToFile(runInfoFile, "mapping_alpha="+mapping_alpha+"\n");
        writeToFile(runInfoFile, "mapping_epsilon="+mapping_epsilon+"\n");
        writeToFile(runInfoFile, "mapping_epsilon_end="+mapping_epsilon_end+"\n");
        writeToFile(runInfoFile, "mapping_epsilon_delta="+mapping_epsilon_delta+"\n");  	
        writeToFile(runInfoFile, "numAveraging="+numAveraging+"\n");
        writeToFile(runInfoFile, "numSourceEpisodes="+numSourceEpisodes+"\n");
        writeToFile(runInfoFile, "numTargetEpisodes="+numTargetEpisodes+"\n");
        writeToFile(runInfoFile, "numEpisodesMapping="+numEpisodesMapping+"\n");
        writeToFile(runInfoFile, "numEpisodesQValues="+(numTargetEpisodes-numEpisodesMapping)+"\n");
        writeToFile(runInfoFile, "interval="+interval+"\n");
        writeToFile(runInfoFile, "writeModelToFile="+writeModelToFile+"\n");
        if(writeModelFile != null)
        	writeToFile(runInfoFile, "writeModelFile="+writeModelFile.getPath()+"\n");
        writeToFile(runInfoFile, "readModelFromFile="+readModelFromFile+"\n");
        if(readModelFile != null)
        	writeToFile(runInfoFile, "readModelFile="+readModelFile.getPath()+"\n");
        writeToFile(runInfoFile, "visuals="+visuals+"\n");
	}
	
	public static void writeToAllFiles(String str){
		writeToFile(allRewardFile, str);
        writeToFile(allNumWinsFile, str);
        writeToFile(allGameTickFile, str);
	}
	
	public static void writeFinalResultsToFile(File file, double[][] results, int numAveraging, int[] numEpisodes){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for(String condition : conditions.keySet()){ //all conditions
				int c = conditions.get(condition);
				writer.write(condition+", ");
				for(int j=0; j<results[c].length; j++){
					if(j < numEpisodes[c]){
						//divides the total reward by the number of simulation runs and gets the average reward the agent received over time
						writer.write(""+(results[c][j]/numAveraging));		
						if(j < numEpisodes[c]-1)
							writer.write(", ");
					}
				}
				writer.write("\n");
			}
			writer.close();
		} catch(Exception e){
			e.printStackTrace(); 
		}
	}
	
	public static void delete(File f) {
		if (f.isDirectory()) {
			for (File c : f.listFiles())
				delete(c);
		}
		if (!f.delete())
		    System.out.println("Failed to delete file: " + f);
	}
}
