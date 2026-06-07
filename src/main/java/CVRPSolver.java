import model.CVRPInstance;
import model.Solution;
import solver.AcoSolver;
import solver.PsoSolver;
import solver.Solver;
import util.ResultsWriter;
import util.VrpParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CVRPSolver {
    public static void main(String[] args) throws Exception {
        RunConfig config = RunConfig.fromArgs(args);

        System.out.println("=== CVRP Solver ===");
        System.out.println("Results folder: " + config.resultsDir.getAbsolutePath());
        if (config.batchFile != null) {
            System.out.println("Batch file: " + config.batchFile.getAbsolutePath());
        }

        if (config.batchFile != null) {
            runBatch(config);
        } else {
            runConfiguredSolvers(config);
        }
    }

    private static void runBatch(RunConfig baseConfig) throws Exception {
        List<String> lines = Files.readAllLines(baseConfig.batchFile.toPath(), StandardCharsets.UTF_8);
        int runNumber = 1;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] lineArgs = tokenizeArguments(trimmed);
            String[] mergedArgs = mergeArguments(baseConfig.toArgumentList(), lineArgs);
            RunConfig runConfig = RunConfig.fromArgs(mergedArgs);

            System.out.println();
            System.out.println("--- Batch run " + runNumber + " ---");
            runConfiguredSolvers(runConfig);
            runNumber++;
        }
    }

    private static void runConfiguredSolvers(RunConfig config) throws Exception {
        System.out.println("Instance: " + config.inputFile.getName());
        CVRPInstance instance = VrpParser.parse(config.inputFile.getAbsolutePath());
        System.out.println("Loaded customers: " + instance.nodes.size());
        System.out.println("Capacity: " + instance.vehicleCapacity);
        System.out.println();

        if (config.runPso) {
            runSolver("PSO", new PsoSolver(config.swarmSize, config.inertia, config.c1, config.c2),
                    instance, config.iterations, config.resultsDir, config.toParameterMap());
        }

        if (config.runAco) {
            runSolver("ACO", new AcoSolver(config.numAnts, config.alpha, config.beta, config.evaporation),
                    instance, config.iterations, config.resultsDir, config.toParameterMap());
        }
    }

    private static void runSolver(String solverName, Solver solver, CVRPInstance instance, int iterations,
                                  File resultsDir, Map<String, String> parameters) throws Exception {
        long start = System.currentTimeMillis();
        Solution bestSolution = solver.solve(instance, iterations);
        long runtimeMs = System.currentTimeMillis() - start;

        System.out.println(solverName + " total distance: " + String.format(Locale.US, "%.2f", bestSolution.totalDistance));

        File resultFile = ResultsWriter.writeRunSummary(resultsDir, solverName, instance, bestSolution, parameters, iterations, runtimeMs);
        System.out.println("Saved results: " + resultFile.getAbsolutePath());
        System.out.println();
    }

    private static String[] mergeArguments(String[] baseArgs, String[] overrideArgs) {
        String[] merged = new String[baseArgs.length + overrideArgs.length];
        System.arraycopy(baseArgs, 0, merged, 0, baseArgs.length);
        System.arraycopy(overrideArgs, 0, merged, baseArgs.length, overrideArgs.length);
        return merged;
    }

    private static String[] tokenizeArguments(String line) {
        List<String> tokens = new ArrayList<String>();
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(line);
        while (matcher.find()) {
            String quoted = matcher.group(1);
            tokens.add(quoted != null ? quoted : matcher.group(2));
        }
        return tokens.toArray(new String[0]);
    }

    private static class RunConfig {
        private File inputFile;
        private File resultsDir;
        private File batchFile;
        private boolean runPso = true;
        private boolean runAco = true;
        private int iterations = 1000;

        // ACO
        private int numAnts = 100;
        private double alpha = 1.0;
        private double beta = 2.0;
        private double evaporation = 0.1;

        //PSO
        private int swarmSize = 100;
        private double inertia = 0.7;
        private double c1 = 1.2;
        private double c2 = 1.2;

        private static RunConfig fromArgs(String[] args) {
            RunConfig config = new RunConfig();
            config.resultsDir = new File("results");

            File dataDir = new File("data");
            File[] vrpFiles = dataDir.listFiles((d, name) -> name.endsWith(".vrp"));
            if (vrpFiles == null || vrpFiles.length == 0) {
                throw new IllegalStateException("No .vrp files found in data/ folder");
            }
            Arrays.sort(vrpFiles);
            config.inputFile = vrpFiles[2];

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--input".equals(arg) && i + 1 < args.length) {
                    config.inputFile = new File(args[++i]);
                } else if ("--data-dir".equals(arg) && i + 1 < args.length) {
                    File[] files = new File(args[++i]).listFiles((d, name) -> name.endsWith(".vrp"));
                    if (files == null || files.length == 0) {
                        throw new IllegalArgumentException("No .vrp files found in provided data directory");
                    }
                    Arrays.sort(files);
                    config.inputFile = files[0];
                } else if ("--results-dir".equals(arg) && i + 1 < args.length) {
                    config.resultsDir = new File(args[++i]);
                } else if ("--batch-file".equals(arg) && i + 1 < args.length) {
                    config.batchFile = new File(args[++i]);
                } else if ("--solver".equals(arg) && i + 1 < args.length) {
                    String solver = args[++i].toLowerCase(Locale.ROOT);
                    config.runPso = "pso".equals(solver) || "both".equals(solver);
                    config.runAco = "aco".equals(solver) || "both".equals(solver);
                    if (!config.runPso && !config.runAco) {
                        throw new IllegalArgumentException("Unknown solver: " + solver);
                    }
                } else if ("--iterations".equals(arg) && i + 1 < args.length) {
                    config.iterations = Integer.parseInt(args[++i]);
                } else if ("--ants".equals(arg) && i + 1 < args.length) {
                    config.numAnts = Integer.parseInt(args[++i]);
                } else if ("--alpha".equals(arg) && i + 1 < args.length) {
                    config.alpha = Double.parseDouble(args[++i]);
                } else if ("--beta".equals(arg) && i + 1 < args.length) {
                    config.beta = Double.parseDouble(args[++i]);
                } else if ("--evaporation".equals(arg) && i + 1 < args.length) {
                    config.evaporation = Double.parseDouble(args[++i]);
                } else if ("--swarm".equals(arg) && i + 1 < args.length) {
                    config.swarmSize = Integer.parseInt(args[++i]);
                } else if ("--inertia".equals(arg) && i + 1 < args.length) {
                    config.inertia = Double.parseDouble(args[++i]);
                } else if ("--c1".equals(arg) && i + 1 < args.length) {
                    config.c1 = Double.parseDouble(args[++i]);
                } else if ("--c2".equals(arg) && i + 1 < args.length) {
                    config.c2 = Double.parseDouble(args[++i]);
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    printUsageAndExit();
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            return config;
        }

        private Map<String, String> toParameterMap() {
            Map<String, String> parameters = new LinkedHashMap<String, String>();
            parameters.put("iterations", Integer.toString(iterations));
            parameters.put("swarmSize", Integer.toString(swarmSize));
            parameters.put("inertia", Double.toString(inertia));
            parameters.put("c1", Double.toString(c1));
            parameters.put("c2", Double.toString(c2));
            parameters.put("numAnts", Integer.toString(numAnts));
            parameters.put("alpha", Double.toString(alpha));
            parameters.put("beta", Double.toString(beta));
            parameters.put("evaporation", Double.toString(evaporation));
            return parameters;
        }

        private String[] toArgumentList() {
            List<String> args = new ArrayList<String>();
            args.add("--input");
            args.add(inputFile.getPath());
            args.add("--results-dir");
            args.add(resultsDir.getPath());
            if (batchFile != null) {
                args.add("--batch-file");
                args.add(batchFile.getPath());
            }
            if (runPso && runAco) {
                args.add("--solver");
                args.add("both");
            } else if (runPso) {
                args.add("--solver");
                args.add("pso");
            } else if (runAco) {
                args.add("--solver");
                args.add("aco");
            }
            args.add("--iterations");
            args.add(Integer.toString(iterations));
            args.add("--swarm");
            args.add(Integer.toString(swarmSize));
            args.add("--inertia");
            args.add(Double.toString(inertia));
            args.add("--c1");
            args.add(Double.toString(c1));
            args.add("--c2");
            args.add(Double.toString(c2));
            args.add("--ants");
            args.add(Integer.toString(numAnts));
            args.add("--alpha");
            args.add(Double.toString(alpha));
            args.add("--beta");
            args.add(Double.toString(beta));
            args.add("--evaporation");
            args.add(Double.toString(evaporation));
            return args.toArray(new String[0]);
        }

        private static void printUsageAndExit() {
            System.out.println("Usage:");
            System.out.println("  java CVRPSolver [--input path] [--results-dir path] [--solver pso|aco|both]");
            System.out.println("                     [--iterations n] [--swarm n] [--inertia x] [--c1 x] [--c2 x]");
            System.out.println("                     [--ants n] [--alpha x] [--beta x] [--evaporation x]");
            System.out.println("                     [--batch-file path]");
            System.out.println("Batch file format: one run per line using the same CLI flags, e.g.");
            System.out.println("  --solver pso --iterations 100 --swarm 30");
            System.exit(0);
        }
    }
}
