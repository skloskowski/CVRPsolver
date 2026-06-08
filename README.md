# CRP Solver

This program written in Java solves Capacitated Vehicle Routing Problem (CVRP) using
ACO and PSO algorithms when provided with appropriate .vrp files that can be found on
CVRPLIB site provided below:

`
https://galgos.inf.puc-rio.br/cvrplib/en/instances
`

The results of the runs are stored in the `results` folder - files there are marked
with timestamps and problem filename.

## Usage

Run the solver from the project root with:

```bash
mvn -q clean package
java -cp target/classes CVRPSolver [options]
```

Common options:

- `--input path` - use a specific `.vrp` file
- `--data-dir path` - load the first `.vrp` file from a directory
- `--results-dir path` - write result files here
- `--solver pso|aco|both` - choose which solver(s) to run
- `--iterations n` - number of iterations
- `--batch-file path` - run multiple configurations from a text file

PSO options:

- `--swarm n`
- `--inertia x`
- `--c1 x`
- `--c2 x`

ACO options:

- `--ants n`
- `--alpha x`
- `--beta x`
- `--evaporation x`

Example:

```bash
java -cp target/classes CVRPSolver --input data\A-n53-k7.vrp --solver both --iterations 1000
```

## How the solvers work

### ACO

ACO builds each solution by having ants construct customer tours one node at a time. At each step, a customer is chosen with roulette-wheel selection based on:

- pheromone on the current edge
- a distance heuristic (`1 / distance`)
- the vehicle capacity constraint

If no customer fits the current route, the ant returns to the depot and starts a new route. After all ants finish, pheromone evaporates and the better ants deposit more pheromone on the edges they used.

### PSO

PSO represents each particle as a permutation of customer IDs. A particle is turned into routes by splitting the permutation into capacity-feasible routes. Each iteration, particles move by:

- random inversions
- moves toward their own best permutation
- moves toward the global best permutation

The solver starts with a mix of greedy, mutated, and random permutations, then keeps the best solution found while periodically restarting stagnant particles.