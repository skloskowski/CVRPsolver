package util;

import model.CVRPInstance;
import model.Node;
import model.Route;
import model.Solution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteBuilder {
    /**
     * Converts a permutation of customer IDs into routes.
     *
     * The permutation is treated as a tour and split into capacity-feasible routes
     */
    public static Solution buildSolutionFromPermutation(int[] permutation, CVRPInstance instance) {
        Solution solution = new Solution();
        if (permutation.length == 0) {
            SolutionEvaluator.evaluate(solution, instance);
            return solution;
        }

        List<Route> optimalRoutes = splitPermutationOptimally(permutation, instance);
        if (optimalRoutes == null) {
            solution.routes.addAll(buildGreedyRoutes(permutation, instance));
        } else {
            solution.routes.addAll(optimalRoutes);
        }

        SolutionEvaluator.evaluate(solution, instance);
        return solution;
    }

    private static List<Route> splitPermutationOptimally(int[] permutation, CVRPInstance instance) {
        int n = permutation.length;
        int[] matrixIndices = new int[n];
        int[] demands = new int[n];
        for (int i = 0; i < n; i++) {
            matrixIndices[i] = getNodeIndexInMatrix(instance, permutation[i]);
            demands[i] = getNodeDemand(instance, permutation[i]);
            if (matrixIndices[i] < 0) {
                return null;
            }
        }

        double[] bestCost = new double[n + 1];
        int[] previousCut = new int[n + 1];
        Arrays.fill(bestCost, Double.POSITIVE_INFINITY);
        Arrays.fill(previousCut, -1);
        bestCost[0] = 0.0;

        for (int end = 0; end < n; end++) {
            int routeDemand = 0;
            double routeCost = 0.0;
            int endMatrixIndex = matrixIndices[end];

            for (int start = end; start >= 0; start--) {
                int startMatrixIndex = matrixIndices[start];
                routeDemand += demands[start];
                if (routeDemand > instance.vehicleCapacity) {
                    break;
                }

                if (start == end) {
                    routeCost = instance.distanceMatrix[0][endMatrixIndex] + instance.distanceMatrix[endMatrixIndex][0];
                } else {
                    int nextMatrixIndex = matrixIndices[start + 1];
                    routeCost = routeCost
                            - instance.distanceMatrix[0][nextMatrixIndex]
                            + instance.distanceMatrix[0][startMatrixIndex]
                            + instance.distanceMatrix[startMatrixIndex][nextMatrixIndex];
                }

                if (bestCost[start] + routeCost < bestCost[end + 1]) {
                    bestCost[end + 1] = bestCost[start] + routeCost;
                    previousCut[end + 1] = start;
                }
            }
        }

        if (Double.isInfinite(bestCost[n])) {
            return null;
        }

        List<Route> routes = new ArrayList<>();
        int position = n;
        while (position > 0) {
            int start = previousCut[position];
            if (start < 0 || start >= position) {
                return null;
            }

            Route route = new Route();
            for (int i = start; i < position; i++) {
                route.nodeIds.add(permutation[i]);
            }
            routes.add(0, route);
            position = start;
        }
        return routes;
    }

    private static List<Route> buildGreedyRoutes(int[] permutation, CVRPInstance instance) {
        List<Route> routes = new ArrayList<>();
        Route currentRoute = new Route();
        int currentDemand = 0;

        for (int customerId : permutation) {
            Node customer = getNode(instance, customerId);
            if (customer == null) {
                continue;
            }

            if (currentDemand + customer.demand > instance.vehicleCapacity && !currentRoute.nodeIds.isEmpty()) {
                routes.add(currentRoute);
                currentRoute = new Route();
                currentDemand = 0;
            }

            currentRoute.nodeIds.add(customerId);
            currentDemand += customer.demand;
        }

        if (!currentRoute.nodeIds.isEmpty()) {
            routes.add(currentRoute);
        }

        return routes;
    }

    private static Node getNode(CVRPInstance instance, int nodeId) {
        for (Node node : instance.nodes) {
            if (node.id == nodeId) {
                return node;
            }
        }
        return null;
    }

    private static int getNodeIndexInMatrix(CVRPInstance instance, int nodeId) {
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

    private static int getNodeDemand(CVRPInstance instance, int nodeId) {
        Node node = getNode(instance, nodeId);
        return node == null ? 0 : node.demand;
    }
}
