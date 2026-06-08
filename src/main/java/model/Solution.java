package model;

import java.util.ArrayList;
import java.util.List;

public class Solution {
    public List<Route> routes;
    public double totalDistance;
    public int vehicleCount;

    public Solution() {
        this.routes = new ArrayList<>();
        this.totalDistance = 0.0;
        this.vehicleCount = 0;
    }

    @Override
    public String toString() {
        return String.format("Solution{routes=%d, distance=%.2f}",
                routes.size(), totalDistance);
    }
}
