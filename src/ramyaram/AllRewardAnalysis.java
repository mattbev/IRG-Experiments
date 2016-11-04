package ramyaram;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class AllRewardAnalysis {
	public static String line = null;
	public static int count = 0;
	public static int numAveraging = 1;
	public static ArrayList<String> labels = new ArrayList<String>();
	public static int numDataPoints = -1;
	public static int numTokensInLine = -1;
	public static double[][] reward = null;
	
	public static void main(String[] args){
		try{
			String fileName = args[0]+"/allReward.csv";
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(",");
				if(count == 0){				
					numDataPoints = tokens.length/3;
					for(int i=0; i<tokens.length; i++){
						if(tokens[i].length() > 1){
							labels.add(tokens[i]);
						}
					}
				} else {
					if(count == 1){
						reward = new double[labels.size()][numDataPoints];
						numTokensInLine = tokens.length;
					}
					if(tokens.length == numTokensInLine){
						int currLabelNum = 0;
						int currDataPointNum = 0;
						for(int i=0; i<tokens.length; i++){
							if(tokens[i].length() == 1){
								currLabelNum++;
								currDataPointNum = 0;
							}
							if(tokens[i].length() > 1){
								reward[currLabelNum][currDataPointNum] += Double.parseDouble(tokens[i]);
								currDataPointNum++;
							}
						}
						numAveraging++;
					}
				}
				count++;
			}
			System.out.println("numAveraging "+numAveraging);
			for(int i=0; i<reward.length; i++){
				for(int j=0; j<reward[i].length; j++){
					reward[i][j] = reward[i][j]/numAveraging;
				}
			}
			reader.close();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(args[0]+"/postanalysis_reward.csv")));
			for(int i=0; i<reward.length; i++){ //all conditions
				writer.write(labels.get(i)+", ");
				for(int j=0; j<reward[i].length; j++){
					//divides the total reward by the number of simulation runs and gets the average reward the agent received over time
					writer.write(""+reward[i][j]);
					if(j<reward[i].length-1)
						writer.write(", ");
				}
				writer.write("\n");
			}
			writer.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
}
