#include <iostream>
#include <cstdlib>
#include <string>
#include <spdlog/spdlog.h>
#include <spdlog/sinks/basic_file_sink.h>
#include <spdlog_setup/conf.h>

using namespace std;

// Take the path for storing logs from Environment variable. Throw exception otherwise.
string setFilePath()
{
    char* path;
    try
    {
        if((path = getenv("TMT_LOG_HOME")) == NULL)
            throw new exception;
    }
    catch(...)
    {
        cerr<<"Environment variable(TMT_LOG_HOME) not set."<<endl;
    }
    return path;
}

// Set global pattern for all logs.
inline void setGlobalPattern()
{
    spdlog::set_pattern(R"({"timestamp":"%Y-%m-%dT%H:%M:%S.%fZ", "logger":"%n", "@severity":"%l", "file":"%s", "line":"%#", "message":"%v"})",
            spdlog::pattern_time_type::utc);
}
