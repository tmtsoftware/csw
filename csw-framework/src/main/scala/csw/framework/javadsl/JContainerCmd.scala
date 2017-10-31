package csw.framework.javadsl

import java.util.Optional

import akka.typed.ActorRef
import com.typesafe.config.Config
import csw.apps.deployment.containercmd.ContainerCmd

import scala.compat.java8.OptionConverters._

object JContainerCmd {
  def start(name: String, args: Array[String], defaultConfig: Optional[Config]): ActorRef[_] =
    ContainerCmd.start(name, args, defaultConfig.asScala)
}
