package csw.framework.javadsl

import java.util.Optional

import akka.actor.typed.ActorRef
import com.typesafe.config.Config
import csw.framework.deploy.containercmd.ContainerCmd

import scala.compat.java8.OptionConverters._

/**
 * Helper instance for Java to start [[csw.framework.deploy.containercmd.ContainerCmd]] app
 */
object JContainerCmd {

  /**
   * Utility for starting a Container to host components or start a component in Standalone mode
   *
   * @param name the name to be used for the main app which uses this utility
   * @param args the command line args accepted in the main app which uses this utility
   * @param defaultConfig the default configuration which specifies the container or the component to be started
                          alone without any container
   * @return actor ref of the container or supervisor of the component started without container
   */
  def start(name: String, args: Array[String], defaultConfig: Optional[Config]): ActorRef[_] =
    ContainerCmd.start(name, args, defaultConfig.asScala)
}
