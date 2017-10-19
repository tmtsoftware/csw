package csw.services.logging.components.iris;

import csw.services.logging.javadsl.JCommonComponentLoggerActor;

public abstract class JIrisActorLogger extends JCommonComponentLoggerActor {

    public static String NAME = "jIRIS";

    @Override
    public String componentName() {
        return NAME;
    }

}
