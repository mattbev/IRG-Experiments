package ramyaram;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * Given the allReward.csv file from running Main.java, this class will run an analysis and print out a summary of the results (e.g., reward curve averaged over runs)
 * This can be used even if the full code has not yet executed (will not include partially-completed runs)
 */
public class AllRewardAnalysis {
	public static String line = null;
	public static int count = 0;
	public static int numAveraging = 0;
	public static ArrayList<String> labels = new ArrayList<String>();
	public static int[] numDataPoints;
	public static double[][] reward = null;
	public static int lineLength = -1;
	public static void main(String[] args){
		try{
			String fileName = args[0]+"/allReward.csv";
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(",");
				if(count == 0){	//line with condition labels	
					for(int i=0; i<tokens.length; i++){
						if(tokens[i].length() > 1)
							labels.add(tokens[i]); //get all labels
					}
					numDataPoints = new int[labels.size()]; //number of data points per condition
					int numSourceEpisodes = getVariableValueFromFile(args[0]+"/runInfo.txt", "numSourceEpisodes");
					int numTargetEpisodes = getVariableValueFromFile(args[0]+"/runInfo.txt", "numTargetEpisodes");
					int maxEpisodes = Math.max(numSourceEpisodes, numTargetEpisodes);
					numDataPoints[0] = numSourceEpisodes;
					for(int i=1; i<numDataPoints.length; i++)
						numDataPoints[i] = numTargetEpisodes;
					reward = new double[labels.size()][maxEpisodes];
				} else {
					if(count == 1)
						lineLength = tokens.length;
					if(tokens.length < lineLength) //if the full run hasn't been completed, do not include it in the summary
						break;
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
				count++;
			}
			System.out.println("numAveraging "+numAveraging);
			for(int i=0; i<reward.length; i++){
				for(int j=0; j<reward[i].length; j++){
					reward[i][j] = reward[i][j]/numAveraging; //divide total by number of runs
				}
			}
			reader.close();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(args[0]+"/postanalysis_reward.csv")));
			for(int i=0; i<reward.length; i++){ //all conditions
				writer.write(labels.get(i)+", ");
				for(int j=0; j<reward[i].length; j++){
					if(j < numDataPoints[i]){
						//average reward the agent received over time
						writer.write(""+reward[i][j]);
						if(j < numDataPoints[i]-1)
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
	
	/**
	 * Parses the given file for the value of the specified variable
	 */
	public static int getVariableValueFromFile(String fileName, String variableName){
		int value = -1;
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
			while ((line = reader.readLine()) != null) {
				if(line.contains(variableName)){
					String valueStr = line.substring(line.indexOf(variableName)+variableName.length()+1);
					value = Integer.parseInt(valueStr);
					break;
				}
			}
			reader.close();
		} catch(Exception e){e.printStackTrace();}
		return value;
	}
}
