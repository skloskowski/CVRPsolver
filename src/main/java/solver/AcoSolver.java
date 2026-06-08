package solver;

import model.CVRPInstance;
import model.Node;
import model.Solution;
import util.RouteBuilder;
import util.SolutionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AcoSolver implements Solver {
    private int numAnts = 20;
    private double alpha = 1.0;      // pheromone
    private double beta = 2.0;       // distance
    private double evaporation = 0.1; // pheromone decay
    private Random random = new Random();

    public AcoSolver() {
    }

    public AcoSolver(int numAnts, double alpha, double beta, double evaporation) {
        this.numAnts = numAnts;
        this.alpha = alpha;
        this.beta = beta;
        this.evaporation = evaporation;
    }

    @Override
    public Solution solve(CVRPInstance instance, int iterations) {
        int n = instance.nodes.size();

        double[][] pheromone = new double[n + 1][n + 1];
        double initialPheromone = 0.1;
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                pheromone[i][j] = initialPheromone;
            }
        }

        Solution bestSolution = null;
        double bestDistance = Double.MAX_VALUE;

        // Main ACO loop
        for (int iter = 0; iter < iterations; iter++) {
            List<Solution> antSolutions = new ArrayList<>();
            double[] antDistances = new double[numAnts];

            // Build tours for each ant
            for (int ant = 0; ant < numAnts; ant++) {
                int[] tour = buildAntTour(instance, pheromone);
                Solution sol = RouteBuilder.buildSolutionFromPermutation(tour, instance);
                antSolutions.add(sol);
                antDistances[ant] = sol.totalDistance;

                // Update best
                if (sol.totalDistance < bestDistance) {
                    bestDistance = sol.totalDistance;
                    bestSolution = sol;
                }
            }

            // Update pheromone
            updatePheromone(instance, pheromone, antSolutions, antDistances, n);

        }

        return bestSolution != null ? bestSolution : RouteBuilder.buildSolutionFromPermutation(getRandomTour(n), instance);
    }

    private int[] buildAntTour(CVRPInstance instance, double[][] pheromone) {
        int n = instance.nodes.size();
        // Find max node ID to size visited array properly
        int maxNodeId = 0;
        for (Node node : instance.nodes) {
            maxNodeId = Math.max(maxNodeId, node.id);
        }
        maxNodeId = Math.max(maxNodeId, instance.depot.id);
        
        boolean[] visited = new boolean[maxNodeId + 1];  // Size to accommodate all node IDs
        List<Integer> tour = new ArrayList<>();

        int currentIdx = 0;
        int currentDemand = 0;

        while (tour.size() < n) {
            int nextCustomer = selectNextCustomer(instance, pheromone, visited, currentDemand, currentIdx);

            if (nextCustomer == -1) {
                // No feasible customer
                currentIdx = 0;
                currentDemand = 0;
                continue;
            }

            tour.add(nextCustomer);
            visited[nextCustomer] = true;
            int nextIdx = getMatrixIndex(instance, nextCustomer);
            if (nextIdx >= 0) {
                currentIdx = nextIdx;
            }
            Node customer = getNode(instance, nextCustomer);
            if (customer != null) {
                currentDemand += customer.demand;
            }
        }

        int[] result = new int[n];
        for (int i = 0; i < tour.size(); i++) {
            result[i] = tour.get(i);
        }
        return result;
    }

    private int selectNextCustomer(CVRPInstance instance, double[][] pheromone,
                                    boolean[] visited, int currentDemand, int currentIdx) {
        int n = instance.nodes.size();
        double[] probabilities = new double[n];
        double sum = 0.0;

        for (int i = 0; i < n; i++) {
            int nodeId = instance.nodes.get(i).id;
            if (visited[nodeId]) {
                probabilities[i] = 0;
                continue;
            }

            Node customer = instance.nodes.get(i);
            if (currentDemand + customer.demand > instance.vehicleCapacity) {
                probabilities[i] = 0;
                continue;
            }

            int matrixIdx = i + 1;
            double ph = Math.pow(pheromone[currentIdx][matrixIdx], alpha);
            double dist = instance.distanceMatrix[currentIdx][matrixIdx];
            double heuristic = (dist > 0) ? Math.pow(1.0 / dist, beta) : 0;
            probabilities[i] = ph * heuristic;
            sum += probabilities[i];
        }

        if (sum == 0) {
            return -1;  // No feasible customer
        }

        // Roulette wheel selection
        double rand = random.nextDouble() * sum;
        double cumsum = 0;
        for (int i = 0; i < n; i++) {
            cumsum += probabilities[i];
            if (rand <= cumsum) {
                return instance.nodes.get(i).id;
            }
        }

        return instance.nodes.get(n - 1).id;
    }

    private void updatePheromone(CVRPInstance instance, double[][] pheromone, List<Solution> antSolutions,
                                 double[] antDistances, int n) {
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                pheromone[i][j] *= (1.0 - evaporation);
            }
        }

        double bestDistance = Double.MAX_VALUE;
        for (double dist : antDistances) {
            bestDistance = Math.min(bestDistance, dist);
        }

        for (int ant = 0; ant < antSolutions.size(); ant++) {
            if (antDistances[ant] <= bestDistance * 1.1) {  // Top 10%
                double depositAmount = 1.0 / antDistances[ant];
                Solution solution = antSolutions.get(ant);
                for (int r = 0; r < solution.routes.size(); r++) {
                    List<Integer> route = solution.routes.get(r).nodeIds;
                    if (route.isEmpty()) {
                        continue;
                    }

                    int previousIdx = 0;
                    for (int i = 0; i < route.size(); i++) {
                        int nodeId = route.get(i);
                        int currentIdx = getMatrixIndex(instance, nodeId);
                        if (currentIdx >= 0) {
                            pheromone[previousIdx][currentIdx] += depositAmount;
                            pheromone[currentIdx][previousIdx] += depositAmount;
                            previousIdx = currentIdx;
                        }
                    }

                    if (previousIdx >= 0) {
                        pheromone[previousIdx][0] += depositAmount;
                        pheromone[0][previousIdx] += depositAmount;
                    }
                }
            }
        }
    }

    private int getMatrixIndex(CVRPInstance instance, int nodeId) {
        if (instance.depot.id == nodeId) {
            return 0;
        }
        for (int i = 0; i < instance.nodes.size(); i++) {
            if (instance.nodes.get(i).id == nodeId) {
                return i + 1;
            }
        }
        return -1;
    }

    private int[] getRandomTour(int n) {
        int[] tour = new int[n];
        for (int i = 0; i < n; i++) {
            tour[i] = i + 1;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = tour[i];
            tour[i] = tour[j];
            tour[j] = temp;
        }
        return tour;
    }

    private Node getNode(CVRPInstance instance, int nodeId) {
        for (Node node : instance.nodes) {
            if (node.id == nodeId) {
                return node;
            }
        }
        return null;
    }
}
