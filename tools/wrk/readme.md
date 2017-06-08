# Performance Test Set Up

## wrk: HTTP benchmarking tool
Load testing is performed through wrk tool. wrk is a modern HTTP benchmarking tool capable of generating significant load when run on a single multi-core CPU. It combines a multithreaded design with scalable event notification systems such as epoll and kqueue.

An optional LuaJIT script can perform HTTP request generation, response processing, and custom reporting.

### How To Install
    1. Mac users can simply install it through `brew`. Run below command :
        brew install wrk
    
    2. For detailed instruction on how to install wrk on Linux / CentOS / RedHat / Fedora, 
    visit this link https://github.com/wg/wrk/wiki/Installing-Wrk-on-Linux

Verify wrk installation by running `wrk --version` and you should get something like this:
    
    
    Usage: wrk <options> <url>
      Options:
        -c, --connections <N>  Connections to keep open
        -d, --duration    <T>  Duration of test
        -t, --threads     <N>  Number of threads to use
    
        -s, --script      <S>  Load Lua script file
        -H, --header      <H>  Add header to request
            --latency          Print latency statistics
            --timeout     <T>  Socket/request timeout
        -v, --version          Print version details
    
      Numeric arguments may include a SI unit (1k, 1M, 1G)
      Time arguments may include a time unit (2s, 2m, 2h)

## Basic Usage
    wrk -t100 -c400 -d60s --timeout 10s -s post.lua http://127.0.0.1:4000
- This runs a benchmark for 60 seconds, using 100 threads, and keeping 400 HTTP connections open.
- post.lua is used here to create http POST request with dyanamic configuration file path so that we dont get conflict or file already exist error.
- post.lua internally calls debug.lua which prints debug logs.

## Lua Scripts:
1. `post.lua` : 
    - As mentioned above, this script creates HTTP POST request with dynamic config file path.
    - It maintains a counter starting from 0 and append that to config file name to create unique file name.
    - Counter is maintained for each thread, hence if we try to use multiple threads for POST request, we might get conflict in few cases as file path will not be unique.

2. `get.lua` :
    - This script is used to create HTTP GET request.
    - Like post.lua, it also maintains counter starting from 0.
    - It is recommended to use this script to fire request after running wrk with post.lua
    - This way, config repo will have enough files for get request to fetch the same.

3. `debug.lua` :
    - This script used by get.lua and post.lua for printing debug logs on console.

## Test Instructions
Once the wrk is installed on test machine, follow below instructions to run load test on Configuration Server :
1. Start cluster seed app
2. Start config service at port 4000 with initRepo option enabled
3. Now run wrk command with test parameters
    - ex. 
        - wrk -t1 -c10 -d60s -s post.lua http://127.0.0.1:4000
        - wrk -t100 -c100 -d30s -s get.lua http://127.0.0.1:4000
