package solver;

import model.CVRPInstance;
import model.Solution;

public interface Solver {
    Solution solve(CVRPInstance instance, int iterations);
}
