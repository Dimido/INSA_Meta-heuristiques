package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.Arrays;

/** Représentation par numéro de job. */
public class JobNumbers extends Encoding {

    /** A numJobs * numTasks array containing the representation by job numbers. */
    public final int[] jobs;

    /** In case the encoding is only partially filled, indicates the index of first
     * element of `jobs` that has not been set yet. */
    public int nextToSet = 0;

    public JobNumbers(Instance instance) {
        super(instance);

        jobs = new int[instance.numJobs * instance.numMachines];
        Arrays.fill(jobs, -1);
    }

    @Override
    public Schedule toSchedule() {
        // time at which each machine is going to be freed
        int[] nextFreeTimeResource = new int[instance.numMachines];

        // for each job, the first task that has not yet been scheduled
        int[] nextTask = new int[instance.numJobs];

        // for each task, its start time
        int[][] startTimes = new int[instance.numJobs][instance.numTasks];

        // compute the earliest start time for every task of every job
        for(int job : jobs) {
            int task = nextTask[job];
            int machine = instance.machine(job, task);
            // earliest start time for this task
            int est = task == 0 ? 0 : startTimes[job][task-1] + instance.duration(job, task-1);
            est = Math.max(est, nextFreeTimeResource[machine]);

            startTimes[job][task] = est;
            nextFreeTimeResource[machine] = est + instance.duration(job, task);
            nextTask[job] = task + 1;
        }

        return new Schedule(instance, startTimes);
    }

    //ce serait un nouveau constructeur!
    public JobNumbers fromSchedule(Schedule s){
        JobNumbers jn = new JobNumbers(s.pb);

        //recreer la matrice des start times
        int[][] aux = new int[s.pb.numJobs][s.pb.numTasks];
        for (int k=0; k<s.pb.numJobs;k++){
            for (int o=0;o<s.pb.numTasks;o++) {
                aux[k][o]=s.startTime(k,o);
            }
        }

        boolean done= false;
        int start_time=0;
        int l=0,c=0;
        int compter =0;
        int min =9999;
        //comter les cellules deja etudiées
        int cc = 0;
        int mult = s.pb.numJobs * s.pb.numTasks;

        //boucle while a faire
        while (cc<mult) {
            for (int j = 0; j < s.pb.numJobs; j++) {
                for (int i = 0; i < s.pb.numTasks; i++) {
                    start_time = aux[j][i];
                    if (start_time <= min) {
                        min = start_time;
                        l = j;
                        c = i;
                    }
                }
            }
            aux[l][c]=9999;
            jn.jobs[cc]=l;
        }

        return jn;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOfRange(jobs,0, nextToSet));
    }
}
