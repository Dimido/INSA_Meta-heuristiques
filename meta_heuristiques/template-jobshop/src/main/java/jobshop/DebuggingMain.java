package jobshop;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
import jobshop.solvers.LRPTGreedySolver;
import jobshop.solvers.RandomSolver;
import jobshop.solvers.SPTGreedySolver;

import java.io.IOException;
import java.nio.file.Paths;

public class DebuggingMain {

    public static void main(String[] args) {
        try {
            // load the aaa1 instance
            Instance instance = Instance.fromFile(Paths.get("instances/aaa1"));
            System.out.println("nombre de machines : " + instance.numMachines);
            // construit une solution dans la représentation par
            // numéro de jobs : [0 1 1 0 0 1]
            // Note : cette solution a aussi été vue dans les exercices (section 3.3)
            //        mais on commençait à compter à 1 ce qui donnait [1 2 2 1 1 2]
            JobNumbers enc = new JobNumbers(instance);
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            ResourceOrder ro = new ResourceOrder(instance);
            ro.matrix[0][0]= new Task(0,0);
            ro.matrix[0][1]= new Task(1,1);
            ro.matrix[1][0]= new Task(1,0);
            ro.matrix[1][1]= new Task(0,1);
            ro.matrix[2][0]= new Task(0,2);
            ro.matrix[2][1]= new Task(1,2);

            System.out.println("\nENCODING: " + enc);

            Schedule sched = enc.toSchedule();
            // TODO: make it print something meaningful
            // by implementing the toString() method
            sched.toString();
            System.out.println("noww with ro:");
            Schedule s= ro.toSchedule();
            s.toString();

            //ResourceOrder ro1 = null;
            //ro1=ro1.fromSchedule(s);
            //Schedule s2 = ro1.toSchedule();
            //s2.toString();

            //System.out.println("SCHEDULE: " + sched);
            System.out.println("VALID: " + sched.isValid());
            System.out.println("MAKESPAN: " + sched.makespan());

            /*long dealine = 10000000;
            Solver solv = new LRPTGreedySolver();
            Result res = solv.solve(instance, dealine);
            res.schedule.toString();*/


        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
