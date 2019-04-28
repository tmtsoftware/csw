eventPublisher: main.o eventPublisher.o
	g++ -std=c++17 main.o eventPublisher.o -o eventPublisher -ll hiredis/libhiredis.a  -ll /usr/local/lib/libprotobuf.a -ll /usr/local/lib/libev.a
main.o: main.cc
	g++ -std=c++17 -c main.cc
eventPublisher.o: eventPublisher.cc eventPublisher.h
	g++ -std=c++17 -c eventPublisher.cc
clean:
	rm eventPublisher.o main.o eventPublisher