package jobshop.solvers;

import jdk.internal.loader.Resource;
import jobshop.Instance;
import jobshop.encodings.JobNumbers;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class SRMTGreedySolver implements Solver {


    @Override
    public Result solve(Instance instance, long deadline) {

        //JobNumbers jn = new JobNumbers(instance);
        ResourceOrder RO = new ResourceOrder(instance);

        //date de début (je sais pas si on en a besoin, je le garde pour les comparaisons) bas besoin finalement
        //int t=0;
        //liste des taches pouvant être réalisées
        ArrayList<Task> task = new ArrayList<Task>();
        //liste des taches déja gérées, remplie dans le bon ordre
        ArrayList<Task> task_r=new ArrayList<Task>();


        //on place toutes les premieres taches dans la liste de taches a regarder

        for (int j=0;j<instance.numJobs;j++) {
            task.add(new Task(j, 0));
            //System.out.println("tache ajoutée du job " + j);
        }


        //définir les durrées des machines!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        int[] durees = new int[instance.numMachines];
        for(int j=0;j<instance.numJobs;j++){
            for (int i=0;i<instance.numTasks;i++){
                //on ajoute la durée de la somme des taches sur la machine
                int machine = instance.machine(j,i);
                durees[machine]+=instance.duration(j,i);
            }
        }
        /*for (int k=0;k<durees.length;k++){
            System.out.println("duree machine " + k + " = " + durees[k]);
        }*/


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
            int m =0;
            /*for (int h=0;h<task.size();h++) {
                System.out.println("taches dans task : " + task.get(h).job + "," + task.get(h).task);
            }*/

            for (int a=0;a<task.size();a++){
                Task aux = task.get(a);
                m=instance.machine(aux.job,aux.task);
                if(durees[m]<min){
                    tache=aux;
                    min=durees[m];
                    c=instance.duration(tache.job, tache.task);
                    //m=instance.machine(tache.job,tache.task);
                }
            }
            int ma = instance.machine(tache.job,tache.task);
            //System.out.println("tache trouvéé "+ tache.job + "," + tache.task);
            //System.out.println(instance.machine(tache.job,tache.task) + " et m =" + ma);
            //on update le temps de la tache
            //System.out.println("duree tache = " + instance.duration(tache.job,tache.task) + " et c= "+ c);
            //System.out.println("ancienne duree de machine " + ma + durees[ma]);
            durees[ma]-=c;
            //System.out.println("nouvelle duree machine "+ ma + " = " + durees[ma]);
            //on verifie qu'on a pas fini avec la machine
            if (durees[ma]==0){
                durees[ma]=9999;
            }

            //on ajoute la tache dans la liste des taches gérees et dans le ro
            task_r.add(tache);
            //System.out.println("nb de taches réalisees =" +task_r.size() +"et total = "+instance.numJobs*instance.numTasks);
            //System.out.println("machine " + m + " tache " + tache.task + " du job " + tache.job);
            RO.matrix[ma][RO.nextFreeSlot[ma]]=tache;
            RO.nextFreeSlot[ma] += 1;
            //System.out.println("hello!");
            //on la supprime des taches a réalisées (réalisables)
            task.remove(tache);
            //ajouter ces taches réalisables dans la liste task
            if(!(tache.task == instance.numTasks-1)) {
                task.add(new Task(tache.job, tache.task + 1));
                //System.out.println("malaka");
            }

        }
        //System.out.println(RO.toString());

        //on stock ici le schedule obtenu
        Schedule best=RO.toSchedule();

        return new Result(instance, best, Result.ExitCause.Blocked);
    }
}
