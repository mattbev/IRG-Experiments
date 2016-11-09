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
	public static int numAveraging = 0;
	public static ArrayList<String> labels = new ArrayList<String>();
	public static int numDataPoints = -1;
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
					numDataPoints = tokens.length/labels.size()-1; //number of data points per condition is total in line/number of labels - 1 for space between conditions
					lineLength = tokens.length;
					reward = new double[labels.size()][numDataPoints];
				} else {
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
					//average reward the agent received over time
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
