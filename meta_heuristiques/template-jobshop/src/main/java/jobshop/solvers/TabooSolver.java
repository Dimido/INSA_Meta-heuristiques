package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class TabooSolver implements Solver{

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
            Task Two = order.matrix[machine][t2];

            //on updtate la matrix
            order.matrix[machine][t1]=Two;
            order.matrix[machine][t2]=One;
            //throw new UnsupportedOperationException();
        }
    }


    @Override
    public Result solve(Instance instance, long deadline) {
        //on recup le schedule
        Solver sol = new EST_LRPTGreedySolver();
        Result res = sol.solve(instance, deadline);
        List<Task> cp = res.schedule.criticalPath();
        
        //max iter
        int maxIter = 20000;
        //choix de la duree taboue
        int durreeTaboue = 20;
        //meilleur temps
        int m= res.schedule.makespan();
        //System.out.println("au début le best vaut :" + m);
        //resourceorder pour les évaluations
        ResourceOrder travail = new ResourceOrder(res.schedule);
        //resourceorder de sauvegarde
        ResourceOrder init = travail.copy();
        //solution meilleure
        ResourceOrder best = travail.copy();
        //on cree la liste des blocks
        List<Block> LB = blocksOfCriticalPath4(travail);
        //System.out.println("nombre de blocks: "+ LB.size());
        //tabeau des tabous
        int[][] sTabou= new int[cp.size()][cp.size()];
        for (int j=0;j<sTabou.length;j++){
            for (int i=0;i<sTabou[j].length;i++){
                sTabou[j][i]=0;                    
            }
        }
        //System.out.println("stabou size "+ sTabou.length);
        //pour trouver le meilleur voisin
        int min;
        //iteration integer
        int k = 0;
        

        

        while ( (k<maxIter) /*&& (deadline - System.currentTimeMillis() > 1)*/){
            k++;
            //System.out.println("taille de lb : " +LB.size());
            Swap current=null;
            min = 9999;
            for (int b=0;b<LB.size();b++){
                //System.out.println("taille de b : " +(b.lastTask - b.firstTask));
                List<Swap> LS = neighbors3(LB.get(b));
                
                //System.out.println("taille de ls : " +LS.size());
                
                for (Swap s:LS){
                    //recup les indices des taches i1 et i2
                    int i1=0, i2=0;
                    Task t1=travail.matrix[s.machine][s.t1];
                    Task t2=travail.matrix[s.machine][s.t2];
                    for (int t=0;t<cp.size();t++){
                        if (cp.get(t).job==t1.job && cp.get(t).task==t1.task){
                            i1=t;
                        }
                        if (cp.get(t).job==t2.job && cp.get(t).task==t2.task){
                            i2=t;
                        }
                    }
                    /*for (int j=0;j<sTabou.length;j++){
                        for (int i=0;i<sTabou[j].length;i++){
                            System.out.println("tabou des indices " +j + ","+ i + " = " + sTabou[j][i]);                  
                        }
                    }*/
                    //System.out.println("on considere le swap =" + i1 + ","+ i2 + " du bloc "+ b + "("+(LB.get(b).lastTask-LB.get(b).firstTask+1) + "taches) avec k="+ k + " et tabou =" + sTabou[i1][i2]);
                    if (sTabou[i1][i2]<=k){
                        //System.out.println("on evalue le swap ="+ i1 + ","+ i2 + " du bloc "+ b + " avec k="+ k + " et tabou =" + sTabou[i1][i2]);
                        ResourceOrder aux = travail.copy();
                        //System.out.println("copie de rsourceorder done");
                        s.applyOn(aux);
                        //System.out.println("pour swap ="+ i1 + ","+ i2 + "min = "+ min);
                        if (aux.toSchedule().makespan()<min){
                            min = aux.toSchedule().makespan();
                            //System.out.println("pour swap ="+ i1 + ","+ i2 + "nouveau min = "+ min);
                            current = s;
                        }
                    }
                }
            }
            //System.out.println("on a trouve le min = "+ min + " pour k=" + k);
            if (current==null){
                break;
            }else{
                ResourceOrder last = travail.copy();
                current.applyOn(last);
                m = last.toSchedule().makespan();
                //System.out.println("le best vaut :" + m);
                //recup indices
                int i1=0, i2=0;
                Task t1=travail.matrix[current.machine][current.t1];
                Task t2=travail.matrix[current.machine][current.t2];
                for (int t=0;t<cp.size();t++){
                    if (cp.get(t).job==t1.job && cp.get(t).task==t1.task){
                        i1=t;
                    }
                    if (cp.get(t).job==t2.job && cp.get(t).task==t2.task){
                        i2=t;
                    }
                }
                //System.out.println("on a choisi le swap ="+ i1 + ","+ i2 + " avec k="+ k);
                sTabou[i2][i1]=k+durreeTaboue;
                /*for (int j=0;j<sTabou.length;j++){
                    for (int i=0;i<sTabou[j].length;i++){
                        System.out.println("tabou des indices apres update " +j + ","+ i + " = " + sTabou[j][i]);                  
                    }
                }*/
                //System.out.println(" le best vaut :" + travail.toSchedule().makespan());
                if (last.toSchedule().makespan()<best.toSchedule().makespan()){
                    best=last;
                }
                travail=last;
                LB = blocksOfCriticalPath4(travail);
                //System.out.println("nouveau nombre de blocks: "+ LB.size());
            }
	    }
    
        return new Result(instance, best.toSchedule(), Result.ExitCause.Blocked);
        //throw new UnsupportedOperationException();
    }

    /** Returns a list of all blocks of the critical path. */
    List<Block> blocksOfCriticalPath4(ResourceOrder order) {
        List<Task> criticalPath = order.toSchedule().criticalPath();
        List<Block> listeB = new ArrayList<Block>();
        Task current, next;
        
        for(int i = 0; i<criticalPath.size()-1; i++) {
            current = criticalPath.get(i);
            next = criticalPath.get(i+1);
            
            if(listeB.size() != 0 && (listeB.get(listeB.size()-1).machine) == order.instance.machine(current)) {
                Block newBlock = new Block(order.instance.machine(current), listeB.get(listeB.size()-1).firstTask, taskIndex(order,current));
                listeB.remove(listeB.size()-1);
                listeB.add(newBlock);
            }
            else {
                if(order.instance.machine(current)== order.instance.machine(next)) {
                    listeB.add(new Block(order.instance.machine(current), taskIndex(order,current), taskIndex(order,next)));
                }
            }
        }
        current = criticalPath.get(criticalPath.size()-1);
        return listeB;
        //throw new UnsupportedOperationException();
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors3(Block block) {
        //on créé la liste de swaps à retourner
        List<Swap> listS = new ArrayList<Swap>();
        //on récup le nb de taches
        int nb = block.lastTask-block.firstTask+1;
        //on recup la machine
        int m=block.machine;


        //System.out.println(" block, t1 = "  + block.firstTask + ", t2 =" + block.lastTask);
        
        listS.add(new Swap(m, block.firstTask, block.firstTask+1));
	if (nb>2){
	    listS.add(new Swap(m,block.lastTask-1,block.lastTask));
	}
        /*System.out.println("voici la liste des swaps pour ce block");
        for (int o=0;o<listeS.size();o++){
            System.out.println("first : " +listeS.get(o).t1 + "," +listeS.get(o).t2);
        }*/
        return listS;
        //throw new UnsupportedOperationException();
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
