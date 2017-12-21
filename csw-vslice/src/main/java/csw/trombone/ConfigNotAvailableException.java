package csw.trombone;

import csw.exceptions.FailureStop;

public class ConfigNotAvailableException extends FailureStop {
    public ConfigNotAvailableException() {
        super("Configuration not available. Initialization failure.");
    }
}
