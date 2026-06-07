package solver;

import model.CVRPInstance;
import model.Solution;
import util.RouteBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PsoSolver implements Solver {
    private int swarmSize = 30;
    private double inertia = 0.7;
    private double c1 = 1.5;  // cognitive parameter
    private double c2 = 1.5;  // social parameter
    private Random random = new Random();

    public PsoSolver() {
    }

    public PsoSolver(int swarmSize, double inertia, double c1, double c2) {
        this.swarmSize = swarmSize;
        this.inertia = inertia;
        this.c1 = c1;
        this.c2 = c2;
    }

    @Override
    public Solution solve(CVRPInstance instance, int iterations) {
        int[] customerIds = getCustomerIds(instance);
        int n = customerIds.length;

        int[][] particles = new int[swarmSize][n];
        double[] particleFitness = new double[swarmSize];
        int[][] bestParticles = new int[swarmSize][n];
        double[] bestFitness = new double[swarmSize];
        int[] stagnation = new int[swarmSize];

        int[] bestGlobal = new int[n];
        double bestGlobalFitness = Double.MAX_VALUE;

        for (int i = 0; i < swarmSize; i++) {
            if (i == 0) {
                particles[i] = buildGreedyPermutation(instance);
            } else if (i % 3 == 0) {
                particles[i] = mutatePermutation(particles[0]);
            } else {
                particles[i] = getRandomPermutation(customerIds);
            }
            Solution sol = RouteBuilder.buildSolutionFromPermutation(particles[i], instance);
            particleFitness[i] = sol.totalDistance;
            System.arraycopy(particles[i], 0, bestParticles[i], 0, n);
            bestFitness[i] = particleFitness[i];

            if (particleFitness[i] < bestGlobalFitness) {
                bestGlobalFitness = particleFitness[i];
                System.arraycopy(particles[i], 0, bestGlobal, 0, n);
            }
        }

        bestGlobal = refinePermutation(bestGlobal, instance);
        bestGlobalFitness = evaluatePermutation(bestGlobal, instance);

        int restartThreshold = Math.max(15, iterations / 10);

        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < swarmSize; i++) {
                if (stagnation[i] >= restartThreshold) {
                    particles[i] = getRandomPermutation(customerIds);
                    Solution restarted = RouteBuilder.buildSolutionFromPermutation(particles[i], instance);
                    particleFitness[i] = restarted.totalDistance;
                    System.arraycopy(particles[i], 0, bestParticles[i], 0, n);
                    bestFitness[i] = particleFitness[i];
                    stagnation[i] = 0;
                    continue;
                }

                applySwapMoves(particles[i], bestParticles[i], bestGlobal);

                Solution sol = RouteBuilder.buildSolutionFromPermutation(particles[i], instance);
                particleFitness[i] = sol.totalDistance;

                if (particleFitness[i] < bestFitness[i]) {
                    bestFitness[i] = particleFitness[i];
                    System.arraycopy(particles[i], 0, bestParticles[i], 0, n);
                    stagnation[i] = 0;
                } else {
                    stagnation[i]++;
                }

                if (particleFitness[i] < bestGlobalFitness) {
                    System.arraycopy(particles[i], 0, bestGlobal, 0, n);
                    bestGlobal = refinePermutation(bestGlobal, instance);
                    bestGlobalFitness = evaluatePermutation(bestGlobal, instance);
                }
            }
        }

        Solution bestSolution = RouteBuilder.buildSolutionFromPermutation(bestGlobal, instance);
        return bestSolution;
    }

    private void applySwapMoves(int[] particle, int[] bestParticle, int[] bestGlobal) {
        int n = particle.length;
        int numMoves = Math.max(1, n / 8);
        double cognitiveShare = (c1 + c2) <= 0.0 ? 0.5 : c1 / (c1 + c2);

        for (int s = 0; s < numMoves; s++) {
            double roll = random.nextDouble();
            if (roll < inertia) {
                randomInversion(particle);
            } else if (roll < inertia + (1.0 - inertia) * cognitiveShare) {
                moveTowardsBest(particle, bestParticle);
            } else {
                moveTowardsBest(particle, bestGlobal);
            }
        }
    }

    private void moveTowardsBest(int[] current, int[] best) {
        if (best == null || current.length == 0) {
            return;
        }

        int position = random.nextInt(current.length);
        int targetCustomerId = best[position];
        if (current[position] == targetCustomerId) {
            return;
        }

        int targetIndex = findIndex(current, targetCustomerId);
        if (targetIndex >= 0) {
            swap(current, position, targetIndex);
        }
    }

    private void randomInversion(int[] particle) {
        if (particle.length < 2) {
            return;
        }

        int i = random.nextInt(particle.length);
        int j = random.nextInt(particle.length);
        if (i > j) {
            int temp = i;
            i = j;
            j = temp;
        }

        while (i < j) {
            swap(particle, i, j);
            i++;
            j--;
        }
    }

    private void swap(int[] values, int i, int j) {
        int temp = values[i];
        values[i] = values[j];
        values[j] = temp;
    }

    private int findIndex(int[] values, int target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private int[] getRandomPermutation(int[] customerIds) {
        int[] perm = new int[customerIds.length];
        for (int i = 0; i < customerIds.length; i++) {
            perm[i] = customerIds[i];
        }

        // Fisher-Yates shuffle
        for (int i = perm.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            swap(perm, i, j);
        }
        return perm;
    }

    private int[] buildGreedyPermutation(CVRPInstance instance) {
        int n = instance.nodes.size();
        int[] permutation = new int[n];
        boolean[] used = new boolean[n];
        int position = 0;
        int currentMatrixIndex = 0;
        int currentLoad = 0;

        while (position < n) {
            int chosenIndex = -1;
            double chosenDistance = Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (used[i]) {
                    continue;
                }

                int demand = instance.nodes.get(i).demand;
                if (currentLoad > 0 && currentLoad + demand > instance.vehicleCapacity) {
                    continue;
                }

                int matrixIndex = i + 1;
                double distance = instance.distanceMatrix[currentMatrixIndex][matrixIndex];
                if (distance < chosenDistance) {
                    chosenDistance = distance;
                    chosenIndex = i;
                }
            }

            if (chosenIndex < 0) {
                currentMatrixIndex = 0;
                currentLoad = 0;
                continue;
            }

            used[chosenIndex] = true;
            permutation[position++] = instance.nodes.get(chosenIndex).id;
            currentLoad += instance.nodes.get(chosenIndex).demand;
            currentMatrixIndex = chosenIndex + 1;
        }

        return permutation;
    }

    private int[] mutatePermutation(int[] base) {
        int[] candidate = Arrays.copyOf(base, base.length);
        if (candidate.length < 2) {
            return candidate;
        }

        int mutations = Math.max(1, candidate.length / 20);
        for (int i = 0; i < mutations; i++) {
            randomInversion(candidate);
        }
        return candidate;
    }

    private int[] refinePermutation(int[] permutation, CVRPInstance instance) {
        int[] best = Arrays.copyOf(permutation, permutation.length);
        double bestFitness = evaluatePermutation(best, instance);
        int attempts = Math.max(4, permutation.length / 20);

        for (int i = 0; i < attempts; i++) {
            int[] candidate = Arrays.copyOf(best, best.length);
            if (random.nextBoolean()) {
                randomInversion(candidate);
            } else {
                mutateByRelocation(candidate);
            }

            double fitness = evaluatePermutation(candidate, instance);
            if (fitness < bestFitness) {
                best = candidate;
                bestFitness = fitness;
            }
        }

        return best;
    }

    private void mutateByRelocation(int[] permutation) {
        if (permutation.length < 2) {
            return;
        }

        int from = random.nextInt(permutation.length);
        int to = random.nextInt(permutation.length);
        if (from == to) {
            return;
        }

        int value = permutation[from];
        if (from < to) {
            System.arraycopy(permutation, from + 1, permutation, from, to - from);
        } else {
            System.arraycopy(permutation, to, permutation, to + 1, from - to);
        }
        permutation[to] = value;
    }

    private double evaluatePermutation(int[] permutation, CVRPInstance instance) {
        return RouteBuilder.buildSolutionFromPermutation(permutation, instance).totalDistance;
    }

    private int[] getCustomerIds(CVRPInstance instance) {
        List<Integer> ids = new ArrayList<Integer>(instance.nodes.size());
        for (int i = 0; i < instance.nodes.size(); i++) {
            ids.add(instance.nodes.get(i).id);
        }

        int[] customerIds = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            customerIds[i] = ids.get(i);
        }
        return customerIds;
    }
}
