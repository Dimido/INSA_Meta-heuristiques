package jobshop.solvers;

//import jdk.internal.loader.Resource;
import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DescentSolver implements Solver {

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
    
    private Solver sol;
    public DescentSolver(String s){
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
    public Result solve(Instance instance, long deadline) {


        //on recup le schedule
        Solver sol = this.sol;
        Result res = sol.solve(instance, deadline);
        //Schedule sinit = res.schedule;

        //meilleur temps
        //int best= sinit.makespan();
        //System.out.println("au début le best vaut :" + best);
        //resourceorder pour les évaluations
        ResourceOrder travail = new ResourceOrder(res.schedule);
        //resourceorder de sauvegarde
        ResourceOrder init = travail.copy();
        //on cree la liste des blocks
        List<Block> LB = blocksOfCriticalPath3(travail);
        //List<Block> LB = blocksOfCriticalPath(travail);
        //List<Block> LB2 = blocksOfCriticalPath2(travail);
        //boolean pour savoir si on a fini
        boolean trouve = false;
        

        while ((!trouve) && (deadline - System.currentTimeMillis() > 1)){
            trouve=true;
		//System.out.println("taille de lb : " +LB.size());
		for (Block b : LB){
			//System.out.println("taille de b : " +(b.lastTask - b.firstTask));
			List<Swap> LS = neighbors2(b);
			//System.out.println("taille de ls : " +LS.size());
			for (Swap s:LS){
				ResourceOrder aux = travail.copy();
				//System.out.println("copie de rsourceorder done");
				s.applyOn(aux);
				//System.out.println("swap appliqué");
				if (aux.toSchedule().makespan() < init.toSchedule().makespan()){
                    init=aux;
                    trouve=false;
					//System.out.println("nouveau best trouvé : " + aux.toSchedule().makespan());
				}
			}
		}
		//System.out.println(" le best vaut :" + travail.toSchedule().makespan());
		travail=init;
		LB = blocksOfCriticalPath3(travail);
	}
            
        return new Result(instance, init.toSchedule(), Result.ExitCause.Blocked);
        //throw new UnsupportedOperationException();
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

    /** Returns a list of all blocks of the critical path. */
    public List<Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Task> tl  = order.toSchedule().criticalPath();
        LinkedList<Block> res = new LinkedList<Block>();
        int lastMachine = order.instance.machine(tl.get(0).job,tl.get(0).task);
        Task firstTask  = tl.get(0);
        int index = 0;
        for (int a = 1; a < tl.size(); a++){
            if(order.instance.machine(tl.get(a).job, tl.get(a).task) == lastMachine){
                index++;
            }else if(index > 0){
                int ind = 0;
                boolean found = false;
                while(ind < order.instance.numJobs && !found){
                    if(order.matrix[lastMachine][ind].task == firstTask.task && order.matrix[lastMachine][ind].job == firstTask.job){
                        res.addLast(new Block(lastMachine,ind,ind+index));
                        found = true;
                    }
                    ind ++;
                }
                index = 0;
                firstTask = tl.get(a);
                lastMachine = order.instance.machine(tl.get(a).job,tl.get(a).task);
            }else{
                index = 0;
                firstTask = tl.get(a);
                lastMachine = order.instance.machine(tl.get(a).job,tl.get(a).task);
            }
        }
        if(index > 0){
            int ind = 0;
            boolean found = false;
            while(ind < order.instance.numJobs && !found){
                if(order.matrix[lastMachine][ind].task == firstTask.task && order.matrix[lastMachine][ind].job == firstTask.job){
                    res.addLast(new Block(lastMachine,ind,ind+index));
                    found = true;
                }
                ind ++;
            }
        }
        for (Block b: res){
            System.out.println("block sur machine val "+ b.machine);
        }
        return res;
    }

    List<Block> blocksOfCriticalPath2(ResourceOrder order) {
        //liste des block initialisée vide
        List<Block> LB = new ArrayList<Block>();
        //chemin critique
        List<Task> cp = order.toSchedule().criticalPath();
        //integers pointeurs
        int first  = 0, last=1;
        //compteur on commence a 1
        int c=1;
        //indexes pour retrouver les taches dans le resourceorder
        int indexf=0,indexl=0;
        //int des machines pour simplififer l'écriture
        int mf=0,ml=0;
        //boolean pour savoir quand s'arreter
        boolean trouve=false;

        //initialisation, on recup machines et index de first, pas encore besoin de indexl
        mf=order.toSchedule().pb.machine(cp.get(first).job,cp.get(first).task);
        //System.out.println("machine de first = "+ mf);
        ml=order.toSchedule().pb.machine(cp.get(last).job,cp.get(last).task);
        //System.out.println("machine de last = "+ ml);
        indexf = indexof(order, cp, first, mf);

        while (!trouve){
            
            if (mf==ml){
                if (last == cp.size()-1){
                    trouve=true;
                    indexl = indexof(order, cp, last, mf);
                    LB.add(new Block(mf, indexf, indexl));
                } else{
                    c++;
                    last++;
                }
            }else{
                if (c>1){
                    if (last==cp.size()-1){
                        trouve=true;
                    }
                    indexl = indexof(order, cp, last-1, mf);
                    LB.add(new Block(mf, indexf, indexl));
                    first=last;
                    last++;
                    c=1;
                    mf=order.toSchedule().pb.machine(cp.get(first).job,cp.get(first).task);
                    ml=order.toSchedule().pb.machine(cp.get(last).job,cp.get(last).task);
                    indexf = indexof(order, cp, first, mf);
                }else {
                    if (last==cp.size()-1){
                        trouve=true;
                    }
                    first=last;
                    last++;
                    c=1;
                    mf=order.toSchedule().pb.machine(cp.get(first).job,cp.get(first).task);
                    ml=order.toSchedule().pb.machine(cp.get(last).job,cp.get(last).task);
                    indexf = indexof(order, cp, first, mf);
                }
                
            }
        }
        /*for (Block b: LB){
            System.out.println("block sur machine dimi "+ b.machine);
        }*/
        return LB;
    }

    /** Returns a list of all blocks of the critical path. */
    /*List<Block> blocksOfCriticalPath(ResourceOrder order) {
        //on cherche le schedule car on a besoin d'obtenir le chemin critique
        ResourceOrder Ro = order.copy();
        Schedule s= Ro.toSchedule();
        List<Task> listeT= s.criticalPath();

        //on a besoin de stocker le tout dans une liste de blocks
        List<Block> listeB = new ArrayList<Block>();

        //on prend des entiers qui représentent les indices de la liste, pour construire les blocks
        //on initialise a 0 et 1
        int first, last;
        first=0; last=1;
        //on compte le nombre de taches dans le block
        //on initialize a 1 car une tache seule est dans son propre block, même si elle n'est pas considérée comme un block
        int compt=1;
        //System.out.println("taille  : " +listeT.size());
        //int pour avoir l'index de la tache dans RO
        int indexf = 0, indexl=0;

        //on itère tant que first pointe au dernier element de la listeT
        while (first<(listeT.size()-1)){
        /*System.out.println("first : " +first);
        System.out.println("last : " +last);
        System.out.println("compt : " +compt);
        System.out.println("size : " +listeB.size()+"----------");
	        if (last==(listeT.size()-1) && first<listeT.size()-2){
		        if (s.pb.machine(listeT.get(first).job,listeT.get(first).task)==s.pb.machine(listeT.get(last).job,listeT.get(last).task)){
                    indexf = indexof(order, listeT, first, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                    indexl = indexof(order, listeT, last, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                    listeB.add(new Block(s.pb.machine( listeT.get(first).job,listeT.get(first).task),indexf, indexl));
                    first=last;
                }else { 
                    indexf = indexof(order, listeT, first, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                    indexl =indexof(order, listeT, last-1, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                    listeB.add(new Block(s.pb.machine( listeT.get(first).job,listeT.get(first).task),first, indexl));
                    first=last;
		        }
	        }else if (last==(listeT.size()-1) && first==listeT.size()-2){
                if (s.pb.machine(listeT.get(first).job,listeT.get(first).task)==s.pb.machine(listeT.get(last).job,listeT.get(last).task)){
                    indexf = indexof(order, listeT, first, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                    indexl = indexof(order, listeT, last, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                    listeB.add(new Block(s.pb.machine( listeT.get(first).job,listeT.get(first).task),indexf, indexl));
                    first=last;
                }else { 
                    first=last;
                }
	        }else{
		
            if (s.pb.machine(listeT.get(first).job,listeT.get(first).task)==s.pb.machine(listeT.get(last).job,listeT.get(last).task)){
                compt++;
                indexf = indexof(order, listeT, first, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                //first++;
                last++;
            } else {
                if (compt>1){
                    indexl=indexof(order, listeT, last-1, s.pb.machine(listeT.get(first).job,listeT.get(first).task));
                    listeB.add(new Block(s.pb.machine( listeT.get(first).job,listeT.get(first).task),indexf, indexl));
                    compt=1;
                    first=last;
                    last=first+1;
                } else {
                    compt=1;
                    first=last;
                    last=first+1;
                }

            }
	}
        }
        /*System.out.println("final size : " + listeB.size());
        for (int f=0;f<listeB.size();f++){
            System.out.println("block " + f + " avec t1 = " + listeB.get(f).firstTask + "et t2 =" + listeB.get(f).lastTask);
        }
        return listeB;
        //throw new UnsupportedOperationException();
    }*/

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        //on créé la liste de swaps à retourner
        List<Swap> listeS = new ArrayList<Swap>();
        //on récup le nb de taches
        int nb = block.lastTask-block.firstTask+1;
        //on recup la machine
        int m=block.machine;
        int i=0;

        //part one -> swap the first elements two by two
        while (i<nb-1){
            listeS.add(new Swap(m,i,i+1));
            i+=2;
        }
	
        //part two -> swap with an offset of 1
        i=1;
        while (i<nb-1){
            listeS.add(new Swap(m,i,i+1));
            i+=2;
        }

        return listeS;
        //throw new UnsupportedOperationException();
    }

    List<Swap> neighbors2(Block block) {
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

    int indexof(ResourceOrder order, List<Task> l, int index, int machine){
        int mi=0;
        for (int i=0;i<order.matrix[machine].length;i++){
            if (l.get(index).job==order.matrix[machine][i].job && l.get(index).task==order.matrix[machine][i].task){
                mi=i;
            }
        }
        return mi;
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
