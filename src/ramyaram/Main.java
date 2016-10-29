package ramyaram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Random;

/**
 * Main method that runs simulations of agents playing various games and analyzes the data
 */
public class Main {
	public enum RunType {PLAY, RUN}	
	//structures to store information from runs
	public static double[][] reward;
	public static boolean[] wins;
	public static Model[] learnedModels;
    //parameters to denote number of episodes
	public static int numAveraging = 50;
	public static int numEpisodes = 1000;
	public static int numEpisodesMapping = numEpisodes;
	public static int interval = 1;	
	//parameters for standard Q-learning
	public static double epsilon = 0.1;
	public static double alpha = 0.1;
	public static double gamma = 0.9;	
	//parameters for object-based transfer
	public static double mapping_alpha = 0.1; //The learning rate for the mapping phase
	public static double mapping_epsilon = 0.1; //The amount an agent explores (as opposed to exploit) mappings of different object classes
	public static double mapping_epsilon_end = 0.001; //The ending mapping exploration rate (after decreasing to this value, the parameter stays constant)
	public static double mapping_epsilon_delta = 0.01; //Exploration over mappings decreases by this delta value over time	
	//files for saving results
	public static File avgRewardFile;
	public static File allRewardFile;
	public static File runInfoFile;
	public static File humanDataFile;
	//settings for current run
    public static RunType runType;
	public static int[] sourceGame; //this array has 2 indices, the first specifies the game index and the second is the level index
	public static int[] targetGame; //this array has 2 indices, the first specifies the game index and the second is the level index
	public static HashMap<Integer, Integer> fixedMapping; //fixed mapping if given prior to running the task
	
	public static void main(String[] args) {
		if(args.length <= 0 || args.length > 3){
			System.out.println("Please run with the following notation:");			
			System.out.println("To play aliens (level0): java -jar Main.java aliens0");
			System.out.println("To run transfer from aliens (level0) to sheriff (level0) with a given fixed mapping (ids are 'itypes' of objects in the game): java -jar Main.java aliens5 sheriff0 {9:3,5:4,1:0}");
			System.out.println("To run transfer from aliens (level0) to sheriff (level0) with the mapping being learned: java -jar Main.java aliens0 sheriff0");
			System.exit(0);
		}
		
		String gamesPath = "examples/gridphysics/";
		String games[] = new String[]
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
                "aliens1", "solarfox1", "solarfoxShootGem1", "sheriff1"};
		
		sourceGame = getGameLvlIdx(args[0], games);
		targetGame = args.length > 1? getGameLvlIdx(args[1], games): null;
		fixedMapping = args.length > 2? parseGivenMapping(args[2]): null;
		runType = args.length > 1? RunType.RUN : RunType.PLAY;
		
        int seed = new Random().nextInt();
        int numConditions = Condition.values().length;

        String dirStr = "";
        if(runType == RunType.PLAY)
        	dirStr += runType.name()+"_";
        dirStr += args[0];
        if(targetGame != null)
        	dirStr += "_"+args[1];
        if(fixedMapping != null)
        	dirStr += "_fixed";
        int count = 2;
        while(new File(dirStr).exists()){
        	if(count > 2)
        		dirStr = dirStr.substring(0, dirStr.length()-1)+count;
        	else
        		dirStr += "_"+count;
        	count++;
        }
        File dir = new File(dirStr);	
        dir.mkdir();
        
        if(runType == RunType.RUN){
	        avgRewardFile = new File(dir.getPath()+"/reward.csv");
	        allRewardFile = new File(dir.getPath()+"/allReward.csv");
	        runInfoFile = new File(dir.getPath()+"/runInfo.txt");
	        writeInfoToFile(runInfoFile, args);
        } else if(runType == RunType.PLAY){
        	humanDataFile = new File(dir.getPath()+"/humanData.txt");
        }

		int numDataPoints = numEpisodes/interval;
		reward = new double[Condition.values().length][numDataPoints];
		wins = new boolean[numDataPoints];
		
		switch(runType){
			case RUN:
		        String conditionsStr = "";
		        for(Condition c : Condition.values()){
		        	conditionsStr+=c.name();
		        	for(int i=0; i<numDataPoints; i++)
		        		conditionsStr+=", ";
		        	conditionsStr+=",";
		        }
		        conditionsStr+="\n";
		        writeToFile(allRewardFile, conditionsStr);
		                
		        for(int num=0; num<numAveraging; num++){
		        	learnedModels = new Model[Condition.values().length];
		        	for(int c=0; c<numConditions; c++){
		        		int gameIdx = (c == 0)? sourceGame[0] : targetGame[0];
		        		int levelIdx = (c == 0)? sourceGame[1] : targetGame[1];
		        		String game = gamesPath + games[gameIdx] + ".txt";
		        		String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx +".txt";
		                System.out.println("PLAYING "+games[gameIdx]+" level "+levelIdx);
		        		String controller = initController(Condition.values()[c]);
		        		if(Agent.INSTANCE != null){
		        			System.out.println("Running condition "+Condition.values()[c]);
		        			Agent.INSTANCE.clearEachRun();
				        	System.out.println("Averaging "+num);
				        	learnedModels[c] = Agent.INSTANCE.run(c, numEpisodes, game, level1, false, controller, seed, learnedModels[0]).clone();
		        		}
				        writeToFile(allRewardFile, ",");
		        	}
		        	writeToFile(allRewardFile, "\n");
		        } 
		        
			    try{
			    	BufferedWriter writer = new BufferedWriter(new FileWriter(avgRewardFile));
					for(int i=0; i<reward.length; i++){ //all conditions
						writer.write(Condition.values()[i].name()+", ");
						for(int j=0; j<reward[i].length; j++){
							//divides the total reward by the number of simulation runs and gets the average reward the agent received over time
							writer.write(""+(reward[i][j]/numAveraging));
							if(j<reward[i].length-1)
								writer.write(", ");
						}
						writer.write("\n");
					}
					writer.close();
		        } catch(Exception e){ e.printStackTrace(); }
		        System.exit(0);
		        
			case PLAY:
				System.out.println("Playing "+games[sourceGame[0]]);
		        if(runType == RunType.PLAY){
		        	String game = gamesPath + games[sourceGame[0]] + ".txt";
		        	String level1 = gamesPath + games[sourceGame[0]] + "_lvl" + sourceGame[1] +".txt";
		        	new HumanAgent(null,null);
		        	String controller = "ramyaram.HumanAgent";
		        	int num = 0;
		        	while(true){
		        		writeToFile(humanDataFile, "PLAY #"+num+"\n");
	        			Agent.INSTANCE.run(-1, -1, game, level1, true, controller, seed, null);
		        		num++;
		        	}
		       }
		}
	}
	
	public static void writeInfoToFile(File runInfoFile, String[] args){
		writeToFile(runInfoFile, "runType="+runType+"\n");
        writeToFile(runInfoFile, "sourceGame="+args[0]+"\n");
        if(targetGame != null)
        	writeToFile(runInfoFile, "targetGame="+args[1]+"\n");
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
        writeToFile(runInfoFile, "numEpisodes="+numEpisodes+"\n");
        writeToFile(runInfoFile, "numEpisodesMapping="+numEpisodesMapping+"\n");
        writeToFile(runInfoFile, "numEpisodesQValues="+(numEpisodes-numEpisodesMapping)+"\n");
        writeToFile(runInfoFile, "interval="+interval+"\n");
	}
	
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
	
	public static String initController(Condition condition){
		switch(condition){
			case OF_Q_SOURCE:
			case OF_Q_TARGET:
				new OFQAgent(null,null);
				return "ramyaram.OFQAgent";
			case OBT_TARGET:
				new OBTAgent(null,null);
				return "ramyaram.OBTAgent";
		}
		return null;
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
}
