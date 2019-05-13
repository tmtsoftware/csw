cmake_minimum_required(VERSION 3.14)
project(libcbor_example C)

set(CMAKE_C_STANDARD 99)

add_executable(libcbor_example main.c)

target_link_libraries(libcbor_example -lcbor)