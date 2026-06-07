package util;

import model.CVRPInstance;
import model.Solution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ResultsWriter {
    private ResultsWriter() {
    }

    public static File writeRunSummary(File resultsDir, String solverName, CVRPInstance instance, Solution solution,
                                       Map<String, String> parameters, int iterations, long runtimeMs) throws IOException {
        if (!resultsDir.exists() && !resultsDir.mkdirs()) {
            throw new IOException("Could not create results directory: " + resultsDir.getAbsolutePath());
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String fileName = timestamp + "_" + sanitize(solverName) + "_" + sanitize(instance.name) + ".txt";
        File output = new File(resultsDir, fileName);

        try (FileWriter writer = new FileWriter(output)) {
            writer.write("timestamp: " + timestamp + System.lineSeparator());
            writer.write("solver: " + solverName + System.lineSeparator());
            writer.write("instance: " + instance.name + System.lineSeparator());
            writer.write("customers: " + instance.nodes.size() + System.lineSeparator());
            writer.write("capacity: " + instance.vehicleCapacity + System.lineSeparator());
            writer.write("iterations: " + iterations + System.lineSeparator());
            writer.write("totalDistance: " + String.format(Locale.US, "%.2f", solution.totalDistance) + System.lineSeparator());
            writer.write("vehicleCount: " + solution.vehicleCount + System.lineSeparator());
            writer.write("feasible: " + solution.isFeasible + System.lineSeparator());
            writer.write("runtimeMs: " + runtimeMs + System.lineSeparator());
            writer.write(System.lineSeparator());
            writer.write("parameters:" + System.lineSeparator());
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                writer.write("  " + entry.getKey() + ": " + entry.getValue() + System.lineSeparator());
            }
        }

        appendRunLog(resultsDir, timestamp, solverName, instance, solution, parameters, iterations, runtimeMs);

        return output;
    }

    private static void appendRunLog(File resultsDir, String timestamp, String solverName, CVRPInstance instance,
                                     Solution solution, Map<String, String> parameters, int iterations, long runtimeMs)
            throws IOException {
        File logFile = new File(resultsDir, "run_log.txt");
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(timestamp);
            writer.write('\t');
            writer.write(solverName);
            writer.write('\t');
            writer.write(instance.name);
            writer.write('\t');
            writer.write("iterations=");
            writer.write(Integer.toString(iterations));
            writer.write('\t');
            writer.write("totalDistance=");
            writer.write(String.format(Locale.US, "%.2f", solution.totalDistance));
            writer.write('\t');
            writer.write("vehicleCount=");
            writer.write(Integer.toString(solution.vehicleCount));
            writer.write('\t');
            writer.write("feasible=");
            writer.write(Boolean.toString(solution.isFeasible));
            writer.write('\t');
            writer.write("runtimeMs=");
            writer.write(Long.toString(runtimeMs));
            writer.write('\t');
            writer.write("parameters=");
            writer.write(formatParameters(parameters));
            writer.write(System.lineSeparator());
        }
    }

    private static String formatParameters(Map<String, String> parameters) {
        List<String> entries = new ArrayList<String>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            entries.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join("; ", entries);
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
