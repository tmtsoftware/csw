package csw.services.logging.components.trombone;

import csw.services.logging.javadsl.JComponentLoggerActor;

public abstract class JTromboneHCDActorLogger extends JComponentLoggerActor {

    @Override
    public String componentName() {
        return "jTromboneHcdActor";
    }

}
