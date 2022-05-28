package GA_for_JobShopSchedulingProblem;

class Gene {
    public double fitness;
    public Integer[] chromosome;
    public Gene() {this.fitness = 0;}
    public Gene(double fitness) {this.fitness = fitness;}
}