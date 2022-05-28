package GA_for_JobShopSchedulingProblem;

import java.util.*;


class GeneticAlgorithm {
    private final int populationNumber = 60; // 种群数量
    // private final double crossProbability = 0.95;
    private final double mutationProbability = 0.05; // 变异概率
    private int jobNumber; // 工件号
    private int machineNumber; //机器号
    private int processNumber; //工序号
    private int chromosomeSize; // 染色体长度
 
    private int[][] machineMatrix = new int[1024][1024]; // 第i个工件第j个工序所在的机器
    private int[][] timeMatrix = new int[1024][1024]; // 第i个工件第j个工序所需的时间
    private int[][] processMatrix = new int[1024][1024]; // 第i个工件在第j个机器上工作的工序
 
    private Set<Gene> geneSet = new HashSet<>();

    private Random random = new Random();
    
    private int[][] Gantt; // 甘特图
    
    // 构造函数，输入工件数和机器数
    public GeneticAlgorithm(int jobNumber, int machineNumber) {
        this.jobNumber = jobNumber;
        this.machineNumber = machineNumber;
        for (int[] matrix : this.machineMatrix) Arrays.fill(matrix, -1);
        for (int[] process : this.processMatrix) Arrays.fill(process, -1);
    }
 
    // 生成0到n的数组
    private List<Integer> makeList(int n) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) result.add(i);
        return result;
    }
 
    // 删除数组中的某元素
    private Integer[] filterArray(Integer[] arr, int filterVal) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != filterVal) {
                result.add(arr[i]);
            }
        }
        return result.toArray(new Integer[0]);
    }
 
    // 初始化种群
    public  void initialPopulation() {
        for (int i = 0; i < populationNumber; i ++) {
            Gene g = new Gene();
            int size = jobNumber * machineNumber;
            List<Integer> indexList = makeList(size);
            Integer[] chromosome = new Integer[size];
            Arrays.fill(chromosome, -1);
            for (int j = 0; j < jobNumber; j++) {
                for (int k = 0; k < machineNumber; k ++) {
                    int index = random.nextInt(indexList.size());
                    int val = indexList.remove(index);
                    // 只要第j个工件在第k台机器上要进行加工，随机加入染色体的一个位置
                    if (processMatrix[j][k] != -1) {
                        chromosome[val] = j;
                    }
                }
            }
            g.chromosome = filterArray(chromosome, -1);
            g.fitness = calculateFitness(g).fulfillTime;
            geneSet.add(g);
        }
    }
 
    // 插入strat到end中的元素
    public List<Integer> subArray(Integer[] arr, int start, int end) {
        List<Integer> list = new ArrayList<>();
        for (int i = start; i < end; i++) list.add(arr[i]);
        return list;
    }
    
    // 计算适应度
    public Result calculateFitness(Gene g) {
        Result result = new Result();
        // 解码：chromosome中第i个元素表示工件编号，出现次数表示工件i正在进行的工序。
        for (int i = 0; i < g.chromosome.length; i ++) {
            int jobId = g.chromosome[i];
            int processId = result.processIds[jobId];
            int machineId = machineMatrix[jobId][processId];
            int time = timeMatrix[jobId][processId];
            // 进入下一道工序
            result.processIds[jobId] += 1;
            // 开始时间为 MAX（该工件上一道工序的结束时间，该机器上一个工件的结束时间）。 更新数据。
            result.startTime[jobId][processId] = processId ==0 ? result.machineWorkTime[machineId] :
                    Math.max(result.endTime[jobId][processId - 1], result.machineWorkTime[machineId]);
            result.machineWorkTime[machineId] = result.startTime[jobId][processId] + time;
            result.endTime[jobId][processId] = result.machineWorkTime[machineId];
            result.fulfillTime = Math.max(result.fulfillTime, result.machineWorkTime[machineId]);
        }
        return result;
    }
    // 交叉算子
    private Gene crossGene(Gene g1, Gene g2) {
        List<Integer> indexList = makeList(chromosomeSize);
        int p1 = indexList.remove(random.nextInt(indexList.size()));
        int p2 = indexList.remove(random.nextInt(indexList.size()));
 
        int start = Math.min(p1, p2);
        int end = Math.max(p1, p2);
 
        // 选取父亲染色体中start-end部分
        List<Integer> proto = subArray(g1.chromosome, start, end + 1);
        // 移除母亲染色体中与之前部分相同的部分
        List<Integer> t = new ArrayList<>();
        for (Integer c : g2.chromosome) t.add(c);
        for (Integer val : proto) {
            for (int i = 0; i < t.size(); i++) {
                if (val.equals(t.get(i))) {
                    t.remove(i);
                    break;
                }
            }
        }
        // 加入母亲部分
        proto.addAll(t.subList(start, t.size()));
        List<Integer> temp = t.subList(0, start);
        temp.addAll(proto);
        
        Gene child = new Gene();
        child.chromosome = temp.toArray(new Integer[0]);
        child.fitness = (double) calculateFitness(child).fulfillTime;
        return child;
    }
    // 变异算子
    public Gene mutationGene(Gene gene, int n) {
        List<Integer> indexList = makeList(chromosomeSize);
        for (int i = 0; i < n; i++) {
            int a = indexList.remove(random.nextInt(indexList.size()));
            int b = indexList.remove(random.nextInt(indexList.size()));
            int t = gene.chromosome[a];
            gene.chromosome[a] = gene.chromosome[b];
            gene.chromosome[b] = t;
        }
        gene.fitness = calculateFitness(gene).fulfillTime;
        return gene;
    }
    // 默认替换两次
    public Gene mutationGene(Gene gene) {
        return mutationGene(gene, 2);
    }
 
    // 选择个体，随机选取n个个体，找出适应度最高的个体
    public Gene selectGene(int n) {
        List<Integer> indexList = makeList(geneSet.size());
        Map<Integer, Boolean> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            map.put(indexList.remove(random.nextInt(indexList.size())), true);
        }
        Gene bestGene = new Gene(0xfffff);
        int i = 0;
        for (Gene gene : geneSet) {
            if (map.containsKey(i)) {
                if (bestGene.fitness > gene.fitness) {
                    bestGene = gene;
                }
            }
            i ++;
        }
        return bestGene;
    }
    // 默认从3个中选择较优个体
    public Gene selectGene() {
        return selectGene(3);
    }
 
    // 遗传算法主体部分
    public Result GA_Strategy(List<List<Integer[]>> job) {
    	// 初始化算例
        int jobSize = job.size();
 
        for (int i = 0; i < jobSize; i ++) {
            chromosomeSize += job.get(i).size();
            processNumber = Math.max(processNumber, job.get(i).size());
            // i表示工件该工序所需机器，j数字表示工作时间。
            for (int j = 0; j < job.get(i).size(); j ++) {
                machineMatrix[i][j] = job.get(i).get(j)[0];
                timeMatrix[i][j] = job.get(i).get(j)[1];
            }
        }
 
        for (int i = 0; i < jobSize; i++) {
            for (int j = 0;j < processNumber; j++){
                if (machineMatrix[i][j] != -1) {
                    processMatrix[i][machineMatrix[i][j]] = j;
                }
            }
        }
        
        // 遗传算法
        
        initialPopulation();
        
        for (int i = 0; i < populationNumber; i++) {
            double p = (double) random.nextInt(100) / 100.0;
            if (p < mutationProbability) {
                int index = random.nextInt(geneSet.size());
                int k = 0;
                for (Gene gene : geneSet) {
                    if (k == index) {
                        mutationGene(gene);
                        break;
                    }
                    k ++;
                }
            } else {
                Gene g1 = selectGene(), g2 = selectGene();
                Gene child1 = crossGene(g1, g2), child2 = crossGene(g2, g1);
                geneSet.add(child1);
                geneSet.add(child2);
            }
        }
        
        Gene bestGene = new Gene(0xffffff);
        for (Gene gene : geneSet) {
            if (bestGene.fitness > gene.fitness) {
                bestGene = gene;
            }
        }
        
        return calculateFitness(bestGene);
    }
 
    static public void main(String[] args) {
    	// 算例：每行分别表示一个工件，第一个数字表示工作机器，第二个数字表示工作时间。
        List<List<Integer[]>> job = Arrays.asList(
                Arrays.asList(new Integer[]{0, 3}, new Integer[]{1, 2}, new Integer[]{2, 2}),
                Arrays.asList(new Integer[]{0, 2}, new Integer[]{2, 1}, new Integer[]{1, 4}),
                Arrays.asList(new Integer[]{1, 4}, new Integer[]{2, 3})
        );
 
        // 工件数量为3，机器数量为3.
        int n = 3, m = 3;
        GeneticAlgorithm ga = new GeneticAlgorithm(n, m);
        Result result = ga.GA_Strategy(job);
        int processNumber = ga.processNumber;
 
        int[][] machineMatrix = ga.machineMatrix;
        System.out.println("totoal time = " + result.fulfillTime);
 
        for (int i = 0; i < n; i++) {
            for (int j = 0 ; j < processNumber; j++) {
                if (machineMatrix[i][j] != -1) {
                    System.out.println(String.format("job: %d, process: %d, machine: %d, startTime: %d, endTime: %d",
                            i, j, machineMatrix[i][j], result.startTime[i][j], result.endTime[i][j]));
                }
            }
        }
        
        // 打印对应简易甘特图
        ga.Gantt = new int[m][result.fulfillTime];
        for (int[] matrix : ga.Gantt) Arrays.fill(matrix, -1);
        // 填入数据
        for (int i = 0; i < n; i++) {
        	for (int j = 0; j < processNumber; j++) {
        		if (machineMatrix[i][j] != -1) {
        			for (int k = result.startTime[i][j]; k < result.endTime[i][j]; k++) {
        				ga.Gantt[machineMatrix[i][j]][k] = i;
        			}
        		}
        	}
        }
        // 打印
        System.out.println("Gantt chart:");
        for (int i = 0; i < n; i++) {
        	System.out.print("machine" + i + ":");
        	for (int j = 0; j < n * processNumber; j++) {
        		if (ga.Gantt[i][j] == -1)
        			System.out.print(" ");
        		else
        			System.out.print(ga.Gantt[i][j]);
        	}
        	System.out.println();
        }
    }
}