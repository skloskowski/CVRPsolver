package model;

public class Node {
    public int id;
    public double x;
    public double y;
    public int demand;

    public Node(int id, double x, double y, int demand) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.demand = demand;
    }

    @Override
    public String toString() {
        return String.format("Node{id=%d, x=%.0f, y=%.0f, demand=%d}", id, x, y, demand);
    }
}
