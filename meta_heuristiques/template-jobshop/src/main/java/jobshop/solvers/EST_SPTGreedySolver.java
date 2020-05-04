package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class EST_SPTGreedySolver implements Solver {

    @Override
    public Result solve(Instance instance, long deadline) {

        ResourceOrder RO = new ResourceOrder(instance);
        //liste des taches pouvant être réalisées
        ArrayList<Task> task = new ArrayList<Task>();
        //liste des taches déja gérées, remplie dans le bon ordre
        ArrayList<Task> task_r=new ArrayList<Task>();
        for(int j=0;j<instance.numJobs;j++){
            task.add(new Task(j,0));
            //System.out.println("nouvell tache au job "+ j);
        }
        
        //définir les durrées des taches
        int[][] durees = new int[instance.numJobs][instance.numTasks];
        for(int j=0;j<instance.numJobs;j++){
            for (int i=0;i<instance.numTasks;i++){
                //on ajoute la durée de la tache
                durees[j][i]=instance.duration(j,i);
            }
        }

        //vecteurs des temps des jobs
        int[] vectjob = new int[instance.numJobs];
        for(int d=0;d<vectjob.length;d++){
            vectjob[d]=0;
        }
        //vecteurs des temps des machines
        int[] vectm = new int[instance.numMachines];
        for(int d=0;d<vectm.length;d++){
            vectm[d]=0;
        }  
        //tableau pour check si on a déja traité la tache
        int[][] check = new int[instance.numJobs][instance.numTasks];
        for(int e=0;e<instance.numJobs;e++){
            for(int f=0;f<instance.numTasks;f++){
                check[e][f]=0;
            }
        } 

        while(!(task_r.size()==(instance.numJobs*instance.numTasks))){
            //on cherche le min de start time et on update les vecteurs
            int minst=9999;
            for (Task t:task){
                int starttime = max(vectjob[t.job], vectm[instance.machine(t)]);
                if (starttime<minst){
                    if ((check[t.job][t.task])==0){
                        minst=starttime;
                    }
                }
            }
            //System.out.println("starttime minimal = "+ minst);
            //on le min
            //on construit la liste des taches qui vont etre évaluées
            List<Task> lt = new ArrayList<Task>();
            for (Task t:task){
                int start = max(vectjob[t.job], vectm[instance.machine(t)]);
                if(start==minst){
                    lt.add(t);
                    //System.out.println("noouvelle tache ajoutee job "+ t.job + " tach "+ t.task);
                }
            }
            //while(lt.size()!=0){
                //System.out.println("taille de la liste : "+ lt.size());
                /*for (Task l:lt){
                    System.out.println(" job "+ l.job + "tache " + l.task);
                }*/
                //le maximum pour LRPT
                int min=9999;
                //la tache équivalente
                Task tache = null, aux=null;
                //int pour la durée de la tache récupéree
                int c=0;
                //récuperer le numéro de machine
                int machine =0;
                for (Task t: lt){
                    aux=t;
                    if(durees[aux.job][aux.task]<min){
                        tache=aux;
                        min=durees[tache.job][tache.task];
                        c=instance.duration(tache.job, tache.task);
                        machine=instance.machine(tache.job,tache.task);
                    }
                }
                //System.out.println("nouvell tache retenue job "+ tache.job + " task " + tache.task);
                //on update le temps de la tache
                durees[tache.job][tache.task]=9999;
                //update t avec la duree de la tache
                //t+=c;
                //on ajoute la tache dans la liste des taches gérees et dans le jn
                task_r.add(tache);
                RO.matrix[machine][RO.nextFreeSlot[machine]]=tache;
                RO.nextFreeSlot[machine]+=1;
                vectjob[tache.job]+= instance.duration(tache.job, tache.task);
                vectm[instance.machine(tache)]+=instance.duration(tache.job, tache.task);
                //on la supprime des taches a réalisées (réalisables)
                task.remove(tache);
                //lt.remove(tache);
                check[tache.job][tache.task]=1;
                //vérifier quelles taches sont réalisables
                //pas besoin je crois, il suffit d'ajouter dans task la tache suivante du job qu'on vient de traiter
                //ajouter ces taches réalisables dans la liste task
                if(tache.task != instance.numTasks-1) {
                    task.add(new Task(tache.job, tache.task + 1));
                }                
            }

        //}
       
        return new Result(instance, RO.toSchedule(), Result.ExitCause.Blocked);
    }

    private int max(int i, int j) {
        if (i<=j){
            return j;
        }else{
            return i;
        }
    }
}