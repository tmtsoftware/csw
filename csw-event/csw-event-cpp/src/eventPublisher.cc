#include "eventPublisher.h"
using namespace EventPublisher;

   EventTest::EventTest() {

   }

   EventTest::~EventTest() {
      
   }

   void EventTest::connectCallback(const redisAsyncContext *c, int status) {
    if (status != REDIS_OK) {
        printf("Error: %s\n", c->errstr);
        return;
    }
    printf("\nConnected callback...\n");
   }

   void EventTest::disconnectCallbackFn(const redisAsyncContext *c, int status)
   {
         if(status == REDIS_OK)
         {
            printf("\nDisconnection initiated by user\n");
         }
         else if(status == REDIS_ERR)
         {
            if(c->err)
            {
               printf("\nError: %s\n", c->errstr);
            }
         }
         else
            printf("\nDisconnect callback invoked!");
   }

   void EventTest::getCallback(redisAsyncContext *c, void *r, void *privdata) {
    redisReply* reply = (redisReply*) r;
    if (reply == NULL) return;
    printf("argv[%s]: %s\n", (char*)privdata, reply->str);

    /* Disconnect after receiving the reply to GET */
    redisAsyncDisconnect(c);
   }

   int EventTest::syncTest() {
      printf("\n\n***Hiredis Synchronous API test***\n");
      redisContext *c = redisConnect("127.0.0.1", 6379);
      if (c == NULL || c->err) {
         if (c) {
         printf("Error: %s\n", c->errstr);
         // handle error
         } else {
         printf("Can't allocate redis context\n");
         }
      }
      else {
         const char* sendCommandStr = "SET foo bar";
         printf("\nSending command: %s", sendCommandStr);
         redisReply* reply = (redisReply*) redisCommand(c, sendCommandStr);
         if(reply->type == REDIS_REPLY_STATUS)
         {
            printf("\nReply status: %s",reply->str);
            const char* recvCommandStr = "GET foo";
            printf("\nSending command: %s", recvCommandStr);
            reply = (redisReply*) redisCommand(c, recvCommandStr);
            if(reply->type == REDIS_REPLY_STATUS)
            {
               printf("\nGET reply: %s", reply->str);
            }
         }
         if(reply->type == REDIS_REPLY_ERROR)
         {
            printf("\nReply error: %s", reply->str);
         }
         if(reply->type == REDIS_REPLY_INTEGER)
         {
            printf("\nReply error: %lld", reply->integer);
         }
         if(reply->type == REDIS_REPLY_NIL)
         {
            printf("\nNo data to access");
         }
         if(reply->type == REDIS_REPLY_STRING)
         {
            printf("\nReply string: %s", reply->str);
         }
      }
      redisFree(c);
      return 0;
   }

   int EventTest::asyncTest(int argc, char** argv) {
      signal(SIGPIPE, SIG_IGN);

      printf("\n\n***Hiredis Asynchronous API test***\n");
      redisAsyncContext *c = redisAsyncConnect("127.0.0.1", 6379);
      if (c->err) {
         printf("\nError: %s\n", c->errstr);
         return 1;   
      }
      else {
         redisLibevAttach(EV_DEFAULT_ c);
         //setting connect callback
         if(REDIS_OK == redisAsyncSetConnectCallback(c, this->connectCallback))
         {
            printf("\nRedis async connect callback established.");
         }
         //setting disconnect callback
         if(REDIS_OK == redisAsyncSetDisconnectCallback(c, this->disconnectCallbackFn))
         {
            printf("\nRedis async disconnect callback established.");
         }
         printf("\nSending command...");
         redisReply *reply;
         if(REDIS_OK == redisAsyncCommand(c, NULL, NULL, "SET key %b", argv[argc-1], strlen(argv[argc-1])))
         {
            printf("\nSET Command sent successfully!");
            if(REDIS_OK == redisAsyncCommand(c, this->getCallback, (char*)"end-1", "GET key"))
            {
               printf("\nGET Command sent successfully!");
               ev_loop(EV_DEFAULT_ 0);
            }
         }
      } 
      return 0;
   }