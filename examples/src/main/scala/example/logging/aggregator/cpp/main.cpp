#include "tmt_logging.cpp"

int main(const int argc, const char * argv[])
{
    string path = setFilePath(); //called from tmt_logging.cpp

    spdlog_setup::from_file_with_tag_replacement(
        "logging_default.toml",
        fmt::arg("TMT_LOG_HOME",path)
    );
    auto file_logger = spdlog::get("root");
    spdlog::set_default_logger(file_logger);
    setGlobalPattern(); // called from tmt_logging.cpp

    SPDLOG_INFO("I am INFO level log");
    SPDLOG_CRITICAL("I am CRITICAL log");
    SPDLOG_DEBUG("I am DEBUG level log {}","message");
    SPDLOG_ERROR("I am an ERROR log");
    SPDLOG_WARN("I am WARN level log");
    SPDLOG_TRACE("I am TRACE level log");

    spdlog::drop_all();
}