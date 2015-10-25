package levelGenerators.geneticLevelGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import core.game.GameDescription;
import core.generator.AbstractLevelGenerator;
import levelGenerators.GameAnalyzer;
import tools.ElapsedCpuTimer;
import tools.LevelMapping;

public class LevelGenerator extends AbstractLevelGenerator{
	private LevelMapping bestChromosomeLevelMapping;
	private ArrayList<Double> bestFitness;
	private ArrayList<Double> averageFitness;
	private ArrayList<Double> stdFitness;
	
	public LevelGenerator(GameDescription game, ElapsedCpuTimer elapsedTimer){
		SharedData.random = new Random();
		SharedData.gameDescription = game;
		SharedData.gameAnalyzer = new GameAnalyzer(game);
		SharedData.constructiveGen = new levelGenerators.constructiveLevelGenerator.LevelGenerator(game, null);
		bestChromosomeLevelMapping = null;
		bestFitness = null;
		averageFitness = null;
		stdFitness = null;
	}
	
	private double getMean(ArrayList<Double> array){
		double total = 0;
		for(double d:array){
			total += d;
		}
		
		return total / array.size();
	}
	
	private double getSTD(ArrayList<Double> array){
		double total = 0;
		double average = getMean(array);
		for(double d:array){
			total += d * d;
		}
		
		return Math.sqrt(total / array.size() - average * average);
	}
	
	private ArrayList<Chromosome> getNextPopulation(ArrayList<Chromosome> population){
		ArrayList<Chromosome> newPopulation = new ArrayList<Chromosome>();
		
		ArrayList<Double> fitnessArray = new ArrayList<Double>();
		for(int i=0;i<population.size();i++){
			population.get(i).calculateFitness(SharedData.EVALUATION_TIME, true);
			System.out.println("\tChromosome #" + (i+1) + " Fitness: " + population.get(i).getFitness());
			fitnessArray.add(population.get(i).getFitness());
		}
		
		Collections.sort(fitnessArray);
		bestFitness.add(fitnessArray.get(fitnessArray.size() - 1));
		averageFitness.add(getMean(fitnessArray));
		stdFitness.add(getSTD(fitnessArray));
		
		while(newPopulation.size() < SharedData.POPULATION_SIZE){
			Chromosome parent1 = rouletteWheelSelection(population);
			Chromosome parent2 = rouletteWheelSelection(population);
			Chromosome child1 = parent1.clone();
			Chromosome child2 = parent2.clone();
			if(SharedData.random.nextDouble() < SharedData.CROSSOVER_PROB){
				ArrayList<Chromosome> children = parent1.crossOver(parent2);
				child1 = children.get(0);
				child2 = children.get(1);
				
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child1.mutate();
				}
				
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child2.mutate();
				}
			}
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child1.mutate();
			}
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child2.mutate();
			}
			
			newPopulation.add(child1);
			newPopulation.add(child2);
		}
		
		Collections.sort(newPopulation);
		for(int i=SharedData.POPULATION_SIZE - SharedData.ELITISM_NUMBER;i<newPopulation.size();i++){
			newPopulation.remove(i);
		}
		
		Collections.sort(population);
		for(int i=0;i<SharedData.ELITISM_NUMBER;i++){
			newPopulation.add(population.get(i));
		}
		
		return newPopulation;
	}
	
	private Chromosome rouletteWheelSelection(ArrayList<Chromosome> population){
		double[] probabilities = new double[population.size()];
		probabilities[0] = population.get(0).getFitness();
		for(int i=1; i<population.size(); i++){
			probabilities[i] = probabilities[i-1] + population.get(i).getFitness();
		}
		
		for(int i=0; i<probabilities.length; i++){
			probabilities[i] = probabilities[i] / probabilities[probabilities.length - 1];
		}
		
		double prob = SharedData.random.nextDouble();
		
		for(int i=0; i<probabilities.length; i++){
			if(prob < probabilities[i]){
				return population.get(i);
			}
		}
		
		return null;
	}
	
	@Override
	public String generateLevel(GameDescription game, ElapsedCpuTimer elapsedTimer) {
		bestFitness = new ArrayList<Double>();
		averageFitness = new ArrayList<Double>();
		stdFitness  = new ArrayList<Double>();
		
		SharedData.gameDescription = game;
		
		int size = 0;
		if(SharedData.gameAnalyzer.getSolidSprites().size() > 0){
			size = 2;
		}
		
		int width = (int)Math.max(SharedData.MIN_SIZE + size, game.getAllSpriteData().size() * (1 + 0.25 * SharedData.random.nextDouble()) + size);
		int height = (int)Math.max(SharedData.MIN_SIZE + size, game.getAllSpriteData().size() * (1 + 0.25 * SharedData.random.nextDouble()) + size);
		width = (int)Math.min(width, SharedData.MAX_SIZE + size);
		height = (int)Math.min(height, SharedData.MAX_SIZE + size);
		
		ArrayList<Chromosome> chromosomes = new ArrayList<Chromosome>();
		for(int i =0; i < SharedData.POPULATION_SIZE; i++){
			Chromosome chromosome = new Chromosome(width, height);
			if(SharedData.CONSTRUCTIVE_INITIALIZATION){
				chromosome.InitializeConstructive();
			}
			else{
				chromosome.InitializeRandom();
			}
			chromosomes.add(chromosome);
		}
		
		double worstTime = 2 * SharedData.EVALUATION_TIME * SharedData.POPULATION_SIZE;
		double avgTime = worstTime;
		double totalTime = 0;
		int numberOfIterations = 0;
		
		while(elapsedTimer.remainingTimeMillis() > 2 * avgTime && 
				elapsedTimer.remainingTimeMillis() > worstTime){
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			
			System.out.println("Generation #" + (numberOfIterations + 1) + ": ");
			chromosomes = getNextPopulation(chromosomes);
			
			numberOfIterations += 1;
			totalTime += timer.elapsedMillis();
			avgTime = totalTime / numberOfIterations;
		}
		
		for(int i=0;i<chromosomes.size();i++){
			chromosomes.get(i).calculateFitness(SharedData.EVALUATION_TIME, true);
		}
		
		Collections.sort(chromosomes);
		
		bestChromosomeLevelMapping = chromosomes.get(0).getLevelMapping();
		System.out.println("Best Chromosome Fitness: " + chromosomes.get(0).getFitness());
		System.out.println(bestFitness);
		System.out.println(averageFitness);
		System.out.println(stdFitness);
		return chromosomes.get(0).getLevelString(bestChromosomeLevelMapping);
	}

	@Override
	public HashMap<Character, ArrayList<String>> getLevelMapping(){
		return bestChromosomeLevelMapping.getCharMapping();
	}
}
