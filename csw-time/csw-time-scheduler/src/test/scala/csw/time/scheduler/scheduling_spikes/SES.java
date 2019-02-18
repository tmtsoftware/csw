package csw.time.scheduler.scheduling_spikes;

import java.time.Instant;
import java.util.concurrent.*;

public class SES {

    public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

        Runnable callable = () -> {
            Instant instant = Instant.now();
            System.out.println("Beep! -> " + instant + " :" + instant.toEpochMilli());
        };

        ScheduledFuture scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(callable, 0, 5, TimeUnit.MILLISECONDS);
    }
}
