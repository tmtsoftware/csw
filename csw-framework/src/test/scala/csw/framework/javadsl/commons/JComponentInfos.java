/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.javadsl.commons;

import csw.command.client.models.framework.ComponentInfo;
import csw.command.client.models.framework.LocationServiceUsage;
import csw.framework.javadsl.JComponentInfo;
import csw.location.api.javadsl.JComponentType;
import csw.prefix.models.Prefix;

import java.time.Duration;
import java.util.Collections;


public class JComponentInfos {

    public static final ComponentInfo jHcdInfo = JComponentInfo.from(
            Prefix.apply("wfos.jSampleHcd"),
            JComponentType.HCD,
            "csw.framework.javadsl.components.JSampleComponentHandlers",
            LocationServiceUsage.JRegisterOnly(),
            Collections.emptySet(),
            Duration.ofSeconds(10)
    );

    public static final ComponentInfo jHcdInfoWithInitializeTimeout = JComponentInfo.from(
            Prefix.apply("wfos.trombone"),
            JComponentType.HCD,
            "csw.framework.javadsl.components.JSampleComponentHandlers",
            LocationServiceUsage.JRegisterOnly(),
            Collections.emptySet(),
            Duration.ofMillis(50)
    );
}
