package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class LRPTgreedySolver implements Solver {

    @Override
    public Result solve(Instance instance, long deadline) {
        ResourceOrder RO = new ResourceOrder(instance);
	//date de début (je sais pas si on en a besoin, je le garde pour les comparaisons)
        int t=0;
        //liste des taches pouvant être réalisées
        ArrayList<Task> task = new ArrayList<Task>();
        //liste des taches déja gérées, remplie dans le bon ordre
        ArrayList<Task> task_r=new ArrayList<Task>();
        
	//en vrai il faut initialiser avec l'ordre de passage, placer toutes les premieres taches
        for(int j=0;j<instance.numJobs;j++){
            task.add(new Task(j,0));
        }

        //définir les durrées des jobs
        int[] durees = new int[instance.numJobs];
        for(int j=0;j<instance.numJobs;j++){
            int somme =0;
            for (int i=0;i<instance.numTasks;i++){
                //on ajoute la durée de la tache
                somme+=instance.duration(j,i);
            }
            durees[j]=somme;
        }

        //debug
        /*for (int g=0;g<durees.length;g++){
            System.out.println(g + ": " + durees[g]);
        }
        System.out.println("done");*/


        //tant qu'on a pas géré toutes les opérations
        while(!(task_r.size()==(instance.numJobs*instance.numTasks))){
            //le maximum pour LRPT
            int max=0;
            //int de sauvegarde pour l'update des durees
            int b=0;
            //la tache équivalente
            Task tache=null,aux = null;
            //int pour la durée de la tache récupéree
            int c=0;
            //récuperer le numéro de machine (pas besoin je crois)
            int machine =0;

            for (int a=0;a<task.size();a++){
                aux=task.get(a);
                //System.out.println("num job = " + aux.job);
                if(durees[aux.job]>max){
                    tache=aux;
                    max=durees[task.get(a).job];
                    b=aux.job;
                    c=instance.duration(tache.job, tache.task);
                    machine=instance.machine(tache.job,tache.task);
                }
            }
            //on update le temps restant du job
            durees[b]-=c;
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


        Schedule best = RO.toSchedule();

        return new Result(instance, best, Result.ExitCause.Blocked);

    }

}

