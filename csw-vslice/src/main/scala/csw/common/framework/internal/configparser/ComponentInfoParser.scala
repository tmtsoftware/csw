package csw.common.framework.internal.configparser

import java.util.NoSuchElementException

import com.typesafe.config.{Config, ConfigException}
import csw.common.framework.internal.configparser.Constants._
import csw.common.framework.models.ComponentInfo
import csw.common.framework.models.LocationServiceUsages.{RegisterAndTrackServices, RegisterOnly}
import csw.services.location.models.ComponentType.{Assembly, Container, HCD}
import csw.services.location.models.{ComponentType, Connection}

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

object ComponentInfoParser {

  def parse(config: Config): Option[ComponentInfo] =
    try {
      val containerConfig = config.getConfig(CONTAINER)
      val containerName   = parseContainerName(containerConfig)
      val componentInfoes = parseComponents(containerName.get, containerConfig)

      Some(
        ComponentInfo(containerName.get, Container, "", "", RegisterOnly, maybeComponentInfoes = componentInfoes)
      )
    } catch {
      case ex: ConfigException.Missing ⇒
        println(s"Missing configuration field '$CONTAINER'")
        None
      case ex: Throwable ⇒
        None
    }

  private def parseContainerName(containerConfig: Config) =
    try {
      Some(containerConfig.getString(CONTAINER_NAME))
    } catch {
      case ex: ConfigException ⇒
        println(s"Missing configuration field: '$CONTAINER_NAME'")
        None
    }

  private def parseComponents(containerName: String, config: Config) = {

    // Parse the "components" section of the config file
    def parseComponent(name: String, conf: Config): Option[ComponentInfo] =
      try {
        val componentType = conf.getString(COMPONENT_TYPE)
        val componentInfo = ComponentType.withName(componentType) match {
          case Assembly => parseAssembly(name, conf)
          case HCD      => parseHcd(name, conf)
          case _        => None
        }
        componentInfo
      } catch {
        case ex: ConfigException.Missing ⇒
          println(s"Missing configuration field: '$COMPONENT_TYPE' for component '$name'")
          None
        case ex: NoSuchElementException ⇒
          println(s"Invalid $COMPONENT_TYPE for component '$name' - ${ex.getMessage}")
          None
      }

    try {
      val conf  = config.getConfig(COMPONENTS)
      val names = conf.root.keySet().asScala.toSet

      val entries = names.flatMap { key ⇒
        parseComponent(key, conf.getConfig(key))
      }

      //validation check!!
      if (names.size == entries.size)
        Some(entries)
      else //one of component failed to parse
        None
    } catch {
      case ex: ConfigException.Missing ⇒
        println(s"Missing configuration field '$COMPONENTS' for component '$containerName'")
        None
      case ex: ConfigException.WrongType ⇒
        println(s"Expected config object for configuration field '$COMPONENTS' for component '$containerName'")
        None
    }
  }

  // Parse the "services" section of the component config
  private def parseAssembly(assemblyName: String, conf: Config): Option[ComponentInfo] = {
    def parseConnections(assemblyName: String, assemblyConfig: Config): Option[Set[Connection]] =
      try {
        Some(assemblyConfig.getStringList(CONNECTIONS).asScala.toSet.map(connection ⇒ Connection.from(connection)))
      } catch {
        case ex: ConfigException.Missing ⇒
          println(s"Missing configuration field '$CONNECTIONS' for component '$assemblyName'")
          None
        case ex: ConfigException.WrongType ⇒
          println(
            s"Expected an array of connections for configuration field '$CONNECTIONS' for component '$assemblyName'. e.g. [HCD2A-hcd-akka, HCD2A-hcd-http, HCD2B-hcd-tcp]"
          )
          None
        case ex: IllegalArgumentException ⇒
          println(s"Invalid connection for component '$assemblyName' - ${ex.getMessage}")
          None
      }

    try {
      val componentClassName = parseClassName(assemblyName, conf)
      val prefix             = parsePrefix(assemblyName, conf)
      val connections        = parseConnections(assemblyName, conf)

      Some(
        ComponentInfo(assemblyName, Assembly, prefix.get, componentClassName.get, RegisterAndTrackServices, connections)
      )
    } catch {
      case ex: Throwable ⇒ None
    }
  }

  // Parse the "services" section of the component config
  private def parseHcd(name: String, conf: Config): Option[ComponentInfo] =
    try {
      val componentClassName = parseClassName(name, conf)
      val prefix             = parsePrefix(name, conf)
      Some(ComponentInfo(name, HCD, prefix.get, componentClassName.get, RegisterOnly))
    } catch {
      case ex: NoSuchElementException ⇒ None
    }

  private def parseClassName(name: String, config: Config): Option[String] =
    try {
      Some(config.getString(CLASS))
    } catch {
      case ex: ConfigException ⇒
        println(s"Missing configuration field '$CLASS' for component '$name'")
        None
    }

  private def parsePrefix(name: String, config: Config): Option[String] =
    try {
      Some(config.getString(PREFIX))
    } catch {
      case ex: ConfigException ⇒
        println(s"Missing configuration field '$PREFIX' for component '$name'")
        None
    }
}
