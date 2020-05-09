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

    /*@Override
    public Result solve(Instance instance, long deadline){


        ResourceOrder RO = new ResourceOrder(instance);
        List<Task> task= new ArrayList<Task>();
        for (int a=0;a<instance.numJobs;a++){
            task.add(new Task(a,0));
        }
        int[] vectjob = new int[instance.numJobs];
        for (int r=0;r<vectjob.length;r++){
            vectjob[r]=0;
        }
        int[] vectm = new int[instance.numMachines];
        for (int t=0;t<vectm.length;t++){
            vectm[t]=0;
        }
        int c=0;
        
        /////////////////////////////////////////////


        while(c<(instance.numJobs*instance.numTasks)){
            int minst=9999;
            Task tache=null;
            int machine = 0;
            for (Task t: task){
                int est = max(vectjob[t.job],vectm[instance.machine(t)]);
                if(est<=minst){
                    minst=est;
                }
            }
            
            int min=9999;
            tache=null;
            for (Task t: task){
                int est = max(vectjob[t.job], vectm[instance.machine(t)]);
                if(est==minst && instance.duration(t.job,t.task)<min){
                    min=instance.duration(t.job,t.task);
                    tache=t;
                    machine= instance.machine(tache);
                }
            }

            
            c++;
            RO.matrix[RO.nextFreeSlot[machine]][tache.job]=tache;
            RO.nextFreeSlot[machine]+=1;
            vectjob[tache.job]+=instance.duration(tache.job,tache.task);
            vectm[machine]+=instance.duration(tache.job,tache.task);
            task.remove(tache);
            if(tache.task != instance.numTasks-1) {
                task.add(new Task(tache.job, tache.task + 1));
            } 
        }
        System.out.print(RO.toString());
        return new Result(instance, RO.toSchedule(), Result.ExitCause.Blocked);
    }*/

    @Override
    public Result solve(Instance instance, long deadline) {

        ResourceOrder RO = new ResourceOrder(instance);
        //liste des taches pouvant être réalisées
        ArrayList<Task> task = new ArrayList<Task>();
        //liste des taches déja gérées, remplie dans le bon ordre
        //ArrayList<Task> task_r=new ArrayList<Task>();
        //a la place de task_r, un indice qui compte le nombre de taches gerees
        int compteur = 0;
        //minimum pour spt
        int min=0;
        //minimum pour est
        int minst=0;
        for(int j=0;j<instance.numJobs;j++){
            task.add(new Task(j,0));
            //System.out.println("nouvell tache au job "+ j);
        }
        
        //définir les durrées des taches
        //int[][] durees = new int[instance.numJobs][instance.numTasks];
        //for(int j=0;j<instance.numJobs;j++){
        //    for (int i=0;i<instance.numTasks;i++){
        //        //on ajoute la durée de la tache
        //        durees[j][i]=instance.duration(j,i);
        //    }
        //}

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
        //int[][] check = new int[instance.numJobs][instance.numTasks];
        //for(int e=0;e<instance.numJobs;e++){
        //    for(int f=0;f<instance.numTasks;f++){
        //        check[e][f]=0;
        //    }
        //} 

        while(compteur<(instance.numJobs*instance.numTasks)){ //task_r.size()==(instance.numJobs*instance.numTasks))
            //on cherche le min de start time et on update les vecteurs
            minst=9999;
            for (Task t:task){
                int starttime = max(vectjob[t.job], vectm[instance.machine(t)]);
                //System.out.println("start time de la tache : job "+ t.job + " task " + t.task + " = "+ starttime);
                if (starttime<=minst){
                    //if ((check[t.job][t.task])==0){
                        minst=starttime;
                    //}
                }
            }
            //System.out.println("starttime minimal = "+ minst);
            //on a le start time min
            //on construit la liste des taches qui vont etre évaluées
            //List<Task> lt = new ArrayList<Task>();
            //for (Task t:task){
            //    int start = max(vectjob[t.job], vectm[instance.machine(t)]);
            //    if(start==minst){
            //        lt.add(t);
                    //System.out.println("noouvelle tache ajoutee job "+ t.job + " tach "+ t.task);
            //    }
            //}
            //while(lt.size()!=0){
                //System.out.println("taille de la liste : "+ lt.size());
                //for (Task l:lt){
                //    System.out.println(" job "+ l.job + "tache " + l.task);
                //}
                //le maximum pour LRPT
                min=9999;
                //la tache équivalente
                Task tache = null, aux=null;
                //int pour la durée de la tache récupéree
                //int c=0;
                //récuperer le numéro de machine
                int machine =0;
                //String s="taches dans lt: ";
                for (Task t: task){//lt){
                    int start = max(vectjob[t.job], vectm[instance.machine(t)]);
                    //s+="job "+ Integer.toString(t.job) + " task " + Integer.toString(t.task);
                    if(start==minst && instance.duration(t.job, t.task)<min){
                        tache=t;
                        min=instance.duration(t.job, t.task);
                        //c=instance.duration(tache.job, tache.task);
                        machine=instance.machine(tache.job,tache.task);
                        //System.out.println("c = " + c);
                    }
                }
                //System.out.println(s);
                System.out.println("tache retenue job "+ tache.job + " task " + tache.task);
                //on update le temps de la tache
                //durees[tache.job][tache.task]=9999;
                //update t avec la duree de la tache
                //t+=c;
                //on ajoute la tache dans la liste des taches gérees et dans le jn
                //task_r.add(tache);
                compteur++;
                RO.matrix[machine][RO.nextFreeSlot[machine]]=tache;
                RO.nextFreeSlot[machine]+=1;
                vectjob[tache.job]+= instance.duration(tache.job, tache.task);
                vectm[instance.machine(tache)]+=instance.duration(tache.job, tache.task);
                //on la supprime des taches a réalisées (réalisables)
                task.remove(tache);
                //for(int y =0;y<instance.numJobs;y++){
                //    if (y==tache.job){
                //        System.out.println("update de vectjob pour job "+ y + " val ="+ vectjob[y]);
                //    } else {
                //        System.out.println("vectjob pour job "+ y + " val ="+ vectjob[y]);
                //    }
                //}
                //for(int y =0;y<instance.numMachines;y++){
                //    if(y==instance.machine(tache)){
                //        System.out.println("update de vectm pour machine "+ y + " val ="+ vectm[y]);
                //    }else{
                //        System.out.println("vectm pour machine "+ y + " val ="+ vectm[y]);
                //    }
                //}
                //lt.remove(tache);
                //check[tache.job][tache.task]=1;
                //vérifier quelles taches sont réalisables
                //pas besoin je crois, il suffit d'ajouter dans task la tache suivante du job qu'on vient de traiter
                //ajouter ces taches réalisables dans la liste task
                if(tache.task != instance.numTasks-1) {
                    task.add(new Task(tache.job, tache.task + 1));
                }                
            }

        //}
        //System.out.println(RO.toString());
       
        return new Result(instance, RO.toSchedule(), Result.ExitCause.Blocked);
    }

    private int max(int i, int j) {
        if (i<=j){
            return j;
        }else{
            return i;
        }
    }
    private int min(int i, int j) {
        if (i<=j){
            return i;
        }else{
            return j;
        }
    }
}