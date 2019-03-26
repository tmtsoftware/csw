#include <iostream>
#include <cstdlib>
#include <string>
#include <spdlog/spdlog.h>
#include <spdlog/sinks/basic_file_sink.h>
#include <spdlog_setup/conf.h>

using namespace std;

// take the path for storing logs from env var, if not set throw exception
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

// set global pattern for all logs and note the json string format for generating logs
inline void setGlobalPattern()
{
    spdlog::set_pattern(R"({"timestamp":"%Y-%m-%dT%H:%M:%S.%fZ", "logger":"%n", "@severity":"%l", "file":"%s", "line":"%#", "message":"%v"})",
            spdlog::pattern_time_type::utc); // time should be in UTC
}

int main()
{
    string path = setFilePath();

    const auto path_arg = fmt::arg("TMT_LOG_HOME",path);
    spdlog_setup::from_file_with_tag_replacement(
        "logging_default.toml",
        path_arg
    );
    auto file_logger = spdlog::get("root");
    spdlog::set_default_logger(file_logger);

    setGlobalPattern();
    SPDLOG_INFO("I am INFO level CPP log");
    SPDLOG_CRITICAL("I am CRITICAL CPP log");
    SPDLOG_DEBUG("I am DEBUG level CPP log {}","message");
    SPDLOG_ERROR("I am an ERROR CPP log");
    SPDLOG_WARN("I am WARN level CPP log");
    SPDLOG_TRACE("I am TRACE level CPP log");

    spdlog::drop_all();
}

//clang++ -std=c++17 main.cpp -o main