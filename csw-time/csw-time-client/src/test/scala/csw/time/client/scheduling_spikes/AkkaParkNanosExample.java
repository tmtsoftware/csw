package csw.time.client.scheduling_spikes;

import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

public class AkkaParkNanosExample {

    private static ArrayList<Long> buf = new ArrayList<>();
    private static int numWarningBeeps = 1000;

    public static void main(String[] args) {
        long nanos =  10 * 1000 * 1000;
        while (numWarningBeeps > 0) {
            LockSupport.parkNanos(nanos);
            buf.add(System.nanoTime());
//            TimeSpec timeSpec = new TimeSpec();
//            TimeLibrary.clock_gettime(0, timeSpec);
//            long s = timeSpec.seconds;
//            String n = String.format("%09d", timeSpec.nanoseconds);
//            buf.add(s+""+n);
            numWarningBeeps -= 1;
        }

        for(int i=0; i<1000; i++){
            System.out.println(buf.get(i));
        }
    }
}
