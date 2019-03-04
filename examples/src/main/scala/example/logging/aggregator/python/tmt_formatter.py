import logging
import time

class UTCFormatter(logging.Formatter):
    converter = time.gmtime