package jobshop.solvers;

//import jdk.internal.loader.Resource;
import jobshop.Instance;
import jobshop.encodings.JobNumbers;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class SPTGreedySolver implements Solver {

    @Override
    public Result solve(Instance instance, long deadline) {

        //JobNumbers jn = new JobNumbers(instance);
        ResourceOrder RO = new ResourceOrder(instance);

        //date de début (je sais pas si on en a besoin, je le garde pour les comparaisons)
        int t=0;
        //liste des taches pouvant être réalisées
        ArrayList<Task> task = new ArrayList<Task>();
        //liste des taches déja gérées, remplie dans le bon ordre
        ArrayList<Task> task_r=new ArrayList<Task>();


        //on place toutes les premieres taches dans la liste de taches a regarder

        for (int j=0;j<instance.numJobs;j++) {
            task.add(new Task(j, 0));
        }


        //définir les durrées des taches
        int[][] durees = new int[instance.numJobs][instance.numTasks];
        for(int j=0;j<instance.numJobs;j++){
            for (int i=0;i<instance.numTasks;i++){
                //on ajoute la durée de la tache
                durees[j][i]=instance.duration(j,i);
            }
        }


        //tant qu'on a pas géré toutes les opérations
        while(!(task_r.size()==(instance.numJobs*instance.numTasks))){
            //le min pour SPT
            int min=9999;
            //int de sauvegarde pour l'update des durees (plus besoin je crois)
            //int b=0;
            //la tache équivalente
            Task tache = null;
            //int pour la durée de la tache récupéree
            int c=0;
            //récuperer le numéro de machine
            int machine =0;

            for (int a=0;a<task.size();a++){
                Task aux = task.get(a);
                if(durees[aux.job][aux.task]<min){
                    tache=aux;
                    min=durees[tache.job][tache.task];
                    c=instance.duration(tache.job, tache.task);
                    machine=instance.machine(tache.job,tache.task);
                }
            }
            //on update le temps de la tache
            durees[tache.job][tache.task]=9999;
            //update t avec la duree de la tache
            t+=c;
            //on ajoute la tache dans la liste des taches gérees et dans le jn
            task_r.add(tache);
            RO.matrix[machine][RO.nextFreeSlot[machine]]=tache;
            RO.nextFreeSlot[machine]+=1;
            //on la supprime des taches a réalisées (réalisables)
            task.remove(tache);
            //vérifier quelles taches sont réalisables
            //pas besoin je crois, il suffit d'ajouter dans task la tache suivante du job qu'on vient de traiter
            //ajouter ces taches réalisables dans la liste task
            if(!(tache.task == instance.numTasks-1)) {
                task.add(new Task(tache.job, tache.task + 1));
            }

        }

        //on stock ici le schedule obtenu
        Schedule best=RO.toSchedule();

        return new Result(instance, best, Result.ExitCause.Blocked);
    }
}
