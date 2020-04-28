package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;


import java.util.Arrays;

public class ResourceOrder extends Encoding {

    //matrix
    public final Task[][] matrix;

    public final int[] nextFreeSlot;

    public ResourceOrder(Instance instance) {
        super(instance);

        matrix = new Task[instance.numMachines][instance.numJobs];

        nextFreeSlot = new int[instance.numMachines];
    }

    /** Creates a resource order from a schedule. */
    public ResourceOrder(Schedule schedule)
    {
        super(schedule.pb);
        Instance pb = schedule.pb;

        this.matrix = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        for(int m = 0 ; m<schedule.pb.numMachines ; m++) {
            final int machine = m;

            // for thi machine, find all tasks that are executed on it and sort them by their start time
            matrix[m] =
                    IntStream.range(0, pb.numJobs) // all job numbers
                            .mapToObj(j -> new Task(j, pb.task_with_machine(j, machine))) // all tasks on this machine (one per job)
                            .sorted(Comparator.comparing(t -> schedule.startTime(t.job, t.task))) // sorted by start time
                            .toArray(Task[]::new); // as new array and store in tasksByMachine

            // indicate that all tasks have been initialized for machine m
            nextFreeSlot[m] = instance.numJobs;
        }
    }

    /*@Override
    public Schedule toSchedule() {
        // time at which each machine is going to be freed
        int[] nextFreeTimeResource = new int[instance.numMachines];

        // for each job, the first task that has not yet been scheduled
        int[] nextTask = new int[instance.numJobs];

        // for each task, its start time
        int[][] startTimes = new int[instance.numJobs][instance.numTasks];

        // compute the earliest start time for every task of every job
        for(int row=0;row<matrix.length;row++) {
            for (int col=0;col<matrix[row].length;col++){
                Task t= matrix[row][col];
                //System.out.println("tache :" + t.job + ", " + t.task);
                int machine = instance.machine(t.job,t.task);

                // earliest start time for this task
                int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
                est = Math.max(est, nextFreeTimeResource[machine]);

                startTimes[t.job][t.task] = est;
                nextFreeTimeResource[machine] = est + instance.duration(t.job, t.task);
                nextTask[t.job] = t.task + 1;
            }
               return new Schedule(instance, startTimes);
    }
        }*/

        @Override
        public Schedule toSchedule() {
            // indicate for each task that have been scheduled, its start time
            int [][] startTimes = new int [instance.numJobs][instance.numTasks];

            // for each job, how many tasks have been scheduled (0 initially)
            int[] nextToScheduleByJob = new int[instance.numJobs];

            // for each machine, how many tasks have been scheduled (0 initially)
            int[] nextToScheduleByMachine = new int[instance.numMachines];

            // for each machine, earliest time at which the machine can be used
            int[] releaseTimeOfMachine = new int[instance.numMachines];


            // loop while there remains a job that has unscheduled tasks
            while(IntStream.range(0, instance.numJobs).anyMatch(m -> nextToScheduleByJob[m] < instance.numTasks)) {

                // selects a task that has noun scheduled predecessor on its job and machine :
                //  - it is the next to be schedule on a machine
                //  - it is the next to be scheduled on its job
                // if there is no such task, we have cyclic dependency and the solution is invalid
                Optional<Task> schedulable =
                        IntStream.range(0, instance.numMachines) // all machines ...
                                .filter(m -> nextToScheduleByMachine[m] < instance.numJobs) // ... with unscheduled jobs
                                .mapToObj(m -> this.matrix[m][nextToScheduleByMachine[m]]) // tasks that are next to schedule on a machine ...
                                .filter(task -> task.task == nextToScheduleByJob[task.job])  // ... and on their job
                                .findFirst(); // select the first one if any

                if(schedulable.isPresent()) {
                    // we found a schedulable task, lets call it t
                    Task t = schedulable.get();
                    int machine = instance.machine(t.job, t.task);

                    // compute the earliest start time (est) of the task
                    int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
                    est = Math.max(est, releaseTimeOfMachine[instance.machine(t.job,t.task)]);
                    startTimes[t.job][t.task] = est;

                    // mark the task as scheduled
                    nextToScheduleByJob[t.job]++;
                    nextToScheduleByMachine[machine]++;
                    // increase the release time of the machine
                    releaseTimeOfMachine[machine] = est + instance.duration(t.job, t.task);
                } else {
                    // no tasks are schedulable, there is no solution for this resource ordering
                    return null;
                }
            }
            // we exited the loop : all tasks have been scheduled successfully
            return new Schedule(instance, startTimes);
        }




    /*//ce serait un nouveau constructeur!
    public ResourceOrder fromSchedule(Schedule s){
        ResourceOrder ro = new ResourceOrder(s.pb);

        //tableau qui compte pour chaque machine la colone a ajouter une tache
        int[] compt = new int[s.pb.numMachines];
        //on le rempli a 0
        Arrays.fill(compt,0);

        Task t;
        int start_time;
        int l=0,c=0;
        int min = 9999;
        boolean done = false;
        //comter les cellules deja etudiées
        int cc = 0;
        int mult = s.pb.numJobs * s.pb.numTasks;

        //recreer la matrice des start times
        int[][] aux = new int[s.pb.numJobs][s.pb.numTasks];
        for (int k=0; k<s.pb.numJobs;k++){
            for (int o=0;o<s.pb.numTasks;o++) {
                aux[k][o]=s.startTime(k,o);
            }
        }

        //boucle while a faire
        while (!done) {
            for (int j = 0; j < s.pb.numJobs; j++) {
                for (int i = 0; i < s.pb.numTasks; i++) {
                    start_time = aux[j][i];
                    if (start_time <= min) {
                        min = start_time;
                        l = j;
                        c = i;
                    }
                    if (aux[j][i]==9999){
                        cc++;
                    }
                }
            }
            //on recupere la tache avec la plus petite start time
            t = new Task(l, c);
            int b = s.pb.machine(l, c);
            aux[l][c] = 9999;
            ro.matrix[b][compt[b]] = t;
            compt[b]++;
            if (cc==mult){
                done = true;
            }
        }


        return ro;
    } c'était mon fromSchedule */

    /** Creates an exact copy of this resource order. */
    public ResourceOrder copy() {
        return new ResourceOrder(this.toSchedule());
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for(int m=0; m < instance.numMachines; m++)
        {
            s.append("Machine ").append(m).append(" : ");
            for(int j=0; j<instance.numJobs; j++)
            {
                s.append(matrix[m][j]).append(" ; ");
            }
            s.append("\n");
        }

        return s.toString();
    }
}
