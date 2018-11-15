package csw.time.client.scheduling_spikes;

public class SleepTest {
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            long start = System.nanoTime();
            Thread.sleep(0, 10000);
            System.out.println(System.nanoTime() - start + "---" + (System.nanoTime() - start) / 1000000);
        }
    }
}
