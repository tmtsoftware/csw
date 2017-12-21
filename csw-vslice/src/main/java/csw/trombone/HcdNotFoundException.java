package csw.trombone;

import csw.exceptions.FailureRestart;

public class HcdNotFoundException extends FailureRestart{
    public HcdNotFoundException() {
        super("Could not resolve hcd location. Initialization failure.");
    }
}
