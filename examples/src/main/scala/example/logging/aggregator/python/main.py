from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals
from __future__ import absolute_import

import logging.config
import codecs
import json
import logging.config
import os
import pathlib
import tmt_formatter

def main():

    logging.basicConfig(level="INFO")
    logger = logging.getLogger()
    logger.info("This is the logger configured by `logging.basicConfig()`.")

    config_file = "logging_default.json"
    with codecs.open(config_file, "r", encoding="utf-8") as fd:
        config = json.load(fd)


    dirPath = os.environ.get("TMT_LOG_HOME")
    pathlib.Path(dirPath).mkdir(parents=True, exist_ok=True)
    logPath = dirPath + "/python.log"
    config["logging"]["handlers"]["file_handler"]["filename"]=logPath
    logging.config.dictConfig(config["logging"])

    # each time we need to log something we can create a logger object
    # The operation of creating a logger should be quite cheap.
    # getLogger() without arguments returns the "root" logger.
    logger = logging.getLogger()
    logger.info("This is an INFO message on the root logger.")
    logger.debug("This is an INFO message on the root logger.")

    # If we need to separate things, we can always create child loggers:
    child = logging.getLogger().getChild("child")
    child.warning("This is a WARNING message on the child logger.")

    # let's create an error. This will send an email
    child.error("This is an ERROR message.")

main()
