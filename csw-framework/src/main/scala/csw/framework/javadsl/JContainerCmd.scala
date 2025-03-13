/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.javadsl

import org.apache.pekko.actor.typed.ActorRef
import com.typesafe.config.Config
import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem

import java.util.Optional
import scala.jdk.OptionConverters.*

// $COVERAGE-OFF$
/**
 * Helper instance for Java to start `ContainerCmd` app
 */
object JContainerCmd {

  /**
   * Utility for starting a Container to host components or start a component in Standalone mode
   *
   * @param name the name to be used for the main app which uses this utility
   * @param args the command line args accepted in the main app which uses this utility
   * @param defaultConfig the default configuration which specifies the container or the component to be started
   * alone without any container
   * @return actor ref of the container or supervisor of the component started without container
   */
  def start(name: String, subsystem: Subsystem, args: Array[String], defaultConfig: Optional[Config]): ActorRef[?] =
    ContainerCmd.start(name, subsystem, args, defaultConfig.toScala)
}
// $COVERAGE-ON$
