package csw.framework.javadsl.commons;

import csw.framework.javadsl.JComponentInfo;
import csw.messages.framework.ComponentInfo;
import csw.messages.framework.LocationServiceUsage;
import csw.messages.params.models.Prefix;
import csw.services.location.javadsl.JComponentType;
import scala.concurrent.duration.FiniteDuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class JComponentInfos {

    public static ComponentInfo jHcdInfo = JComponentInfo.from(
            "JSampleHcd",
            JComponentType.HCD,
            new Prefix("wfos"),
            "csw.framework.javadsl.components.JSampleComponentBehaviorFactory",
            LocationServiceUsage.JRegisterOnly(),
            Collections.emptySet(),
            new FiniteDuration(10, TimeUnit.SECONDS)
    );

    public static ComponentInfo jHcdInfoWithInitializeTimeout = JComponentInfo.from(
            "trombone",
            JComponentType.HCD,
            new Prefix("wfos"),
            "csw.framework.javadsl.components.JSampleComponentBehaviorFactory",
            LocationServiceUsage.JRegisterOnly(),
            Collections.emptySet(),
            new FiniteDuration(50, TimeUnit.MILLISECONDS)
    );
}
