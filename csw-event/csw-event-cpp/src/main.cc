#include "eventPublisher.h"
using namespace std;

int main(int argc, char** argv)
{
   EventPublisher::EventTest* et = new EventPublisher::EventTest();
   et->syncTest();
   et->asyncTest(argc, argv);
   delete et;
   return 0;
}
