package jobshop.solvers;

//Simport jdk.internal.loader.Resource;
import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.math.*;

public class RecuitSimule implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
        public String toString() {
            return("machine : " + this.machine + " First Task : " + this.firstTask + " Last Task : "+ this.lastTask);
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            //on récupère les deux taches
            Task One = order.matrix[machine][t1];
            //Task Two = order.matrix[machine][t2];

            //on updtate la matrix
            order.matrix[machine][t1]=order.matrix[machine][t2];
            order.matrix[machine][t2]=One;
            //throw new UnsupportedOperationException();
        }
    }

    //fonction de metropolis
    boolean accept(double deltaf, double t){
        if (deltaf<=0){
            return true;
        } else {
            double a = Math.pow(Math.E,-deltaf/t);
            Random rand = new Random();
            double p = rand.nextDouble();
            if (p<=a){
                return true;
            }else{
                return false;
            }
        }
    }
    private Solver sol;
    public RecuitSimule(String s){
        if (s.equals("SPT")){
            this.sol = new SPTGreedySolver();
        } else if (s.equals("LRPT")){
            this.sol = new LRPTgreedySolver();
        } else if (s.equals("ESTSPT")){
            this.sol = new EST_SPTGreedySolver();
        } else if (s.equals("ESTLRPT")){
            this.sol = new EST_LRPTGreedySolver();
        } else if (s.equals("SRMT")){
            this.sol = new SRMTGreedySolver();
        } else {
            this.sol = new LRPTgreedySolver();
        }
    }

    @Override
    public Result solve(Instance instance, long deadline){

        //on recup un resultat
        Solver sol = this.sol;
        Result res = sol.solve(instance, deadline);
        //resourceorder pour les évaluations
        ResourceOrder travail = new ResourceOrder(res.schedule);
        //resourceorder de sauvegarde
        ResourceOrder init = travail.copy();
        //temperature de départ
        double temp = 100.0;
        //coefficient pour réguler la température
        double k = 0.48;
        //calcul de la difference deltaf
        double delta = 0.0;
        //random
        Random rand = new Random();


        //on cree la liste des blocks
        List<Block> LB = blocksOfCriticalPath3(travail);

        //liste des voisins (tous les swaps de tous les blocks)
        List<Swap> voisins = new ArrayList<Swap>();

        while (temp>0.5){
            
            //System.out.println("taille de lb : " +LB.size());
            for (Block b : LB){
                //System.out.println("taille de b : " +(b.lastTask - b.firstTask));
                List<Swap> LS = neighbors(b);
                //System.out.println("taille de ls : " +LS.size());
                for (Swap s:LS){
                    voisins.add(s);
                }
            }
            int choix = rand.nextInt(voisins.size());
            //System.out.println("choix = "+ choix + " , taille de voisins "+ voisins.size());
            ResourceOrder aux = travail.copy();
            voisins.get(choix).applyOn(aux);
            delta = (init.toSchedule().makespan()-aux.toSchedule().makespan());
            if (accept(delta,temp)){
                init=aux;
            }
            temp = k*temp;
            travail=init;
            LB = blocksOfCriticalPath3(travail);
            voisins.clear();
        }
        

        return new Result(instance, init.toSchedule(), Result.ExitCause.Blocked);
    }

    public static List<Block> blocksOfCriticalPath3(ResourceOrder order) {
        List<Task> criticalPath = order.toSchedule().criticalPath();
        List<Block> blockOfPath = new ArrayList<Block>();
        Task current, next;
        
        for(int i = 0; i<criticalPath.size()-1; i++) {
            current = criticalPath.get(i);
            next = criticalPath.get(i+1);
            
            if(blockOfPath.size() != 0 && (blockOfPath.get(blockOfPath.size()-1).machine) == order.instance.machine(current)) {
                Block newBlock = new Block(order.instance.machine(current), blockOfPath.get(blockOfPath.size()-1).firstTask, taskIndex(order,current));
                blockOfPath.remove(blockOfPath.size()-1);
                blockOfPath.add(newBlock);
            }
            else {
                if(order.instance.machine(current)== order.instance.machine(next)) {
                    blockOfPath.add(new Block(order.instance.machine(current), taskIndex(order,current), taskIndex(order,next)));
                }
            }
        }
        current = criticalPath.get(criticalPath.size()-1);
        return(blockOfPath);
    }


    List<Swap> neighbors(Block block) {
        //on créé la liste de swaps à retourner
        List<Swap> listeS = new ArrayList<Swap>();
        //on récup le nb de taches
        int nb = block.lastTask-block.firstTask+1;
        //on recup la machine
        int m=block.machine;


        //System.out.println(" block, t1 = "  + block.firstTask + ", t2 =" + block.lastTask);
        
        listeS.add(new Swap(m, block.firstTask, block.firstTask+1));
	if (nb>2){
	    listeS.add(new Swap(m,block.lastTask-1,block.lastTask));
	}
        /*System.out.println("voici la liste des swaps pour ce block");
        for (int o=0;o<listeS.size();o++){
            System.out.println("first : " +listeS.get(o).t1 + "," +listeS.get(o).t2);
        }*/
        return listeS;
    }

    static int taskIndex(ResourceOrder order, Task task) {
        int index=0;
        for(int i =0; i<order.instance.numJobs;i++) {
            if(order.matrix[order.instance.machine(task)][i].equals(task)) {
                index = i;
                break;
            }
                
        }
        return (index);       
    }

}