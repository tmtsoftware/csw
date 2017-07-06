package csw.services.logging.components.iris;

import csw.services.logging.javadsl.JComponentLoggerActor;

public abstract class JIrisActorLogger extends JComponentLoggerActor {

    public static String NAME = "jIRIS";

    @Override
    public String componentName() {
        return NAME;
    }

}
