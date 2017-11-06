package csw.framework.javadsl

import java.util.Optional

import akka.typed.ActorRef
import com.typesafe.config.Config
import csw.apps.containercmd.ContainerCmd

import scala.compat.java8.OptionConverters._

object JContainerCmd {

  /**
   * Utility for starting a Container to host components or start a component in Standalone mode.
   * @param name              The name to be used for the main app which uses this utility
   * @param args              The command line args accepted in the main app which uses this utility
   * @param defaultConfig     The default configuration which specifies the container or the component to be started
                              alone without any container
   * @return                  Actor ref of the container or supervisor of the component started without container
   */
  def start(name: String, args: Array[String], defaultConfig: Optional[Config]): ActorRef[_] =
    ContainerCmd.start(name, args, defaultConfig.asScala)
}
