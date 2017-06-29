package csw.services.logging.components;

import csw.services.logging.javadsl.JComponentLoggerActor;

public abstract class JTromboneHCDActorLogger extends JComponentLoggerActor {

    @Override
    public String componentName() {
        return "jTromboneHcdActor";
    }

}
