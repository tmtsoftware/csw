import logging.config
import tmt_logging

def main():
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
