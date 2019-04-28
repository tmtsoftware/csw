#ifndef EVENTPUBLISHER_H
#define EVENTPUBLISHER_H

#include <iostream>
#include <signal.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include "hiredis/hiredis.h"
#include "hiredis/async.h"
#include "hiredis/adapters/libev.h"
#include "gen/events.pb.h"

using namespace std;

namespace EventPublisher {
    class EventTest {
        public:
            EventTest();
            ~EventTest();
            int syncTest();
            int asyncTest(int argc, char** argv);
        private:
            void static connectCallback(const redisAsyncContext *c, int status);
            void static disconnectCallbackFn(const redisAsyncContext *c, int status);
            void static getCallback(redisAsyncContext *c, void *r, void *privdata);
    };
};

#endif //EVENTPUBLISHER_H