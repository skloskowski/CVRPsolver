package model;

import java.util.ArrayList;
import java.util.List;

public class Solution {
    public List<Route> routes;
    public double totalDistance;
    public boolean isFeasible;
    public int vehicleCount;

    public Solution() {
        this.routes = new ArrayList<>();
        this.totalDistance = 0.0;
        this.isFeasible = true;
        this.vehicleCount = 0;
    }

    @Override
    public String toString() {
        return String.format("Solution{routes=%d, distance=%.2f, feasible=%s}",
                routes.size(), totalDistance, isFeasible);
    }
}
