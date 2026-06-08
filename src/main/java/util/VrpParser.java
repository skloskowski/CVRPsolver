package util;

import model.CVRPInstance;
import model.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VrpParser {
    public static CVRPInstance parse(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath));
        String[] lines = content.split("\n");

        String name = "";
        int capacity = 0;
        List<Node> nodes = new ArrayList<>();
        int depotId = 1;

        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            i++;

            if (line.startsWith("NAME")) {
                name = line.split(":")[1].trim();
            } else if (line.startsWith("CAPACITY")) {
                capacity = Integer.parseInt(line.split(":")[1].trim());
            } else if (line.equals("NODE_COORD_SECTION")) {
                while (i < lines.length && !lines[i].trim().isEmpty() && !lines[i].trim().startsWith("DEMAND_SECTION")) {
                    String[] parts = lines[i].trim().split("\\s+");
                    if (parts.length >= 3) {
                        int id = Integer.parseInt(parts[0]);
                        double x = Double.parseDouble(parts[1]);
                        double y = Double.parseDouble(parts[2]);
                        nodes.add(new Node(id, x, y, 0));
                    }
                    i++;
                }
                i--;
            } else if (line.equals("DEMAND_SECTION")) {
                while (i < lines.length && !lines[i].trim().isEmpty() && !lines[i].trim().startsWith("DEPOT_SECTION")) {
                    String[] parts = lines[i].trim().split("\\s+");
                    if (parts.length >= 2) {
                        int nodeId = Integer.parseInt(parts[0]);
                        int demand = Integer.parseInt(parts[1]);
                        for (Node node : nodes) {
                            if (node.id == nodeId) {
                                node.demand = demand;
                                break;
                            }
                        }
                    }
                    i++;
                }
                i--;
            } else if (line.equals("DEPOT_SECTION")) {
                while (i < lines.length && !lines[i].trim().isEmpty() && !lines[i].trim().equals("-1")) {
                    String[] parts = lines[i].trim().split("\\s+");
                    if (parts.length >= 1) {
                        depotId = Integer.parseInt(parts[0]);
                    }
                    i++;
                }
            }
        }

        Node depot = null;
        List<Node> customers = new ArrayList<>();
        for (Node node : nodes) {
            if (node.id == depotId) {
                depot = node;
            } else {
                customers.add(node);
            }
        }

        if (depot == null && !nodes.isEmpty()) {
            depot = nodes.get(0);
        }

        // Compute distance matrix
        List<Node> allNodes = new ArrayList<>();
        allNodes.add(depot);
        allNodes.addAll(customers);
        double[][] distMatrix = computeDistanceMatrix(allNodes);

        return new CVRPInstance(name, customers, depot, capacity, distMatrix);
    }

    private static double[][] computeDistanceMatrix(List<Node> nodes) {
        int n = nodes.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    double dx = nodes.get(i).x - nodes.get(j).x;
                    double dy = nodes.get(i).y - nodes.get(j).y;
                    matrix[i][j] = Math.sqrt(dx * dx + dy * dy);
                }
            }
        }
        return matrix;
    }
}
