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
        //situation initiale
        //on recup le schedule
        Solver sol = new RandomSolver();
        Result res = sol.solve(instance, deadline);
        Schedule init = res.schedule;
        ResourceOrder Ro = new ResourceOrder(init);
        int makespan = init.makespan();

        Schedule aux = null;

        //on cree le critical path
        List<Task> LT = init.criticalPath();

        //on cree la liste des blocks
        List<DescentSolver.Block> LB = blocksOfCriticalPath(Ro);

        if (LB.size()==0){
            return new Result(instance, init, Result.ExitCause.Blocked);
        } else {

            //valeur de maxIter arbitraire
            int maxIter = 10;

            //boucle
            int k = 0;

            while (k < maxIter) {
                k+=1;
                //on cherche le makespan minimal
                int min = 9999;

                //liste des swap possibles pour le schedule
                List<DescentSolver.Swap> LS = new ArrayList<DescentSolver.Swap>();

                for (int a = 0; a < LB.size(); a++) {
                    LS.addAll(neighbors(LB.get(a)));
                }
                //pour stocker le swap
                DescentSolver.Swap swap = null;

                //pour tous les swap trouvés, on trouve celui qui a le makespan minimal
                for(int b=0;b<LS.size();b++){
                    LS.get(b).applyOn(Ro);
                    aux=Ro.toSchedule();
                    if(aux.makespan()<min){
                        min = aux.makespan();
                        swap=LS.get(b);
                    }
                }

                swap.applyOn(Ro);
                aux=Ro.toSchedule();
                if (aux.makespan()<init.makespan()){
                    init = aux;
                    //on update les listes de blocks et de swap
                    LT = init.criticalPath();
                    //update les blocks
                    LB = blocksOfCriticalPath(Ro);

                    if (LB.size()==0){
                        return new Result(instance, init, Result.ExitCause.Blocked);
                    }
                }
            }
        }

        return new Result(instance, init, Result.ExitCause.Blocked);
        //throw new UnsupportedOperationException();
    }

    /** Returns a list of all blocks of the critical path. */
    List<DescentSolver.Block> blocksOfCriticalPath(ResourceOrder order) {
        //on cherche le schedule car on a besoin d'obtenir le chemin critique
        Schedule s= order.toSchedule();
        List<Task> listeT= s.criticalPath();

        //on a besoin de stocker le tout dans une liste de blocks
        List<DescentSolver.Block> listeB = new ArrayList<DescentSolver.Block>();

        //on prend des entiers qui représentent les indices de la liste, pour construire les blocks
        //on initialise a 0 et 1
        int first, last;
        first=0; last=1;
        //on compte le nombre de taches dans le block
        //on initialize a 1 car une tache seule est dans son propre block, même si elle n'est pas considérée comme un block
        int compt=1;

        //on itère tant que first pointe au dernier element de la listeT
        while (first<listeT.size()-1){
            if (listeT.get(first)==listeT.get(last)){
                compt++;
                first++;
                last++;
            } else {
                if (compt>1){
                    listeB.add(new DescentSolver.Block(s.pb.machine( listeT.get(first).job,listeT.get(first).task),last-compt, last));
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

        return listeB;
        //throw new UnsupportedOperationException();
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<DescentSolver.Swap> neighbors(DescentSolver.Block block) {
        //on créé la liste de swaps à retourner
        List<DescentSolver.Swap> listeS = new ArrayList<DescentSolver.Swap>();
        //on récup le nb de taches
        int nb = block.lastTask-block.firstTask;
        //on recup la machine
        int m=block.machine;
        int i=0;

        //part one -> swap the first elements two by two
        while (i<nb-1){
            listeS.add(new DescentSolver.Swap(m,i,i+1));
            i+=2;
        }

        //part two -> swap with an offset of 1
        i=1;
        while (i<nb-1){
            listeS.add(new DescentSolver.Swap(m,i,i+1));
            i+=2;
        }

        return listeS;
        //throw new UnsupportedOperationException();
    }

}