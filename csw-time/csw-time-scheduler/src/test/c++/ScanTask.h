//\file ScanTask.h
//\brief Definition of the ScanTask class.

// D L Terrett
// Copyright STFC All Rights Reserved

#ifndef SCANTASK_H
#define SCANTASK_H

#include <pthread.h>
#include <semaphore.h>
#include <vector>

/// Task scheduler
/**
   The ScanTask class implements a simple scheduler for threads that 
   executes periodically. Each ScanTask object creates a thread which 
   waits for a semaphore to be released, executes its scan method and 
   then goes back to waiting for the semaphore. The class method 
   startScheduler creates a thread (running at the highest available 
   priority) that unlocks the semaphore of each ScanTask object at the
   appropriate interval. The waitForScan method enables other threads
   to pause until the ScanTask thread has executed a complete 
   iteration.

   The work of the thread is done in the virtual method scan which 
   must be implemented by classes derived from ScanTask.

   Before creating any ScanTask objects the class method makeRealTime
   must be called.

   Once startScheduler has been called it is not safe to create any 
   more ScanTask objects and deleting one at any time will be a 
   disaster.
*/

class ScanTask {
public:

    /// Constructor
    ScanTask(const char* name,  ///< name used for semaphore
            int waitticks,     ///< Number of ticks between executions
            int prio           ///< thread priority
    );

    /// Set the process to be real-time.
    static void makeRealTime();

    /// Virtual method executed every waitticks ticks.
    virtual void scan() = 0;

    /// Start the scan task
    /**
        The tasks scan method does not runn until the scheduler has 
        been started
    */
    [[noreturn]] void start();

    /// Start the scheduler
    static void startScheduler();

    /// Wait for scan to run
    void waitForScan();

private:

    // The semaphore that the scan task waits on
    sem_t *Sem;

    // Mutex to protect the condition variables
    pthread_mutex_t WaitMutex;

    // Condition variables for signaling the start and end of the 
    // scan
    pthread_cond_t ScanStart;
    pthread_cond_t ScanEnd;

    // Flags for synchonizing with the start and end of the scan.
    bool StartFlag;
    bool EndFlag;

    // The number of ticks between each run of the scan
    int WaitTicks;

    // The number of ticks since the scan last ran
    int TickCount;

    static std::vector<void *> Tasks;
    static bool RealTime;
    static pthread_attr_t Tattr;

    [[noreturn]] static void *scheduler(void *arg);

    static void *startScan(void *scanTask);
};

#endif
