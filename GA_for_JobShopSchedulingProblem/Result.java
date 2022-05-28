package GA_for_JobShopSchedulingProblem;

class Result {
    public int fulfillTime = 0;
    public int[] machineWorkTime = new int[1024];
    public int[] processIds = new int[1024];
    public int[][] endTime = new int[1024][1024];
    public int[][] startTime = new int[1024][1024];
}
