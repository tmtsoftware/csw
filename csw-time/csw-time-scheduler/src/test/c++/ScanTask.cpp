//\file ScanTask.cpp
//\brief Implementation of the ScanTask class

// D L Terrett
// Copyright STFC All Rights Reserved

#include <cerrno>
#include <pthread.h>
#include <sched.h>
#include <semaphore.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstdio>
#include <sys/mman.h>
#include <ctime>
#include <unistd.h>
#include <vector>
#include "ScanTask.h"

using std::vector;

// Definitions of static data members.
bool ScanTask::RealTime = false;

// XXX Note: This value must be null (or properly initialized) on MacOS if not being used for scheduling
pthread_attr_t ScanTask::Tattr  ;

vector<void *>ScanTask::Tasks;

/*
    The constructor stores the reference to to the semaphore, 
    initialises the semaphore and the mutexes and creates the 
    scan thread.
*/
ScanTask::ScanTask(const char* name, int waitticks, int prio) :
        WaitTicks(waitticks), TickCount(0) {

// Initialise the semaphore (not shared between processes and 
// initially zero so that the thread is blocked).
//    int ierr = sem_init(&Sem, 0, 0);
    sem_unlink(name);
    Sem = sem_open(name, O_CREAT, 0777, 0);
    if (Sem == SEM_FAILED) perror("sem_open");

// Initialise the mutex used for waiting for the scan to run.
    WaitMutex = PTHREAD_MUTEX_INITIALIZER;

// Initialise the condition variables
    ScanStart = PTHREAD_COND_INITIALIZER;
    ScanEnd = PTHREAD_COND_INITIALIZER;

    pthread_t threadId;
    pthread_attr_t* attr = nullptr;

// Create the scan thread.
    if (RealTime) {
        struct sched_param sched{};
        sched.sched_priority = sched_get_priority_max(SCHED_FIFO) -
                               prio;
        if (pthread_attr_setschedparam(&Tattr, &sched))
            perror("pthread_attr_setschedparam");
        attr = &Tattr;
    }

    // XXX Note: Tattr must be null (or properly initialized) on MacOS if not being used for scheduling (above)
    if (pthread_create(&threadId, attr, startScan, this))
        perror("pthread_create (ScanTask)");

    // Add ourself to the list of scan tasks.
    Tasks.push_back(this);
}

/*
   This procedure attempts to make the process a real-time process 
   and lock itself into memory. If it succeeds it set the global 
   RealTime flag to true.
*/
void ScanTask::makeRealTime() {
/*
// Initialise the thread attribute structure.
    if (pthread_attr_init( &Tattr )) perror("pthread_attr_init");

// Attempt to set ourselves as a real-time process.
    struct sched_param sched;
    sched.sched_priority = sched_get_priority_min(SCHED_FIFO);
    if (sched_setscheduler( getpid(), SCHED_FIFO, &sched )) {
        perror( "sched_setscheduler" );
    } else {
        RealTime = true;
    }

// If we were allowed to set our own scheduling policy then assume 
// that we can create real-time threads.
    if ( RealTime ) {
        if (pthread_attr_setschedpolicy( &Tattr, SCHED_FIFO ))
                perror("pthread_attr_setschedpolicy");

    // Attempt to lock the application into physical memory.
        if (mlockall( MCL_FUTURE )) perror( "mlockall" );
    }
*/
}

// This is the thread start routine for the scheduler thread.

extern "C" [[noreturn]] void *ScanTask::scheduler(void *) {

// Loop forever.
    for (;;) {

        // For each scan task...
        for (auto Task : Tasks) {
            auto *task = static_cast<ScanTask *>(Task);

            // If the counter has reached zero...
            if (task->TickCount == 0) {

                // Reset the counter to the number of ticks to wait.
                task->TickCount = task->WaitTicks;

                // Post the semaphore to release the scan.
                (void) sem_post(task->Sem);

            }

            // Decrement the counter
            --(task->TickCount);
        }

        // Wait for the one tick (1ms)
        struct timespec interval{};
        interval.tv_sec = 0;
        interval.tv_nsec = 1000000;
        while (nanosleep(&interval, &interval) != 0) {
            if (errno == EINTR) continue;
            perror("nanosleep");
        }
    }
//    return nullptr;
}

//   Starts the scheduler thread.

void ScanTask::startScheduler() {
    pthread_attr_t* attr = nullptr;

   // Create the scheduler thread with the highest possible priority.
    if (RealTime) {
        struct sched_param sched{};
        sched.sched_priority = sched_get_priority_max(SCHED_FIFO);
        int ierr = pthread_attr_setschedparam(&Tattr, &sched);
        if (ierr) perror("pthread_attr_setschedparam");
        attr = &Tattr;
    }
    pthread_t threadId;
    int ierr = pthread_create(&threadId, attr, scheduler, nullptr);
    if (ierr) perror("pthread_create (startScheduler)");
}


// Start is called by the thread start routine and never returns.

[[noreturn]] void ScanTask::start() {

// Loop forever.
    for (;;) {

        // Wait for the semaphore to be released.
        (void) sem_wait(Sem);

        // Signal that the scan has started.
        pthread_mutex_lock(&WaitMutex);
        StartFlag = true;
        pthread_cond_signal(&ScanStart);
        pthread_mutex_unlock(&WaitMutex);

        // Call the action routine.
        scan();

        // Signal that the scan has ended
        pthread_mutex_lock(&WaitMutex);
        EndFlag = true;
        pthread_cond_signal(&ScanEnd);
        pthread_mutex_unlock(&WaitMutex);

    }
}

// This is a thread start routine which must have C linkage. It just 
// calls the start method of the Scan object pointed to by its 
// argument. 

extern "C" void *ScanTask::startScan(void *scanTask) {
    (static_cast<ScanTask *>(scanTask))->start();
//    return nullptr;
}

void ScanTask::waitForScan() {

// Clear the start and end flags and wait for the start-f-scan 
// condition variable.
    pthread_mutex_lock(&WaitMutex);
    StartFlag = false;
    EndFlag = false;
    while (!StartFlag) pthread_cond_wait(&ScanStart, &WaitMutex);
    pthread_mutex_unlock(&WaitMutex);

// Wait for the end-of-scan condition variable
    pthread_mutex_lock(&WaitMutex);
    while (!EndFlag) pthread_cond_wait(&ScanEnd, &WaitMutex);
    pthread_mutex_unlock(&WaitMutex);
}
