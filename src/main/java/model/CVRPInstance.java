package model;

import java.util.List;

public class CVRPInstance {
    public String name;
    public List<Node> nodes;
    public Node depot;
    public int vehicleCapacity;
    public double[][] distanceMatrix;

    public CVRPInstance(String name, List<Node> nodes, Node depot, int vehicleCapacity, double[][] distanceMatrix) {
        this.name = name;
        this.nodes = nodes;
        this.depot = depot;
        this.vehicleCapacity = vehicleCapacity;
        this.distanceMatrix = distanceMatrix;
    }

    @Override
    public String toString() {
        return String.format("CVRPInstance{name='%s', nodes=%d, capacity=%d}", name, nodes.size(), vehicleCapacity);
    }
}
