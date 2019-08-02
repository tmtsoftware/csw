package csw.framework.javadsl.commons;

import csw.framework.javadsl.JComponentInfo;
import csw.command.client.models.framework.ComponentInfo;
import csw.command.client.models.framework.LocationServiceUsage;
import csw.location.api.javadsl.JComponentType;
import csw.params.core.models.Prefix;

import java.time.Duration;
import java.util.Collections;


public class JComponentInfos {

    public static ComponentInfo jHcdInfo = JComponentInfo.from(
            "JSampleHcd",
            JComponentType.HCD,
            new Prefix("wfos.test"),
            "csw.framework.javadsl.components.JSampleComponentBehaviorFactory",
            LocationServiceUsage.JRegisterOnly(),
            Collections.emptySet(),
            Duration.ofSeconds(10)
    );

    public static ComponentInfo jHcdInfoWithInitializeTimeout = JComponentInfo.from(
            "trombone",
            JComponentType.HCD,
            new Prefix("wfos.test"),
            "csw.framework.javadsl.components.JSampleComponentBehaviorFactory",
            LocationServiceUsage.JRegisterOnly(),
            Collections.emptySet(),
            Duration.ofMillis(10)
    );
}
