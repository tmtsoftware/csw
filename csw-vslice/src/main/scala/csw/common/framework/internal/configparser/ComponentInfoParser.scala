package csw.common.framework.internal.configparser

import java.util.NoSuchElementException

import com.typesafe.config.{Config, ConfigException}
import csw.common.framework.internal.configparser.Constants._
import csw.common.framework.models.ComponentInfo
import csw.common.framework.models.LocationServiceUsages.{RegisterAndTrackServices, RegisterOnly}
import csw.services.location.models.ComponentType.{Assembly, Container, HCD}
import csw.services.location.models.{ComponentType, Connection}
import csw.services.logging.scaladsl.GenericLogger

import scala.collection.JavaConverters.collectionAsScalaIterableConverter

object ComponentInfoParser extends GenericLogger.Simple {

  def parseContainerConfig(config: Config): Option[ComponentInfo] = {
    def parseContainerName(containerConfig: Config) =
      try {
        Some(containerConfig.getString(CONTAINER_NAME))
      } catch {
        case _: ConfigException.Missing ⇒
          log.error(s"Missing configuration field: '$CONTAINER_NAME'")
          None
        case _: ConfigException.WrongType ⇒
          log.error(s"Expected a string in configuration field '$CONTAINER_NAME'")
          None
      }

    try {
      val containerConfig = config.getConfig(CONTAINER)
      val containerName   = parseContainerName(containerConfig)
      val componentInfoes = parseComponents(containerName.get, containerConfig)
      Some(ComponentInfo(containerName.get, Container, "", "", RegisterOnly, maybeComponentInfoes = componentInfoes))
    } catch {
      case _: ConfigException.Missing ⇒
        log.error(s"Missing configuration field '$CONTAINER'")
        None
      case _: Throwable ⇒
        None
    }
  }

  def parseStandaloneConfig(config: Config): Option[ComponentInfo] =
    try {
      //Standalone configs contain only a single component configuration
      val componentConfig = config.getConfig(s"$CONTAINER.$COMPONENTS")
      val names           = componentConfig.root.keySet().asScala.toList

      if (names.length > 1) {
        log.error(s"Expected single component in standalone mode but found [${names.mkString(",")}].")
        None
      } else {
        parseComponent(names.head, componentConfig.getConfig(names.head))
      }
    } catch {
      case _: ConfigException.Missing ⇒
        log.error(s"Missing configuration field '$CONTAINER.$COMPONENTS'")
        None
      case _: ConfigException.WrongType ⇒
        log.error(s"Expected a config object in configuration field '$CONTAINER.$COMPONENTS'")
        None
      case _: Throwable ⇒
        None
    }

  private def parseComponents(containerName: String, config: Config): Option[Set[ComponentInfo]] = {
    try {
      val conf  = config.getConfig(COMPONENTS)
      val names = conf.root.keySet().asScala.toSet

      val entries = names.flatMap { key ⇒
        parseComponent(key, conf.getConfig(key))
      }

      //validation check!!
      if (names.size == entries.size) {
        Some(entries)
      } else {
        None //one of component failed to parse
      }
    } catch {
      case _: ConfigException.Missing ⇒
        log.error(s"Missing configuration field '$COMPONENTS' for component '$containerName'")
        None
      case _: ConfigException.WrongType ⇒
        log.error(s"Expected a config object in configuration field '$COMPONENTS' for component '$containerName'")
        None
    }
  }

  private def parseComponent(componentName: String, componentConfig: Config): Option[ComponentInfo] =
    try {
      val componentType = componentConfig.getString(COMPONENT_TYPE)
      val componentInfo = ComponentType.withName(componentType) match {
        case Assembly => parseAssembly(componentName, componentConfig)
        case HCD      => parseHcd(componentName, componentConfig)
        case _        => None
      }
      componentInfo
    } catch {
      case _: ConfigException.Missing ⇒
        log.error(s"Missing configuration field: '$COMPONENT_TYPE' for component '$componentName'")
        None
      case _: ConfigException.WrongType ⇒
        log.error(s"Expected a string in configuration field '$COMPONENT_TYPE' for component '$componentName'")
        None
      case ex: NoSuchElementException ⇒
        log.error(s"Invalid '$COMPONENT_TYPE' for component '$componentName' - ${ex.getMessage}")
        None
    }

  // Parse the "services" section of the component config
  private def parseAssembly(assemblyName: String, conf: Config): Option[ComponentInfo] = {
    def parseConnections(assemblyName: String, assemblyConfig: Config): Option[Set[Connection]] =
      try {
        Some(assemblyConfig.getStringList(CONNECTIONS).asScala.toSet.map(connection ⇒ Connection.from(connection)))
      } catch {
        case _: ConfigException.Missing ⇒
          log.error(s"Missing configuration field '$CONNECTIONS' for component '$assemblyName'")
          None
        case _: ConfigException.WrongType ⇒
          log.error(
            s"Expected an array in configuration field '$CONNECTIONS' for component '$assemblyName' e.g. [TROMBONEHCD-hcd-akka, TROMBONEHCD-hcd-http]"
          )
          None
        case ex: IllegalArgumentException ⇒
          log.error(s"Invalid '$CONNECTIONS' for component '$assemblyName' - ${ex.getMessage}")
          None
      }

    try {
      val componentClassName = parseClassName(assemblyName, conf)
      val prefix             = parsePrefix(assemblyName, conf)
      val connections        = parseConnections(assemblyName, conf)
      Some(ComponentInfo(assemblyName, Assembly, prefix.get, componentClassName.get, RegisterAndTrackServices, connections))
    } catch {
      case _: NoSuchElementException ⇒ None
    }
  }

  private def parseHcd(name: String, conf: Config): Option[ComponentInfo] =
    try {
      val componentClassName = parseClassName(name, conf)
      val prefix             = parsePrefix(name, conf)
      Some(ComponentInfo(name, HCD, prefix.get, componentClassName.get, RegisterOnly))
    } catch {
      case _: NoSuchElementException ⇒ None
    }

  private def parseClassName(name: String, config: Config): Option[String] =
    try {
      Some(config.getString(CLASS))
    } catch {
      case _: ConfigException ⇒
        log.error(s"Missing configuration field '$CLASS' for component '$name'")
        None
      case _: ConfigException.WrongType ⇒
        log.error(s"Expected a string in configuration field '$CLASS' for component '$name'")
        None

    }

  private def parsePrefix(name: String, config: Config): Option[String] =
    try {
      Some(config.getString(PREFIX))
    } catch {
      case _: ConfigException.Missing ⇒
        log.error(s"Missing configuration field '$PREFIX' for component '$name'")
        None
      case _: ConfigException.WrongType ⇒
        log.error(s"Expected a string in configuration field '$PREFIX' for component '$name'")
        None
    }
}
