import java.util.ArrayList;
import java.util.Arrays;

public class GeneticAlgorithm {
    public static MersenneTwister randomGenerator = new MersenneTwister();

    double currentBestFitness = Double.MIN_VALUE;
    public Chromosome[] population;
    private int numberOfCrossoverOperations = 0;
    private int numberOfMutationOperations = 0;

    public String selectionMethod;
    public double mutation_ratio;
    public double crossover_ratio;
    public String crossover_method;
    public String mutation_method;
    public String config;
    public int maxIterations;

    // What is this used for ??
    private double elitismRatio;

    public GeneticAlgorithm(String selectionMethod, double mutation_ratio, double crossover_ratio,
                            String crossover_method, String mutation_method, String config, int max_iterations){
        this.selectionMethod = selectionMethod;
        this.mutation_ratio = mutation_ratio;
        this.crossover_ratio = crossover_ratio;
        this.crossover_method = crossover_method;
        this.mutation_method = mutation_method;

        this.config = config;
        this.maxIterations = max_iterations;

        population = new Chromosome[Driver.num_of_items];
    }

    public void RandomInitialization() {
        for (int i = 0; i < population.length; i++) {
            Chromosome chromosome = new Chromosome(new SolutionInstance(Driver.Items, Driver.num_of_items, Driver.max_capacity));
            population[i] = chromosome;
            population[i].generateRandom();
            population[i].updateMetrics();
        }
    }

    public void execute(){
        //System.out.println(config);
        RandomInitialization();

        // Sort DESCENDING array according to fitness => comparable already added
        Arrays.sort(population);

        Chromosome bestChromosome = population[0];
        currentBestFitness = bestChromosome.getFitness();
        System.out.printf("Best Fitness : %d\n", bestChromosome.getFitness());
        System.out.printf("Worst Fitness: %d\n", population[149].getFitness());

        long runtimeStart = System.currentTimeMillis();
        int i = 0;

        while ( (i < Driver.max_iterations) ) {

            if ( (i%500) == 0 ) System.out.printf("Generation: %d, Best = %d\n", i, bestChromosome.getFitness());
            evolve();
            Arrays.sort(population); // sorts descending
            bestChromosome = population[0];
            if (bestChromosome.getFitness() > currentBestFitness) {
                currentBestFitness = bestChromosome.getFitness();
                System.out.printf("generation %d, Fitness - %d, Weight - %d\n", i, bestChromosome.getFitness(),
                        bestChromosome.getWeight());
            }
            i++;
        }

        System.out.println("generation                  : " + String.valueOf(i));
        System.out.println("best fitness                : " + String.valueOf(bestChromosome.getFitness()) );
        System.out.println("best weight                : " + String.valueOf(bestChromosome.getWeight()) );
        System.out.println("runtime                     : " + (System.currentTimeMillis() - runtimeStart) + " ms");
        System.out.println("numberOfCrossoverOperations : " + numberOfCrossoverOperations);
        System.out.println("numberOfMutationOperations  : " + numberOfMutationOperations);

        System.out.println(bestChromosome.gene.toString());
        int itotal = 0;
        ArrayList<Integer> arrV = new ArrayList<Integer>();
        ArrayList<Integer> arrW = new ArrayList<Integer>();

        for (int j = 0; j < 150; j++) {
            if (bestChromosome.gene.Solution[j] == true){
                itotal = itotal + bestChromosome.gene.Items[j].getValue();
                arrV.add( bestChromosome.gene.Items[j].getValue() ) ;
                arrW.add( bestChromosome.gene.Items[j].getWeight() ) ;
            }
        }
        System.out.println("Values: " + arrV);
        System.out.println("Weights: " + arrW );
    }

    // *******Make evolve tooHeavy safe and has no chromosomes that are too heavy***********
    public void evolve() {
        Chromosome[] new_Population = new Chromosome[population.length];
        // keeping top 10% and possibly evolving bottom 90%
        double index_percentage = 0.1;
        int index = (int) Math.round(index_percentage * population.length);
//        System.out.println(population.length);
        // Copy top 10% from population into new_population
        System.arraycopy(population, 0, new_Population, 0, index);

        // Fill the rest of new_population with selection, crossover&mutation.
        while (index < population.length){
            //System.out.println("here 1");

            // Check for cross-over or not => produce 2 children
            if (randomGenerator.nextDouble() < crossover_ratio){
                numberOfCrossoverOperations++;
                //System.out.println("here 2");

                // Select Parents
                Chromosome[] parentArray = null;
                if (selectionMethod.equals("RWS")) {
                    parentArray = RouletteWheel.selectParents(population);
                    //System.out.println("here 3");
                } else {                                                              // Could be 40 ****
                    parentArray = Tournament.selectParents(population, 30);
                    //System.out.println("here 4");
                }
                // recombine pairs of parents to make offspring
                Chromosome[] children = new Chromosome[2];
                if (crossover_method.equals("1PX"))
                {
                    //System.out.println("here 5");
                    int cross_point = randomGenerator.nextInt(1, 148);
                    children[0] = rollBack(OnePointX.cross(parentArray, cross_point, 0));
                    children[1] = rollBack(OnePointX.cross(parentArray, cross_point, 1));

                } else {
                    //System.out.println("here 6");
                    int cross_point1 = randomGenerator.nextInt(1, 148);
                    int cross_point2 = randomGenerator.nextInt(1, 148);
                    if (cross_point1 < cross_point2){
                        children[0] = rollBack(TwoPointX.cross(parentArray, cross_point1, cross_point2, 0));
                        children[1] = rollBack(TwoPointX.cross(parentArray, cross_point1, cross_point2, 1));
                    }
                    else {
                        children[0] = rollBack(TwoPointX.cross(parentArray, cross_point2, cross_point1, 0));
                        children[1] = rollBack(TwoPointX.cross(parentArray, cross_point2, cross_point1, 1));
                    }

                }


                // Mutate first child
                if (randomGenerator.nextDouble() < mutation_ratio) {
                    //System.out.println("here 7");
                    numberOfMutationOperations++;
                    Chromosome c = null;

                    if (mutation_method.equals("BFM")) {
                        c = rollBack(Mutation.BFM(children[0]));
                    } else if (mutation_method.equals("EXM")) {
                        c = rollBack(Mutation.EXM(children[0]) );
                    } else if (mutation_method.equals("IVM")) {
                        c = rollBack(Mutation.IVM(children[0]));
                    } else if (mutation_method.equals("ISM")) {
                        c = rollBack(Mutation.ISM(children[0]));
                    } else if (mutation_method.equals("DPM")) {
                        c = rollBack(Mutation.DPM(children[0]));
                    } else {
                        System.out.println("Mutation Error in String - 1");
                    }
                    new_Population[index] = c;
                }else{
                    //System.out.println("here 8");
                    new_Population[index] = children[0];
                }
                index++;
                //System.out.println("here 7.1");

                if (index < population.length){
                    //System.out.println("here 9");
                    // Mutate Second child
                    if (randomGenerator.nextDouble() < mutation_ratio) {
                        //System.out.println("here 10");
                        numberOfMutationOperations++;
                        Chromosome c = null;

                        if (mutation_method.equals("BFM")) {
                            c = rollBack(Mutation.BFM(children[1]));
                        } else if (mutation_method.equals("EXM")) {
                            c = rollBack(Mutation.EXM(children[1]));
                        } else if (mutation_method.equals("IVM")) {
                            c = rollBack(Mutation.IVM(children[1]));
                        } else if (mutation_method.equals("ISM")) {
                            c = rollBack(Mutation.ISM(children[1]));
                        } else if (mutation_method.equals("DPM")) {
                            c = rollBack(Mutation.DPM(children[1]));
                        } else {
                            System.out.println("Mutation Error in String - 2");
                        }
                        new_Population[index] = c;
                    }else{
                        new_Population[index] = children[1];
                    }
                    //System.out.println("here 10.1");
                }


            } else if (randomGenerator.nextDouble() < mutation_ratio){
                //System.out.println("here 11");
                numberOfMutationOperations++;
                Chromosome c = null;

                if (mutation_method.equals("BFM")){
                    c = rollBack(Mutation.BFM(population[index]));
                }
                else if (mutation_method.equals("EXM")){
                    c = rollBack(Mutation.EXM(population[index]));
                }
                else if (mutation_method.equals("IVM")){
                    c = rollBack(Mutation.IVM(population[index]));
                }
                else if (mutation_method.equals("ISM")){
                    c = rollBack(Mutation.ISM(population[index]));
                }
                else if(mutation_method.equals("DPM")){
                    c = rollBack(Mutation.DPM(population[index]));
                }
                else {
                    System.out.println("Mutation Error in String");
                }
                new_Population[index] = c;

            }else{
                //System.out.println("here 12");
                // In the case where no cross over or mutations performed
                new_Population[index] = population[index];
            }

            index++;
        } // end while loop
        //System.out.println("here 13");

        // Clone or no clone??
        population = new_Population.clone();

    }

    public static Chromosome rollBack(Chromosome new_chromosome) {
        //return new_chromosome;
        if (new_chromosome.getWeight() <= 822) return new_chromosome;
        Chromosome rolledChromosome = new Chromosome(new SolutionInstance(Driver.Items, Driver.num_of_items,
                Driver.max_capacity, new_chromosome.gene.Solution));
        int count = 0;

        do{
            int rand = GeneticAlgorithm.randomGenerator.nextInt(0, 149);
            if (rolledChromosome.gene.getBit(rand) ){
                rolledChromosome.gene.Solution[rand] = flip(rolledChromosome.gene.getBit(rand));
            }
            count++;
            rolledChromosome.updateMetrics();
        } while (rolledChromosome.getWeight() > 822);

        //System.out.printf("Rolled Back: %d times\n", count);
        return rolledChromosome;

    }

    public static boolean flip(boolean b){
        if (b) return false;
        else return true;
    }

}
