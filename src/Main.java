import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.HashMap;

import ramyaram.*;

/**
 * Main method that runs simulations of agents playing various games and analyzes the data
 */
public class Main {	
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
            "aliens1", "solarfoxShootGem1", "sheriff1", "S", "M", "P", "F", "W", "H", "D", "E", "K", "A", "simpleGame",
            "X", "Y", "Z", "L", "V", "U", "Q",
            "X_small", "Y_small", "Z_small", "L_small", "V_small", "U_small", "Q_small"};
	
	public static void main(String[] args) {
	    Feedback n = new Feedback();
		//argument list
		//args[0] - directory name
		//args[1] - play or run
		//args[2] and on - can include any of these flags:
		//"-g": specifies game and level, usage: <game><index> (e.g., "-g W5" which means run game W level 5)
		//"-n": number of total episodes to run the game, usage: <number of episodes to run the game for> (e.g., "-n 5000" which means run game for 5000 episodes)
		//"-nl": number of episodes the agent learns before being evaluated, usage: <number of learning episodes> (e.g., "-nl 10" which means learn for 10 episodes, evaluate for ne episodes, then again learn for 10 episodes, evaluate until total # of episodes)
		//"-ne": number of episodes agent is evaluated (no learning takes place), usage: <number of evaluation episodes> (e.g., "-ne 50" which means you run 50 episodes in each evaluation period and average the reward to get the current performance of agent)
		//"-c": which conditions to run, usage: <condition1>,<condition2>... (e.g., "-c ofq,obt" which means you run only the specified conditions), options are: of-q on source (qs), of-q on target (qt), obt on target (tt), random on target (rt), random on source (rs))
		//"-a": number of runs to average over, usage: <number of runs> (e.g., "-a 50" which means run 50 runs and average over them)
		//"-f": file with saved model to read from, usage: <file directory> (e.g., "-f src/F5" which means read from directory src/F5)
		//"-m": mapping between two tasks, usage: {<target obj itype>:<source obj itype>,<target obj itype>:<source obj itype>} (e.g., "-m {3:4,5:16}" which means target obj class itype 3 is mapped to 4 from earlier task and similarly 5 is mapped to 16)
		//"-v": watch the agent play the game, usage: <true or false> (e.g., "-v true" which means visuals is turned on, default is no visuals)

		//pass in directory name (if code is run using run.sh, this directory will already be created and passed in)
		File dir = new File(args[0]);
		if(!dir.exists()) //when not running Java program from run.sh, no directory has been created yet
			dir.mkdir();	

		//read in arguments
		String[] tokens = args[1].split(" ");
		for(String token : tokens)
			System.out.print(token+",");
		System.out.println();
		switch(tokens[0]){
			case "play": Constants.runType = Constants.RunType.PLAY; break;
			case "run": Constants.runType = Constants.RunType.RUN; break;
		}
		for(int i=1; i<tokens.length; i++){
			String flag = tokens[i];
			String argument = tokens[i+1];
			switch(flag){
				case "-g": Constants.gameIdx = getGameIdx(argument, games);
						   Constants.levelIdx = Integer.parseInt(argument.substring(argument.length()-1)); break;
				case "-n": Constants.numTotalEpisodes = Integer.parseInt(argument); break;
				case "-nl": Constants.numEpisodesLearn = Integer.parseInt(argument); break;
				case "-ne": Constants.numEpisodesEval = Integer.parseInt(argument); break;
				case "-c":
					String[] conditions_args = argument.split(",");
					for(String c : conditions_args){
						switch(c){
							case "ofq": Constants.conditions.add("ramyaram.OFQAgent"); break;
							case "ran": Constants.conditions.add("ramyaram.RandomAgent"); break;
							case "obt": Constants.conditions.add("ramyaram.OBTAgent"); break;
						}
					} break;
				case "-a": Constants.numAveraging = Integer.parseInt(argument); break;
				case "-f": Constants.readModelFile = new File(argument); break;
				case "-m": Constants.fixedMapping = parseGivenMapping(argument); break;
				case "-v": Constants.visuals = argument.equalsIgnoreCase("true")? true : false; break;
			}
			i++;
		}
		
        if(Constants.runType == Constants.RunType.RUN){
        	Constants.avgRewardFile = new File(dir.getPath()+"/reward.csv");
        	Constants.allRewardFile = new File(dir.getPath()+"/allReward.csv");
        	Constants.avgGameTickFile = new File(dir.getPath()+"/gameTick.csv");
        	Constants.allGameTickFile = new File(dir.getPath()+"/allGameTick.csv");
        	Constants.avgNumWinsFile = new File(dir.getPath()+"/numWins.csv");
        	Constants.allNumWinsFile = new File(dir.getPath()+"/allNumWins.csv");
        	Constants.runInfoFile = new File(dir.getPath()+"/runInfo.txt");
	        writeInfoToFile(Constants.runInfoFile);
	        Constants.writeModelFile = new File(dir.getPath()+"/learnedQ");
	        Constants.writeModelFile.mkdir();
        } else if(Constants.runType == Constants.RunType.PLAY){
//        	Constants.humanDataFile = new File(dir.getPath()+"/humanData.txt");
        	Constants.humanWinsFile = new File(dir.getPath()+"/humanWins.csv");
        	Constants.humanScoresFile = new File(dir.getPath()+"/humanScores.csv");
        	Constants.humanTicksFile = new File(dir.getPath()+"/humanTicks.csv");
        }

        Constants.reward = new double[Constants.conditions.size()][Constants.numTotalEpisodes/Constants.numEpisodesLearn];
        Constants.numWins = new double[Constants.conditions.size()][Constants.numTotalEpisodes/Constants.numEpisodesLearn];
        Constants.gameTick = new double[Constants.conditions.size()][Constants.numTotalEpisodes/Constants.numEpisodesLearn];
		
		switch(Constants.runType){
			case RUN:
		        String conditionsStr = "";
		        int numDataPoints = Constants.numTotalEpisodes/Constants.numEpisodesLearn;
		        for(String condition : Constants.conditions){
		        	conditionsStr+=condition;
		        	for(int i=0; i<numDataPoints; i++)
		        		conditionsStr+=", ";
		        	conditionsStr+=",";
		        }
		        conditionsStr+="\n";
		        writeToAllFiles(conditionsStr);
		        
		        //number of episodes for each condition (currently, first condition is using numSourceEpisodes and second two Constants.conditions are using numTargetEpisodes)
		        int[] numEpisodes = new int[Constants.conditions.size()];
				for(int c=0; c<Constants.conditions.size(); c++)
					numEpisodes[c] = Constants.numTotalEpisodes;
		        for(int num=0; num<Constants.numAveraging; num++){
		        	for(int c=0; c<Constants.conditions.size(); c++){
		        		if(Constants.fixedMapping == null && Constants.readModelFile != null && Model.getSourceGame(Constants.readModelFile).equals(games[Constants.gameIdx])){
		        			Constants.fixedMapping = new HashMap<Integer, Integer>();
		        			for(int itype : Agent.getImportantObjects(games[Constants.gameIdx]))
		        				Constants.fixedMapping.put(itype, itype); //if a file is given but no mapping, assume that object itypes are mapped to themselves
		        		}
		        		String game = gamesPath + games[Constants.gameIdx] + ".txt";
		        		String level1 = gamesPath + games[Constants.gameIdx] + "_lvl" + Constants.levelIdx +".txt";
		                System.out.println("PLAYING "+games[Constants.gameIdx]+" level "+Constants.levelIdx);
		        		String controller = initController(Constants.conditions.get(c));
		        		if(Agent.INSTANCE != null){
		        			System.out.println("Running condition "+Constants.conditions.get(c));
		        			Agent.INSTANCE.clearEachRun(); //clear learned data before each condition
				        	System.out.println("Averaging "+num);
				        	//run the condition for a full run
				        	System.out.println(Agent.INSTANCE);
				        	Agent.INSTANCE.run(c, numEpisodes[c], game, level1, Constants.visuals, controller);
		        		}
		        		writeToAllFiles(",");
		        	}
		        	writeToAllFiles("\n");
		        } 
		        writeFinalResultsToFile(Constants.avgRewardFile, Constants.reward, Constants.numAveraging, numEpisodes);
		        writeFinalResultsToFile(Constants.avgNumWinsFile, Constants.numWins, Constants.numAveraging, numEpisodes);
		        writeFinalResultsToFile(Constants.avgGameTickFile, Constants.gameTick, Constants.numAveraging, numEpisodes);
		        
			case PLAY:
				System.out.println("Playing "+games[Constants.gameIdx]);
		        if(Constants.runType == Constants.RunType.PLAY){
		        	String game = gamesPath + games[Constants.gameIdx] + ".txt";
		        	String level1 = gamesPath + games[Constants.gameIdx] + "_lvl" + Constants.levelIdx +".txt";
		        	new HumanAgent(null,null);
		        	String controller = "ramyaram.HumanAgent";
		        	while(Constants.GAME_PLAY_NUM <= 5){ //human can keep playing the game until the max number of episodes
//		        		Agent.writeToFile(Constants.humanDataFile, "PLAY #"+Constants.GAME_PLAY_NUM+"\n");
	        			Agent.INSTANCE.run(-1, -1, game, level1, true, controller);
	        			Constants.GAME_PLAY_NUM++;
		        	}
		       }
		}
	}
	
	/**
	 * Initialize controller for the given condition
	 */
	public static String initController(String condition){
		if(condition.contains("OFQ"))
			new OFQAgent(null,null);
		else if(condition.contains("OBT"))
			new OBTAgent(null,null);
		else if(condition.contains("Random"))
			new RandomAgent(null, null);
		return condition;
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
	
	/**
	 * Given the set of all games and a string in the form of game+level (e.g., aliens0), get the game index and level index
	 */
	public static int getGameIdx(String game_lvl, String[] allGames){
		String gameStr = game_lvl.substring(0, game_lvl.length()-1);
		for(int i=0; i<allGames.length; i++){
			if(gameStr.equalsIgnoreCase(allGames[i]))
				return i;
		}		
		return -1;
	}
	
	public static void writeInfoToFile(File runInfoFile){
		try{
			Field[]fields = Constants.class.getFields();
			for(Field field : fields)
				Agent.writeToFile(runInfoFile, field.getName()+"="+field.get(null)+"\n");
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void writeToAllFiles(String str){
		Agent.writeToFile(Constants.allRewardFile, str);
		Agent.writeToFile(Constants.allNumWinsFile, str);
		Agent.writeToFile(Constants.allGameTickFile, str);
	}
	
	public static void writeFinalResultsToFile(File file, double[][] results, int numAveraging, int[] numEpisodes){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			for(int c=0; c<Constants.conditions.size(); c++){ //all conditions
				writer.write(Constants.conditions.get(c)+", ");
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
}
