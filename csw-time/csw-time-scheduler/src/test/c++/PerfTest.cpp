#include "ScanTask.h"
#include <ctime>
#include <cstdio>
#include <cmath>

// A task that measures how accurate the scheduler is and prints out the jitter values.
class MyTask : public ScanTask {
private:
    const char *name;
    timespec startTime{};
    timespec ts{};
    const int intervalMs;
    const double intervalNanos;
    long count = 0;
    long jitterMicrosecs = 0;

    // Override this method to do the work every interval ms
    void scan() override {
        clock_gettime(CLOCK_REALTIME, &ts);
        count++;
        const double t1Nanos = double(startTime.tv_sec) * 1000.0 * 1000.0 * 1000.0 + double(startTime.tv_nsec);
        const double t2Nanos = double(ts.tv_sec) * 1000.0 * 1000.0 * 1000.0 + double(ts.tv_nsec);
//        printf("XXX %s: time (ms): %ld\n", name, long(t2Nanos - t1Nanos)/(1000*1000));
        long diffMicrosecs = long(fabs(((t2Nanos - t1Nanos) - intervalNanos) / 1000));
        if (count > 1000/intervalMs)
            jitterMicrosecs = (jitterMicrosecs * (count - 1) + diffMicrosecs) / count;
        if (count % (1000 / intervalMs) == 0) {
            printf("%s (%d ms): jitter = %ld microsecs (%ld ms)\n", name, intervalMs, jitterMicrosecs, jitterMicrosecs/1000L);
        }
        clock_gettime(CLOCK_REALTIME, &startTime);
    }

public:
    MyTask(const char *name, int intervalMs) :
            ScanTask(name, intervalMs, 1),
            name(name),
            intervalMs(intervalMs),
            intervalNanos(intervalMs * 1000 * 1000) {
        clock_gettime(CLOCK_REALTIME, &startTime);
    };
};

int main() {
    MyTask task1a("1-ms-task-A", 1);
    MyTask task1b("1-ms-task-B", 1);

    MyTask task10a("10-ms-task-A", 10);
    MyTask task10b("10-ms-task-B", 10);

    MyTask task100a("100-ms-task-A", 100);
    MyTask task100b("100-ms-task-B", 100);

    MyTask task1000a("1000-ms-task-A", 1000);
    MyTask task1000b("1000-ms-task-B", 1000);

    // Start the scheduler thread.
    ScanTask::startScheduler();

    // Event loop
#pragma clang diagnostic push
#pragma ide diagnostic ignored "EndlessLoop"
    for (;;) {
//        nanosleep((const struct timespec[]) {{0, 500000L}}, nullptr);
        nanosleep((const struct timespec[]) {{0, 50000L}}, nullptr);
    }
#pragma clang diagnostic pop
//    return 0;
}
