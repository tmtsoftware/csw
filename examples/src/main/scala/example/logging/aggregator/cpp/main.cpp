
#include "tmt_logging.cpp"

int main()
{
    string path = setFilePath(); //called from tmt_logging.cpp

    const auto path_arg = fmt::arg("TMT_LOG_HOME",path);
    spdlog_setup::from_file_with_tag_replacement(
        "logging_default.toml",
        path_arg
    );
    auto file_logger = spdlog::get("root");
    spdlog::set_default_logger(file_logger);

    setGlobalPattern(); // called from tmt_logging.cpp
    SPDLOG_INFO("I am INFO level CPP log");
    SPDLOG_CRITICAL("I am CRITICAL CPP log");
    SPDLOG_DEBUG("I am DEBUG level CPP log {}","message");
    SPDLOG_ERROR("I am an ERROR CPP log");
    SPDLOG_WARN("I am WARN level CPP log");
    SPDLOG_TRACE("I am TRACE level CPP log");

    spdlog::drop_all();
}

//clang++ -std=c++17 main.cpp -o main