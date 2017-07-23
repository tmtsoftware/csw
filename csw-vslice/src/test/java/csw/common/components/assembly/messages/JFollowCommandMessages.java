package csw.common.components.assembly.messages;

import akka.typed.ActorRef;
import csw.common.framework.models.RunningAssemblyMsg;
import csw.param.BooleanParameter;
import csw.param.DoubleParameter;

import java.util.Optional;

public interface JFollowCommandMessages extends JAssemblyDomainMessages {}

class Messages {
    public static class UpdateNssInUse implements JFollowCommandMessages {
        private final BooleanParameter nssInUse;

        public UpdateNssInUse(BooleanParameter nssInUse) {
            this.nssInUse = nssInUse;
        }
    }

    public static class UpdateZAandFE implements JFollowCommandMessages {
        private final DoubleParameter zenithAngle;
        private final DoubleParameter focusError;

        public UpdateZAandFE(DoubleParameter zenithAngle, DoubleParameter focusError) {
            this.zenithAngle = zenithAngle;
            this.focusError = focusError;
        }
    }

    public static class UpdateTromboneHcd implements JFollowCommandMessages {
        private final Optional<ActorRef<RunningAssemblyMsg.Submit>> running;

        public UpdateTromboneHcd(Optional<ActorRef<RunningAssemblyMsg.Submit>> running) {
            this.running = running;
        }
    }
}
