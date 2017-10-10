package csw.framework.javadsl

import java.util.Optional

import scala.compat.java8.OptionConverters._
import akka.typed.ActorRef
import com.typesafe.config.Config
import csw.apps.containercmd.ContainerCmd

object JContainerCmd {
  def start(name: String, args: Array[String], defaultConfig: Optional[Config]): ActorRef[_] =
    ContainerCmd.start(name, args, defaultConfig.asScala)
}
