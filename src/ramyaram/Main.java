package ramyaram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import ramyaram.Constants;

import core.competition.CompetitionParameters;

/**
 * Main method that runs simulations of agents playing various games and analyzes the data
 */
public class Main {
	public enum RunType {PLAY, RUN}	
	//structures to store information from runs
	public static double[][] reward;
	public static double[][] gameTick;
	public static double[][] numWins;
	public static Model[] learnedModels;
	public static int GAME_PLAY_NUM = 1;
	
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
            "aliens1", "solarfoxShootGem1", "sheriff1", "S", "M", "P", "F", "W", "H", "D", "E", "K", "A", "simpleGame"};
	
	public static void main(String[] args) {
		//argument list
		//args[0] - directory name
		//args[1] - play or run
		//args[2] and on - can include any of these flags:
		//"-s": source task flag, usage: <game><index> (e.g., "-s W5" which means run source task: game W level 5)
		//"-t": target task flag, usage: <game><index> (e.g., "-t W5" which means run target task: game W level 5)
		//"-ns": number of episodes to run the source task, usage: <number of episodes to run the task for> (e.g., "-ns 5000" which means run source task for 5000 episodes)
		//"-nt": number of episodes to run the target task, usage: <number of episodes to run the task for> (e.g., "-nt 5000" which means run target task for 5000 episodes)
		//"-c": which Constants.conditions to run, usage: <condition1>,<condition2>... (e.g., "-c qs,qt,tt,rt,rs" which means you run all Constants.conditions: of-q on source (qs), of-q on target (qt), obt on target (tt), random on target (rt), random on source (rs))
		//"-m": mapping between two tasks, usage: {<target obj itype>:<source obj itype>,<target obj itype>:<source obj itype>} (e.g., "-m {3:4,5:16}" which means target obj class itype 3 is mapped to 4 from earlier task and similarly 5 is mapped to 16)
		//"-a": number of runs to average over flag, usage: <number of runs> (e.g., "-a 50" which means run 50 runs and average over them)
		//"-i": interval for recording, usage: <interval> (e.g., "-i 10" which means record reward every 10 episodes)
		//"-f": file with saved model to read from, usage: <file directory> (e.g., "-f src/F5" which means read from directory src/F5)
		//"-v": watch the agent play the game, usage: <true or false> (e.g., "-v true" which means Constants.visuals is turned on, default is no Constants.visuals)

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
			case "play": Constants.runType = RunType.PLAY; break;
			case "run": Constants.runType = RunType.RUN; break;
		}
		for(int i=1; i<tokens.length; i++){
			String flag = tokens[i];
			String argument = tokens[i+1];
			switch(flag){
				case "-s": 
					Constants.sourceGame = getGameLvlIdx(argument, games);
					Constants.writeModelToFile = true; break;
				case "-t":
					Constants.targetGame = getGameLvlIdx(argument, games); break;
				case "-ns":
					Constants.numSourceEpisodes = Integer.parseInt(argument); break;
				case "-nt":
					Constants.numTargetEpisodes = Integer.parseInt(argument); break;
				case "-c":
					String[] conditions_args = argument.split(",");
					for(String c : conditions_args){
						switch(c){
							case "qs": Constants.conditions.put("OF_Q_SOURCE", Constants.conditions.size()); break;
							case "qt": Constants.conditions.put("OF_Q_TARGET", Constants.conditions.size()); break;
							case "tt": Constants.conditions.put("OBT_TARGET", Constants.conditions.size()); break;
							case "rt": Constants.conditions.put("RANDOM_TARGET", Constants.conditions.size()); break;
							case "rs": Constants.conditions.put("RANDOM_SOURCE", Constants.conditions.size()); break;
						}
					} break;
				case "-a":
					Constants.numAveraging = Integer.parseInt(argument); break;
				case "-nl":
					Constants.numEpisodesLearn = Integer.parseInt(argument); break;
				case "-ne":
					Constants.numEpisodesEval = Integer.parseInt(argument); break;
				case "-m":
					Constants.fixedMapping = parseGivenMapping(argument); break;
				case "-f":
					Constants.readModelFromFile = true;
					readModelFile = new File(argument); break;
				case "-v":
					Constants.visuals = argument.equalsIgnoreCase("true")? true : false; break;
			}
			i++;
		}
		
		Constants.numEpisodesMapping = 0;

//		if(fixedMapping != null){ //when a fixed mapping is given
//			if(fixedMapping.isEmpty()) //if the given mapping is empty, run only the Q-values phase (equivalent to OF-Q)
//				numEpisodesMapping = 0;
//			else //otherwise, run only the mapping phase (no update of Q-values)
//				numEpisodesMapping = numTargetEpisodes;
//		} else {
//			numEpisodesMapping = numTargetEpisodes;
//		}
		
        if(Constants.runType == RunType.RUN){
	        avgRewardFile = new File(dir.getPath()+"/reward.csv");
	        allRewardFile = new File(dir.getPath()+"/allReward.csv");
	        avgGameTickFile = new File(dir.getPath()+"/gameTick.csv");
	        allGameTickFile = new File(dir.getPath()+"/allGameTick.csv");
	        avgNumWinsFile = new File(dir.getPath()+"/numWins.csv");
	        allNumWinsFile = new File(dir.getPath()+"/allNumWins.csv");
	        runInfoFile = new File(dir.getPath()+"/runInfo.txt");
	        writeInfoToFile(runInfoFile, getGameStrFromIndices(Constants.sourceGame), getGameStrFromIndices(Constants.targetGame));
	        writeModelFile = new File(dir.getPath()+"/learnedQ");
	        writeModelFile.mkdir();
        } else if(Constants.runType == RunType.PLAY){
        	humanDataFile = new File(dir.getPath()+"/humanData.txt");
        	humanWinsFile = new File(dir.getPath()+"/humanWins.csv");
        	humanScoresFile = new File(dir.getPath()+"/humanScores.csv");
        	humanTicksFile = new File(dir.getPath()+"/humanTicks.csv");
        }

		int maxDataPoints = Math.max(Constants.numSourceEpisodes,Constants.numTargetEpisodes)/Constants.numEpisodesLearn;
		reward = new double[Constants.conditions.size()][maxDataPoints];
		numWins = new double[Constants.conditions.size()][maxDataPoints];
		gameTick = new double[Constants.conditions.size()][maxDataPoints];
		
		switch(Constants.runType){
			case RUN:
		        String conditionsStr = "";
		        int numDataPoints = 0;
		        for(String condition : Constants.conditions.keySet())
		        	numDataPoints += condition.contains("SOURCE") ? Constants.numSourceEpisodes : Constants.numTargetEpisodes; 
		        numDataPoints = numDataPoints/Constants.numEpisodesLearn;
		        for(String condition : Constants.conditions.keySet()){
		        	conditionsStr+=condition;
		        	for(int i=0; i<numDataPoints; i++)
		        		conditionsStr+=", ";
		        	conditionsStr+=",";
		        }
		        conditionsStr+="\n";
		        writeToAllFiles(conditionsStr);
		        
		        //number of episodes for each condition (currently, first condition is using numSourceEpisodes and second two Constants.conditions are using numTargetEpisodes)
		        int[] numEpisodes = new int[Constants.conditions.size()];
				for(String condition : Constants.conditions.keySet())
					numEpisodes[Constants.conditions.get(condition)] = condition.contains("SOURCE") ? Constants.numSourceEpisodes : Constants.numTargetEpisodes;
		        for(int num=0; num<Constants.numAveraging; num++){
		        	learnedModels = new Model[Constants.conditions.size()];
		        	for(String condition : Constants.conditions.keySet()){
		        		int gameIdx = condition.contains("SOURCE") ? Constants.sourceGame[0] : Constants.targetGame[0];
		        		int levelIdx = condition.contains("SOURCE") ? Constants.sourceGame[1] : Constants.targetGame[1];
		        		if(Constants.fixedMapping == null && Constants.readModelFromFile && Model.getSourceGame(readModelFile).equals(games[gameIdx])){
		        			Constants.fixedMapping = new HashMap<Integer, Integer>();
		        			for(int itype : Agent.getImportantObjects(games[gameIdx]))
		        				Constants.fixedMapping.put(itype, itype); //if a file is given but no mapping, assume that object itypes are mapped to themselves
		        		}
		        		String game = gamesPath + games[gameIdx] + ".txt";
		        		String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx +".txt";
		                System.out.println("PLAYING "+games[gameIdx]+" level "+levelIdx);
		        		String controller = initController(condition);
		        		if(Agent.INSTANCE != null){
		        			System.out.println("Running condition "+condition);
		        			Agent.INSTANCE.clearEachRun(); //clear learned data before each condition
				        	System.out.println("Averaging "+num);
				        	int c = Constants.conditions.get(condition);
				        	//run the condition for a full run and save learned model
				        	//currently, only the OBT_TARGET condition uses the source task model (learnedModels[0]) to learn in the target task, but it is passed to all Constants.conditions
				        	learnedModels[c] = Agent.INSTANCE.run(c, numEpisodes[c], game, level1, Constants.visuals, controller, learnedModels[0]).clone();
		        		}
		        		writeToAllFiles(",");
		        	}
		        	writeToAllFiles("\n");
		        } 
		        writeFinalResultsToFile(avgRewardFile, reward, Constants.numAveraging, numEpisodes);
		        writeFinalResultsToFile(avgNumWinsFile, numWins, Constants.numAveraging, numEpisodes);
		        writeFinalResultsToFile(avgGameTickFile, gameTick, Constants.numAveraging, numEpisodes);
		        System.exit(0);
		        
			case PLAY:
				System.out.println("Playing "+games[Constants.sourceGame[0]]);
		        if(Constants.runType == RunType.PLAY){
		        	String game = gamesPath + games[Constants.sourceGame[0]] + ".txt";
		        	String level1 = gamesPath + games[Constants.sourceGame[0]] + "_lvl" + Constants.sourceGame[1] +".txt";
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
		try{
			Field[]fields = Constants.class.getFields();
			for(Field field : fields)
				writeToFile(runInfoFile, field.getName()+"="+field.get(null)+"\n");
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void writeToAllFiles(String str){
		writeToFile(allRewardFile, str);
        writeToFile(allNumWinsFile, str);
        writeToFile(allGameTickFile, str);
	}
	
	public static void writeFinalResultsToFile(File file, double[][] results, int numAveraging, int[] numEpisodes){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for(String condition : Constants.conditions.keySet()){ //all Constants.conditions
				int c = Constants.conditions.get(condition);
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
