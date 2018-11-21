#include <chrono>
#include <thread>
#include <iostream>
#include <ctime>

int main()
{

	int i=0;
	while(i<1000)
	{
	    // Reads system clock

		std::chrono::system_clock::time_point n = std::chrono::system_clock::now();
		std::time_t tt = std::chrono::system_clock::to_time_t(n);
		std::cout<<n.time_since_epoch().count()<<std::endl;
		std::this_thread::sleep_for(std::chrono::milliseconds(10));

        // Reads High Resolution clock

		/*std::chrono::high_resolution_clock::time_point n = std::chrono::high_resolution_clock::now();
		std::time_t tt = std::chrono::high_resolution_clock::to_time_t(n);
		std::cout<<n.time_since_epoch().count()<<std::endl;
		std::this_thread::sleep_for(std::chrono::milliseconds(1));
		*/

		i++;
	}
}