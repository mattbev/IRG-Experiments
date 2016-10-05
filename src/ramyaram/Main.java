package ramyaram;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

public class Main {
	public static double[][] reward;
	public static boolean[] wins;
	public static int numAveraging = 5;
	public static int numEpisodes = 250;
	public static int interval = 1;
	public static String fileName;
	public static String allDataFileName;
	public static LearnedModel[] learnedModels;
	
	public static void main(String[] args) {
		if(args.length <= 0 || args.length > 1){
			System.out.println("Please run with one arguments specifying the name of the csv file (e.g., javac Main.java && java Main reward.csv)");
			System.exit(0);
		}
		learnedModels = new LearnedModel[Condition.values().length];
		String gamesPath = "examples/gridphysics/";
        String games[] = new String[]{};
        games = new String[]{"aliens", "angelsdemons", "assemblyline", "avoidgeorge", "bait", //0-4
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
                "zelda", "zenpuzzle" }; 
        
        int gameIdx = -1;
        int levelIdx = 0; //level names from 0 to 4 (game_lvlN.txt).
        int seed = new Random().nextInt();
        int numConditions = Condition.values().length;
        
        fileName = args[0];
		int periodIndex = fileName.indexOf('.');
//		fileName = fileName.substring(0,periodIndex)+"_"+games[gameIdx]+fileName.substring(periodIndex);
//		periodIndex = fileName.indexOf('.');
		allDataFileName = fileName.substring(0,periodIndex)+"_all"+fileName.substring(periodIndex);
		
		int numDataPoints = numEpisodes/interval;
		reward = new double[Condition.values().length][numDataPoints];
		wins = new boolean[numEpisodes/interval];
		
		File file = new File(fileName);
		if(file.exists())
			file.delete();
		file = new File(allDataFileName);
		if(file.exists())
			file.delete();
	     
        String conditionsStr = "";
        for(Condition c : Condition.values()){
        	conditionsStr+=c.name();
        	for(int i=0; i<numDataPoints; i++)
        		conditionsStr+=", ";
        	conditionsStr+=",";
        }
        conditionsStr+="\n";
        writeToFile(allDataFileName, conditionsStr);
                
        for(int num=0; num<numAveraging; num++){
        	for(int c=0; c<numConditions; c++){
//        		if(c==0)
//        			gameIdx = 0;
//        		else
        			gameIdx = 49;
                String game = gamesPath + games[gameIdx] + ".txt";
                String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx +".txt";
                System.out.println("PLAYING "+games[gameIdx]);
        		String controller = getConditionController(Condition.values()[c]);
        		initializeController(Condition.values()[c]);
        		if(Agent.INSTANCE != null){
        			System.out.println("Running condition "+Condition.values()[c]);
        			Agent.INSTANCE.clearEachRun();
		        	System.out.println("Averaging "+num);
		        	learnedModels[c] = Agent.INSTANCE.run(c, numEpisodes, game, level1, controller, seed, learnedModels[0]);
        		}
		        writeToFile(allDataFileName, ",");
        	}
        	writeToFile(allDataFileName, "\n");
        } 
        
	    try{
	    	BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
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
        } catch(Exception e){
        	e.printStackTrace();
        }
        System.exit(0);
        
//        String game = gamesPath + games[gameIdx] + ".txt";
//        String level1 = gamesPath + games[gameIdx] + "_lvl" + levelIdx +".txt";
//        String myController = "ramyaram.OFQAgent";
//        while(true){
//        	ArcadeMachine.runOneGame(game, level1, true, myController, null, seed, 0);
//        }
//        ArcadeMachine.playOneGame(game, level1, null, seed);
	}
	
	public static String getConditionController(Condition condition){
		switch(condition){
			case OF_Q_SOURCE:
			case OF_Q_TARGET:
				return "ramyaram.OFQAgent";
			case OBT_TARGET:
				return "ramyaram.OBTAgent";
		}
		return null;
	}
	
	public static Agent initializeController(Condition condition){
		switch(condition){
			case OF_Q_SOURCE:
			case OF_Q_TARGET:
				return new OFQAgent(null,null);
			case OBT_TARGET:
				return new OBTAgent(null,null);
		}
		return null;
	}
	
	public static void writeToFile(String fileName, String str){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName), true));
	     	writer.write(str);
	     	writer.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
