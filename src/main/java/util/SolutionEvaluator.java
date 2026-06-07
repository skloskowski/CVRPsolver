package util;

import model.CVRPInstance;
import model.Node;
import model.Route;
import model.Solution;

import java.util.List;

public class SolutionEvaluator {
    public static void evaluate(Solution solution, CVRPInstance instance) {
        double totalDistance = 0.0;
        boolean allFeasible = true;

        // Depot is at index 0 in distance matrix
        int depotIdx = 0;

        for (Route route : solution.routes) {
            if (route.nodeIds.isEmpty()) {
                continue;
            }

            // Start at depot
            int currentIdx = depotIdx;
            double routeDistance = 0.0;
            int routeDemand = 0;
            boolean routeFeasible = true;

            for (int nodeId : route.nodeIds) {
                int nextIdx = getNodeIndexInMatrix(instance, nodeId);
                if (nextIdx >= 0) {
                    routeDistance += instance.distanceMatrix[currentIdx][nextIdx];
                    routeDemand += getNodeDemand(instance, nodeId);
                    currentIdx = nextIdx;
                }
            }

            // Return to depot
            routeDistance += instance.distanceMatrix[currentIdx][depotIdx];

            // Check feasibility
            if (routeDemand > instance.vehicleCapacity) {
                routeFeasible = false;
                allFeasible = false;
            }

            route.totalDistance = routeDistance;
            route.totalDemand = routeDemand;
            route.isFeasible = routeFeasible;
            totalDistance += routeDistance;
        }

        solution.totalDistance = totalDistance;
        solution.isFeasible = allFeasible;
        solution.vehicleCount = solution.routes.size();
    }

    // Get index in distance matrix: depot is at 0, customers at 1..n
    private static int getNodeIndexInMatrix(CVRPInstance instance, int nodeId) {
        if (instance.depot.id == nodeId) {
            return 0;
        }
        for (int i = 0; i < instance.nodes.size(); i++) {
            if (instance.nodes.get(i).id == nodeId) {
                return i + 1;  // Offset by 1 because depot is at index 0
            }
        }
        return -1;
    }

    private static int getNodeDemand(CVRPInstance instance, int nodeId) {
        for (Node node : instance.nodes) {
            if (node.id == nodeId) {
                return node.demand;
            }
        }
        return 0;
    }

    public static void printSolution(Solution solution, CVRPInstance instance) {
        System.out.println("\n=== Solution for " + instance.name + " ===");
        System.out.println("Vehicles used: " + solution.vehicleCount);
        System.out.println("Total distance: " + String.format("%.2f", solution.totalDistance));
        System.out.println("Feasible: " + solution.isFeasible);
        System.out.println("\nRoutes:");

        int routeIdx = 1;
        for (Route route : solution.routes) {
            System.out.print("  Route " + routeIdx + ": " + instance.depot.id);
            for (int nodeId : route.nodeIds) {
                System.out.print(" -> " + nodeId);
            }
            System.out.println(" -> " + instance.depot.id + " (dist: " + String.format("%.2f", route.totalDistance) + ", demand: " + route.totalDemand + ")");
            routeIdx++;
        }
    }
}
