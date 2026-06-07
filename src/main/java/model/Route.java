package model;

import java.util.ArrayList;
import java.util.List;

public class Route {
    public List<Integer> nodeIds;  // Customer node IDs (excluding depot)
    public double totalDistance;
    public int totalDemand;
    public boolean isFeasible;

    public Route() {
        this.nodeIds = new ArrayList<>();
        this.totalDistance = 0.0;
        this.totalDemand = 0;
        this.isFeasible = true;
    }

    @Override
    public String toString() {
        return String.format("Route{nodes=%s, distance=%.2f, demand=%d, feasible=%s}",
                nodeIds, totalDistance, totalDemand, isFeasible);
    }
}
