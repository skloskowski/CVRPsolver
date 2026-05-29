import model.CVRPInstance;
import model.Solution;
import solver.AcoSolver;
import solver.PsoSolver;
import solver.Solver;
import util.SolutionEvaluator;
import util.VrpParser;

import java.io.File;
import java.util.Arrays;

public class CVRPSolver {
    public static void main(String[] args) throws Exception {
        System.out.println("=== CVRP Solver (PSO + ACO) ===\n");

        // Load instances from data folder
        File dataDir = new File("data");
        File[] vrpFiles = dataDir.listFiles((d, name) -> name.endsWith(".vrp"));

        if (vrpFiles == null || vrpFiles.length == 0) {
            System.out.println("No .vrp files found in data/ folder");
            return;
        }

        Arrays.sort(vrpFiles);

        // Solve first instance (A-n53-k7) as demo
        CVRPInstance instance = VrpParser.parse(vrpFiles[1].getAbsolutePath());
        System.out.println("Loaded: " + instance);
        System.out.println("Depot: " + instance.depot);
        System.out.println("Customers: " + instance.nodes.size());
        System.out.println("Capacity: " + instance.vehicleCapacity + "\n");

        // Solve with PSO
        System.out.println("--- Running PSO Solver ---");
        Solver psoSolver = new PsoSolver(30, 0.7, 1.5, 1.5);
        Solution psoBestSolution = psoSolver.solve(instance, 100);
        SolutionEvaluator.printSolution(psoBestSolution, instance);

        // Solve with ACO
        System.out.println("\n--- Running ACO Solver ---");
        Solver acoSolver = new AcoSolver(20, 1.0, 2.0, 0.1);
        Solution acoBestSolution = acoSolver.solve(instance, 100);
        SolutionEvaluator.printSolution(acoBestSolution, instance);

        // Compare
        System.out.println("\n=== Summary ===");
        System.out.println("PSO Best Distance: " + String.format("%.2f", psoBestSolution.totalDistance));
        System.out.println("ACO Best Distance: " + String.format("%.2f", acoBestSolution.totalDistance));
    }
}
