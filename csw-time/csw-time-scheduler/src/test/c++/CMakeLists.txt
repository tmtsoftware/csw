cmake_minimum_required(VERSION 3.20)
project(PerfTest)

set(CMAKE_CXX_STANDARD 17)
enable_language(CXX)
set(THREADS_PREFER_PTHREAD_FLAG ON)
find_package(Threads REQUIRED)

add_executable(perfTest PerfTest.cpp ScanTask.cpp ScanTask.h)
target_link_libraries(perfTest Threads::Threads)
